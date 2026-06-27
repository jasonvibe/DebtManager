package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val borrowerName: String,
    val principal: Double,
    val loanDate: String, // ISO format (e.g. YYYY-MM-DD)
    val totalPeriods: Int,
    val repaymentMethod: String, // "每月等额" / "先息后本" / "自定义"
    val totalInterest: Double,
    val status: String, // "进行中" / "已结清"
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val loanSource: String = "",
    val repaymentDay: Int = 15
)

@Entity(
    tableName = "repayment_plans",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"])]
)
data class RepaymentPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val periodNumber: Int,
    val dueDate: String, // ISO format (e.g. YYYY-MM-DD)
    val totalAmount: Double,
    val principalPart: Double,
    val interestPart: Double,
    val status: String, // "待收" / "已收" / "逾期"
    val actualReceivedDate: String? = null,
    val actualReceivedAmount: Double? = null
)

@Entity(tableName = "sync_records")
data class SyncRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncTime: String, // ISO format or formatted text
    val syncType: String, // "上传" / "下载"
    val fileName: String,
    val status: String, // "成功" / "失败"
    val errorMessage: String? = null
)
