package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        QuestionEntity::class,
        StoreSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun questionDao(): QuestionDao
    abstract fun storeSettingsDao(): StoreSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trendyol_seller_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    initializeCleanDatabase(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    initializeCleanDatabase(database)
                }
            }
        }

        private suspend fun initializeCleanDatabase(database: AppDatabase) {
            if (database.storeSettingsDao().getSettingsDirect() == null) {
                database.storeSettingsDao().saveSettings(
                    StoreSettingsEntity(
                        id = 1,
                        storeName = "متجري في ترنديول",
                        supplierId = "",
                        apiKey = "",
                        apiSecret = "",
                        isLiveMode = false,
                        storeScore = 10.0,
                        deliveryScore = 10.0,
                        commissionAverage = 0.0
                    )
                )
            }
            if (database.productDao().getCount() == 0) {
                database.productDao().insertAll(InitialData.sampleProducts)
            }
            if (database.orderDao().getCount() == 0) {
                database.orderDao().insertAll(InitialData.sampleOrders)
            }
            if (database.questionDao().getCount() == 0) {
                database.questionDao().insertAll(InitialData.sampleQuestions)
            }
        }
    }
}
