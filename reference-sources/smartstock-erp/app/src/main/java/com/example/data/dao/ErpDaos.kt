package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BackupLog
import com.example.data.model.Employee
import com.example.data.model.Product
import com.example.data.model.SaleOrder
import com.example.data.model.SaleOrderItem
import com.example.data.model.SupportTicket
import com.example.data.model.TrendyolSyncLog
import com.example.data.model.Warehouse
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minAlertStock ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode OR trendyolBarcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = :newStock, lastUpdated = :now WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty, lastUpdated = :now WHERE id = :id AND stockQuantity >= :qty")
    suspend fun decreaseStock(id: Long, qty: Int, now: Long = System.currentTimeMillis()): Int
}

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouses(warehouses: List<Warehouse>)

    @Update
    suspend fun updateWarehouse(warehouse: Warehouse)
}

@Dao
interface SaleOrderDao {
    @Query("SELECT * FROM sale_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<SaleOrder>>

    @Query("SELECT * FROM sale_orders WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayOrders(startOfDay: Long): Flow<List<SaleOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SaleOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<SaleOrderItem>)

    @Query("SELECT * FROM sale_order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<SaleOrderItem>>

    @Query("SELECT * FROM sale_order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsList(orderId: Long): List<SaleOrderItem>

    @Query("SELECT COUNT(*) FROM sale_orders")
    suspend fun getOrdersCount(): Int
}

@Dao
interface TrendyolDao {
    @Query("SELECT * FROM trendyol_sync_logs ORDER BY timestamp DESC")
    fun getSyncLogs(): Flow<List<TrendyolSyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TrendyolSyncLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<TrendyolSyncLog>)
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY id ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<Employee>)

    @Query("UPDATE employees SET currentSales = currentSales + :amount WHERE id = :id")
    suspend fun addEmployeeSales(id: Long, amount: Double)

    @Query("UPDATE employees SET scannedItemsCount = scannedItemsCount + :count WHERE id = :id")
    suspend fun addScannedItems(id: Long, count: Int)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<SupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket): Long

    @Update
    suspend fun updateTicket(ticket: SupportTicket)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<SupportTicket>)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_logs ORDER BY timestamp DESC")
    fun getBackupLogs(): Flow<List<BackupLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupLog(log: BackupLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupLogs(logs: List<BackupLog>)
}
