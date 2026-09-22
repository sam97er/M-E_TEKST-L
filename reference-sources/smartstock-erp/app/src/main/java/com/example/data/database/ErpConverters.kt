package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.EmployeeRole
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus

class ErpConverters {
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.CASH)

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus =
        runCatching { OrderStatus.valueOf(value) }.getOrDefault(OrderStatus.COMPLETED)

    @TypeConverter
    fun fromEmployeeRole(value: EmployeeRole): String = value.name

    @TypeConverter
    fun toEmployeeRole(value: String): EmployeeRole =
        runCatching { EmployeeRole.valueOf(value) }.getOrDefault(EmployeeRole.CASHIER)

    @TypeConverter
    fun fromTicketPriority(value: TicketPriority): String = value.name

    @TypeConverter
    fun toTicketPriority(value: String): TicketPriority =
        runCatching { TicketPriority.valueOf(value) }.getOrDefault(TicketPriority.MEDIUM)

    @TypeConverter
    fun fromTicketStatus(value: TicketStatus): String = value.name

    @TypeConverter
    fun toTicketStatus(value: String): TicketStatus =
        runCatching { TicketStatus.valueOf(value) }.getOrDefault(TicketStatus.OPEN)
}
