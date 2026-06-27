package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun getCurrentDate(): String {
        return sdf.format(Date())
    }

    fun getCurrentDateTime(): String {
        return sdfFull.format(Date())
    }

    fun addMonths(dateStr: String, months: Int): String {
        return try {
            val date = sdf.parse(dateStr) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, months)
            sdf.format(cal.time)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun isOverdue(dueDateStr: String, status: String): Boolean {
        if (status == "已收") return false
        return try {
            val todayStr = getCurrentDate()
            val today = sdf.parse(todayStr) ?: Date()
            val dueDate = sdf.parse(dueDateStr) ?: Date()
            dueDate.before(today)
        } catch (e: Exception) {
            false
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = sdf.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance()
            cal.time = date
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            "${year}年${month}月${day}日"
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getDueDateForPeriod(startDateStr: String, periodIndex: Int, repaymentDay: Int): String {
        return try {
            val date = sdf.parse(startDateStr) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, periodIndex)
            
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val targetDay = if (repaymentDay > maxDay) maxDay else repaymentDay
            cal.set(Calendar.DAY_OF_MONTH, targetDay)
            
            sdf.format(cal.time)
        } catch (e: Exception) {
            startDateStr
        }
    }

    fun getDayOfDate(dateStr: String): Int {
        return try {
            val date = sdf.parse(dateStr) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            15
        }
    }
}
