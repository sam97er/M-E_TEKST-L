package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.MAndETekstilRepository
import com.example.domain.ai.AiTaskRouter
import com.example.domain.telegram.TelegramNotifier
import com.example.domain.trendyol.TrendyolSyncManager
import com.example.security.SecureConfigManager

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val configManager: SecureConfigManager = SecureConfigManager(context)

    val aiTaskRouter: AiTaskRouter = AiTaskRouter(
        configManager = configManager,
        aiTaskDao = database.aiTaskDao(),
        auditLogDao = database.auditLogDao()
    )

    val syncManager: TrendyolSyncManager = TrendyolSyncManager(
        configManager = configManager,
        database = database
    )

    val telegramNotifier: TelegramNotifier = TelegramNotifier(
        configManager = configManager,
        auditLogDao = database.auditLogDao()
    )

    val repository: MAndETekstilRepository = MAndETekstilRepository(
        database = database,
        configManager = configManager,
        aiTaskRouter = aiTaskRouter,
        syncManager = syncManager,
        telegramNotifier = telegramNotifier
    )
}
