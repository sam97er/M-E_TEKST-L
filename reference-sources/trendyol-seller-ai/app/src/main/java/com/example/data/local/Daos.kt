package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id DESC")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE stockCount <= :threshold ORDER BY stockCount ASC")
    fun getLowStockProducts(threshold: Int): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stockCount <= :threshold ORDER BY stockCount ASC")
    suspend fun getLowStockProductsDirect(threshold: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE title LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchProductsDirect(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stockCount = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int)

    @Query("UPDATE products SET salePrice = :newPrice WHERE id = :productId")
    suspend fun updatePrice(productId: Long, newPrice: Double)

    @Query("UPDATE products SET isActive = :isActive WHERE id = :productId")
    suspend fun updateActiveStatus(productId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY id DESC")
    suspend fun getAllOrdersDirect(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY id DESC")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY id DESC")
    suspend fun getOrdersByStatusDirect(status: String): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateStatus(orderId: Long, newStatus: String)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getCount(): Int
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM customer_questions ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM customer_questions WHERE isAnswered = 0 ORDER BY id DESC")
    fun getUnansweredQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM customer_questions WHERE isAnswered = 0 ORDER BY id DESC")
    suspend fun getUnansweredQuestionsDirect(): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("UPDATE customer_questions SET replyText = :reply, isAnswered = 1 WHERE id = :id")
    suspend fun submitReply(id: Long, reply: String)

    @Query("UPDATE customer_questions SET aiSuggestedReply = :aiReply WHERE id = :id")
    suspend fun setAiSuggestion(id: Long, aiReply: String)

    @Query("SELECT COUNT(*) FROM customer_questions")
    suspend fun getCount(): Int
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: StoreSettingsEntity)
}
