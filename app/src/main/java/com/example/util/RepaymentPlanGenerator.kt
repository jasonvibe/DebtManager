package com.example.util

import com.example.data.model.RepaymentPlan
import java.math.BigDecimal
import java.math.RoundingMode

object RepaymentPlanGenerator {

    private fun round(value: Double): Double {
        return try {
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            value
        }
    }

    fun generate(
        loanId: Long,
        principal: Double,
        totalPeriods: Int,
        repaymentMethod: String,
        totalInterest: Double,
        startDate: String,
        repaymentDay: Int,
        monthlyRepaymentAmount: Double = 0.0
    ): List<RepaymentPlan> {
        val plans = mutableListOf<RepaymentPlan>()

        if (repaymentMethod == "固定金额") {
            if (monthlyRepaymentAmount <= 0.0) return plans
            val totalDebt = principal + totalInterest
            val calculatedPeriods = Math.ceil(totalDebt / monthlyRepaymentAmount).toInt().coerceAtLeast(1)
            
            val actualPeriods = if (totalPeriods > 0) {
                Math.min(totalPeriods, calculatedPeriods)
            } else {
                calculatedPeriods
            }

            val equalInterestPart = round(totalInterest / actualPeriods)
            var accumulatedPrincipal = 0.0
            var accumulatedInterest = 0.0
            var accumulatedTotal = 0.0

            for (i in 1..actualPeriods) {
                val dueDate = DateUtils.getDueDateForPeriod(startDate, i, repaymentDay)
                val totalAmount: Double
                val iPart: Double
                val pPart: Double

                if (i == actualPeriods) {
                    totalAmount = round(totalDebt - accumulatedTotal)
                    iPart = round(totalInterest - accumulatedInterest)
                    pPart = round(totalAmount - iPart)
                } else {
                    totalAmount = round(monthlyRepaymentAmount)
                    iPart = equalInterestPart
                    pPart = round(totalAmount - iPart)
                    accumulatedPrincipal += pPart
                    accumulatedInterest += iPart
                    accumulatedTotal += totalAmount
                }

                plans.add(
                    RepaymentPlan(
                        loanId = loanId,
                        periodNumber = i,
                        dueDate = dueDate,
                        totalAmount = totalAmount,
                        principalPart = pPart,
                        interestPart = iPart,
                        status = "待收"
                    )
                )
            }
            return plans
        }

        if (totalPeriods <= 0) return plans

        val equalPrincipalPart = round(principal / totalPeriods)
        val equalInterestPart = round(totalInterest / totalPeriods)

        var accumulatedPrincipal = 0.0
        var accumulatedInterest = 0.0

        for (i in 1..totalPeriods) {
            val dueDate = DateUtils.getDueDateForPeriod(startDate, i, repaymentDay)
            var pPart = 0.0
            var iPart = 0.0

            when (repaymentMethod) {
                "先息后本" -> {
                    if (i == totalPeriods) {
                        pPart = round(principal)
                        iPart = round(totalInterest - accumulatedInterest)
                    } else {
                        pPart = 0.0
                        iPart = equalInterestPart
                        accumulatedInterest += iPart
                    }
                }
                else -> { // "每月等额" or default "自定义"
                    if (i == totalPeriods) {
                        pPart = round(principal - accumulatedPrincipal)
                        iPart = round(totalInterest - accumulatedInterest)
                    } else {
                        pPart = equalPrincipalPart
                        iPart = equalInterestPart
                        accumulatedPrincipal += pPart
                        accumulatedInterest += iPart
                    }
                }
            }

            val totalAmount = round(pPart + iPart)
            plans.add(
                RepaymentPlan(
                    loanId = loanId,
                    periodNumber = i,
                    dueDate = dueDate,
                    totalAmount = totalAmount,
                    principalPart = pPart,
                    interestPart = iPart,
                    status = "待收"
                )
            )
        }
        return plans
    }
}
