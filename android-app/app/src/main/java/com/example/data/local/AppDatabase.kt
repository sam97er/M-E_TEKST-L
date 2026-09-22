package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TrendyolProductEntity::class,
        TrendyolOrderEntity::class,
        CustomerQuestionsEntity::class,
        StockMovementEntity::class,
        AiTaskEntity::class,
        AutomationRuleEntity::class,
        AuditLogEntity::class,
        DailyBriefEntity::class,
        TrendyolReturnEntity::class,
        WarehouseEntity::class,
        WarehouseTransferEntity::class,
        SyncLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customerQuestionDao(): CustomerQuestionDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun aiTaskDao(): AiTaskDao
    abstract fun automationRuleDao(): AutomationRuleDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun dailyBriefDao(): DailyBriefDao
    abstract fun returnDao(): ReturnDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "me_tekstil_trendyol.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
