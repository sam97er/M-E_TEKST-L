package com.example.domain.ai

import com.example.data.local.AiTaskDao
import com.example.data.local.AiTaskEntity
import com.example.data.local.AuditLogDao
import com.example.data.local.AuditLogEntity
import com.example.domain.model.AiApprovalStatus
import com.example.domain.model.AiTaskType
import com.example.security.AiSlotConfiguration
import com.example.security.SecureConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiExecutionResult(
    val taskId: Long,
    val isSuccess: Boolean,
    val content: String,
    val providerUsed: String,
    val modelUsed: String,
    val slotUsed: Int,
    val errorMessage: String = ""
)

class AiTaskRouter(
    private val configManager: SecureConfigManager,
    private val aiTaskDao: AiTaskDao,
    private val auditLogDao: AuditLogDao
) {
    private val geminiAdapter = GeminiProviderAdapter()
    private val openAiAdapter = OpenAiCompatibleAdapter()
    private val fallbackAdapter = IntelligentTurkishFallbackAdapter()

    private fun getAdapterForProvider(providerName: String): AiProviderAdapter {
        return when {
            providerName.contains("Gemini", ignoreCase = true) -> geminiAdapter
            providerName.contains("OpenAI", ignoreCase = true) ||
                    providerName.contains("DeepSeek", ignoreCase = true) ||
                    providerName.contains("Uyumlu", ignoreCase = true) -> openAiAdapter
            else -> fallbackAdapter
        }
    }

    private fun resolveSlotForTask(taskType: AiTaskType): Int {
        return when (taskType) {
            AiTaskType.CUSTOMER_REPLY -> 1 // Slot 1: Müşteri İletişimi AI
            AiTaskType.PROFIT_ANALYSIS,
            AiTaskType.DAILY_BRIEF -> 2 // Slot 2: İş ve Kâr AI
            AiTaskType.PRODUCT_IMPROVEMENT,
            AiTaskType.QUALITY_CHECK,
            AiTaskType.PRICING_ADVICE,
            AiTaskType.RETURN_ANALYSIS -> 3 // Slot 3: Ürün ve Büyüme AI
        }
    }

    private fun sanitizeUserPrompt(raw: String): String {
        var sanitized = raw
        val suspiciousPatterns = listOf(
            "(?i)ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions?",
            "(?i)system\\s*:",
            "(?i)assistant\\s*:",
            "(?i)disregard\\s+all\\s+rules",
            "(?i)you\\s+are\\s+now\\s+in\\s+developer\\s+mode",
            "(?i)sen\\s+artık\\s+başka\\s+bir\\s+yapay\\s+zekasın",
            "(?i)önceki\\s+tüm\\s+talimatları\\s+unut"
        )
        for (pattern in suspiciousPatterns) {
            sanitized = sanitized.replace(Regex(pattern), "[güvenlik_filtrelendi]")
        }
        return sanitized.take(4000)
    }

    suspend fun routeAndExecute(
        taskType: AiTaskType,
        targetRefId: String,
        systemInstruction: String,
        userPrompt: String,
        overrideSlot: Int? = null
    ): AiExecutionResult = withContext(Dispatchers.IO) {
        val safePrompt = sanitizeUserPrompt(userPrompt)
        val targetSlot = overrideSlot ?: resolveSlotForTask(taskType)
        var activeConfig = configManager.getAiSlotConfig(targetSlot)

        // Pre-create task entry in room
        val taskId = aiTaskDao.insertTask(
            AiTaskEntity(
                taskType = taskType,
                targetReferenceId = targetRefId,
                inputData = safePrompt,
                outputData = "",
                providerName = activeConfig.providerName,
                modelName = activeConfig.modelName,
                slotNumber = activeConfig.slotNumber,
                status = AiApprovalStatus.INCELENIYOR
            )
        )

        var chosenAdapter = getAdapterForProvider(activeConfig.providerName)
        var chosenConfig = activeConfig
        var executionResult: Result<String>

        // Check if API key is present; if not, use fallback provider
        if (activeConfig.apiKey.isBlank() || !activeConfig.isEnabled) {
            val fallbackSlot = activeConfig.fallbackSlot
            if (fallbackSlot in 1..3 && fallbackSlot != targetSlot) {
                val fallbackConfig = configManager.getAiSlotConfig(fallbackSlot)
                if (fallbackConfig.isEnabled && fallbackConfig.apiKey.isNotBlank()) {
                    chosenConfig = fallbackConfig
                    chosenAdapter = getAdapterForProvider(fallbackConfig.providerName)
                } else {
                    chosenAdapter = fallbackAdapter
                }
            } else {
                chosenAdapter = fallbackAdapter
            }
        }

        executionResult = chosenAdapter.generateResponse(
            config = chosenConfig,
            systemPrompt = systemInstruction,
            userPrompt = safePrompt
        )

        // If primary call failed, try intelligent local fallback
        if (executionResult.isFailure && chosenAdapter !is IntelligentTurkishFallbackAdapter) {
            val primaryError = executionResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"
            auditLogDao.insertLog(
                AuditLogEntity(
                    actionType = "AI_CALL_FAILED_FALLBACK",
                    moduleName = "AI Task Router",
                    details = "Slot ${chosenConfig.slotNumber} (${chosenConfig.providerName}) başarısız oldu: $primaryError. Yerel yedek devreye alındı."
                )
            )
            chosenAdapter = fallbackAdapter
            executionResult = chosenAdapter.generateResponse(
                config = chosenConfig,
                systemPrompt = systemInstruction,
                userPrompt = safePrompt
            )
        }

        return@withContext if (executionResult.isSuccess) {
            val text = executionResult.getOrThrow()
            configManager.incrementAiUsage(chosenConfig.slotNumber)

            aiTaskDao.updateTask(
                AiTaskEntity(
                    id = taskId,
                    taskType = taskType,
                    targetReferenceId = targetRefId,
                    inputData = safePrompt,
                    outputData = text,
                    providerName = chosenAdapter.providerName,
                    modelName = chosenConfig.modelName,
                    slotNumber = chosenConfig.slotNumber,
                    status = AiApprovalStatus.BEKLIYOR
                )
            )

            auditLogDao.insertLog(
                AuditLogEntity(
                    actionType = "AI_TASK_SUCCESS",
                    moduleName = "AI Task Router",
                    details = "Görev: ${taskType.title} | Slot: ${chosenConfig.slotNumber} | Model: ${chosenConfig.modelName}"
                )
            )

            AiExecutionResult(
                taskId = taskId,
                isSuccess = true,
                content = text,
                providerUsed = chosenAdapter.providerName,
                modelUsed = chosenConfig.modelName,
                slotUsed = chosenConfig.slotNumber
            )
        } else {
            val err = executionResult.exceptionOrNull()?.message ?: "Yapay zeka yanıt üretemedi"
            aiTaskDao.updateTask(
                AiTaskEntity(
                    id = taskId,
                    taskType = taskType,
                    targetReferenceId = targetRefId,
                    inputData = userPrompt,
                    outputData = "",
                    providerName = chosenAdapter.providerName,
                    modelName = chosenConfig.modelName,
                    slotNumber = chosenConfig.slotNumber,
                    status = AiApprovalStatus.HATA,
                    errorMessage = err
                )
            )

            AiExecutionResult(
                taskId = taskId,
                isSuccess = false,
                content = "",
                providerUsed = chosenAdapter.providerName,
                modelUsed = chosenConfig.modelName,
                slotUsed = chosenConfig.slotNumber,
                errorMessage = err
            )
        }
    }

    suspend fun testSlotConnection(slotNumber: Int): Result<String> = withContext(Dispatchers.IO) {
        val config = configManager.getAiSlotConfig(slotNumber)
        val adapter = getAdapterForProvider(config.providerName)
        adapter.testConnection(config)
    }
}
