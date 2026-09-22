package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.model.AiApprovalStatus
import com.example.domain.model.OrderPrepStatus
import com.example.domain.model.TrendyolOrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY stockQuantity ASC")
    fun getAllProducts(): Flow<List<TrendyolProductEntity>>

    @Query("SELECT * FROM products WHERE stockQuantity < :threshold")
    fun getLowStockProducts(threshold: Int = 5): Flow<List<TrendyolProductEntity>>

    @Query("SELECT * FROM products WHERE totalSold == 0 AND stockQuantity > 0")
    fun getDeadStockProducts(): Flow<List<TrendyolProductEntity>>

    @Query("SELECT * FROM products WHERE warehouseId = :warehouseId")
    fun getProductsByWarehouse(warehouseId: String): Flow<List<TrendyolProductEntity>>

    @Query("SELECT * FROM products WHERE productCode LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR color LIKE '%' || :query || '%' OR size LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<TrendyolProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): TrendyolProductEntity?

    @Query("SELECT * FROM products WHERE productCode = :code")
    fun getVariantsByProductCode(code: String): Flow<List<TrendyolProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<TrendyolProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: TrendyolProductEntity)

    @Update
    suspend fun updateProduct(product: TrendyolProductEntity)

    @Query("UPDATE products SET stockQuantity = :newStock, lastSyncTime = :syncTime WHERE barcode = :barcode")
    suspend fun updateStock(barcode: String, newStock: Int, syncTime: Long = System.currentTimeMillis())

    @Query("UPDATE products SET totalSold = totalSold + :qty WHERE barcode = :barcode")
    suspend fun incrementTotalSold(barcode: String, qty: Int)

    @Query("UPDATE products SET warehouseId = :warehouseId WHERE barcode = :barcode")
    suspend fun updateWarehouse(barcode: String, warehouseId: String)

    @Query("UPDATE products SET returnCount = returnCount + :qty WHERE barcode = :barcode")
    suspend fun incrementReturnCount(barcode: String, qty: Int)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteProduct(barcode: String)

    @Query("DELETE FROM products WHERE isDemo = 1")
    suspend fun deleteDemoProducts(): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<TrendyolOrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY orderDate DESC")
    fun getOrdersByStatus(status: TrendyolOrderStatus): Flow<List<TrendyolOrderEntity>>

    @Query("SELECT * FROM orders WHERE prepStatus = :prepStatus ORDER BY orderDate DESC")
    fun getOrdersByPrepStatus(prepStatus: OrderPrepStatus): Flow<List<TrendyolOrderEntity>>

    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber LIMIT 1")
    suspend fun getOrderByNumber(orderNumber: String): TrendyolOrderEntity?

    @Query("SELECT * FROM orders WHERE orderNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%' OR productCode LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%' ORDER BY orderDate DESC")
    fun searchOrders(query: String): Flow<List<TrendyolOrderEntity>>

    @Query("SELECT * FROM orders WHERE orderDate BETWEEN :start AND :end ORDER BY orderDate DESC")
    fun getOrdersInDateRange(start: Long, end: Long): Flow<List<TrendyolOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrders(orders: List<TrendyolOrderEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: TrendyolOrderEntity)

    @Update
    suspend fun updateOrder(order: TrendyolOrderEntity)

    @Query("UPDATE orders SET prepStatus = :prepStatus, lastUpdated = :timestamp WHERE orderNumber = :orderNumber")
    suspend fun updatePrepStatus(orderNumber: String, prepStatus: OrderPrepStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET returnStatus = :returnStatus, lastUpdated = :timestamp WHERE orderNumber = :orderNumber")
    suspend fun updateReturnStatus(orderNumber: String, returnStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET isStockDeducted = :isDeducted WHERE orderNumber = :orderNumber")
    suspend fun markOrderStockDeducted(orderNumber: String, isDeducted: Boolean)

    @Query("DELETE FROM orders WHERE isDemo = 1")
    suspend fun deleteDemoOrders(): Int

    @Query("SELECT COUNT(*) FROM orders WHERE status != 'CANCELLED' AND status != 'RETURNED'")
    fun getActiveOrderCount(): Flow<Int>

    @Query("SELECT SUM(totalPrice) FROM orders WHERE status != 'CANCELLED'")
    fun getTotalSales(): Flow<Double?>

    @Query("SELECT SUM(netProfit) FROM orders WHERE status != 'CANCELLED'")
    fun getTotalNetProfit(): Flow<Double?>
}

@Dao
interface CustomerQuestionDao {
    @Query("SELECT * FROM customer_questions ORDER BY questionDate DESC")
    fun getAllQuestions(): Flow<List<CustomerQuestionsEntity>>

    @Query("SELECT * FROM customer_questions WHERE status = 'BEKLIYOR' OR status = 'MANUEL_INCELEME' ORDER BY questionDate DESC")
    fun getPendingQuestions(): Flow<List<CustomerQuestionsEntity>>

    @Query("SELECT * FROM customer_questions WHERE questionId = :id LIMIT 1")
    suspend fun getQuestionById(id: String): CustomerQuestionsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CustomerQuestionsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CustomerQuestionsEntity)

    @Update
    suspend fun updateQuestion(question: CustomerQuestionsEntity)

    @Query("DELETE FROM customer_questions WHERE isDemo = 1")
    suspend fun deleteDemoQuestions(): Int

    @Query("SELECT COUNT(*) FROM customer_questions WHERE isSent = 0")
    fun getUnansweredCount(): Flow<Int>
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE barcode = :barcode ORDER BY timestamp DESC")
    fun getMovementsForBarcode(barcode: String): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)
}

@Dao
interface AiTaskDao {
    @Query("SELECT * FROM ai_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<AiTaskEntity>>

    @Query("SELECT * FROM ai_tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: AiApprovalStatus): Flow<List<AiTaskEntity>>

    @Query("SELECT * FROM ai_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): AiTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AiTaskEntity): Long

    @Update
    suspend fun updateTask(task: AiTaskEntity)

    @Query("UPDATE ai_tasks SET status = :newStatus, userEditedContent = :editedContent, approvedAt = :approvedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: AiApprovalStatus, editedContent: String, approvedAt: Long? = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM ai_tasks WHERE status = 'BEKLIYOR'")
    fun getPendingTaskCount(): Flow<Int>
}

@Dao
interface AutomationRuleDao {
    @Query("SELECT * FROM automation_rules ORDER BY id ASC")
    fun getAllRules(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<AutomationRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRuleEntity)

    @Update
    suspend fun updateRule(rule: AutomationRuleEntity)

    @Query("UPDATE automation_rules SET lastTriggeredAt = :time, executionCount = executionCount + 1, lastError = :error WHERE id = :id")
    suspend fun recordExecution(id: Long, time: Long, error: String = "")
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}

@Dao
interface DailyBriefDao {
    @Query("SELECT * FROM daily_briefs ORDER BY reportDate DESC")
    fun getAllBriefs(): Flow<List<DailyBriefEntity>>

    @Query("SELECT * FROM daily_briefs WHERE reportDate = :date LIMIT 1")
    suspend fun getBriefByDate(date: String): DailyBriefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrief(brief: DailyBriefEntity)
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns ORDER BY returnDate DESC")
    fun getAllReturns(): Flow<List<TrendyolReturnEntity>>

    @Query("SELECT * FROM returns WHERE returnId = :returnId LIMIT 1")
    suspend fun getReturnById(returnId: String): TrendyolReturnEntity?

    @Query("SELECT * FROM returns WHERE orderNumber = :orderNumber")
    fun getReturnsForOrder(orderNumber: String): Flow<List<TrendyolReturnEntity>>

    @Query("SELECT SUM(quantity) FROM returns WHERE orderNumber = :orderNumber AND barcode = :barcode AND status != 'REDDEDİLDİ'")
    suspend fun getReturnedQuantityForOrder(orderNumber: String, barcode: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturns(returns: List<TrendyolReturnEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnEntity: TrendyolReturnEntity)

    @Query("UPDATE returns SET status = :status, restockAction = :restockAction WHERE returnId = :returnId")
    suspend fun updateReturnStatus(returnId: String, status: String, restockAction: String)

    @Query("UPDATE returns SET isRestocked = :isRestocked WHERE returnId = :returnId")
    suspend fun updateReturnRestocked(returnId: String, isRestocked: Boolean)

    @Query("DELETE FROM returns WHERE isDemo = 1")
    suspend fun deleteDemoReturns(): Int

    @Query("SELECT COUNT(*) FROM returns")
    fun getReturnsCount(): Flow<Int>
}

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY isDefault DESC, name ASC")
    fun getAllWarehouses(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE warehouseId = :id LIMIT 1")
    suspend fun getWarehouseById(id: String): WarehouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: WarehouseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouses(warehouses: List<WarehouseEntity>)

    @Query("SELECT * FROM warehouse_transfers ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<WarehouseTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: WarehouseTransferEntity): Long
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllSyncLogs(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSyncLog(): SyncLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLogEntity): Long
}
