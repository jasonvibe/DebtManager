package com.example.data.repository

import com.example.data.local.LoanDao
import com.example.data.model.Loan
import com.example.data.model.RepaymentPlan
import com.example.data.model.SyncRecord
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

class LoanRepository(private val loanDao: LoanDao) {

    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()
    val allRepaymentPlans: Flow<List<RepaymentPlan>> = loanDao.getAllRepaymentPlans()
    val allSyncRecords: Flow<List<SyncRecord>> = loanDao.getAllSyncRecords()

    fun getLoanById(id: Long): Flow<Loan?> = loanDao.getLoanById(id)

    fun getRepaymentPlansForLoan(loanId: Long): Flow<List<RepaymentPlan>> =
        loanDao.getRepaymentPlansForLoan(loanId)

    suspend fun getLoanByIdOneShot(id: Long): Loan? = loanDao.getLoanByIdOneShot(id)

    suspend fun getRepaymentPlansForLoanOneShot(loanId: Long): List<RepaymentPlan> =
        loanDao.getRepaymentPlansForLoanOneShot(loanId)

    private fun round(value: Double): Double {
        return try {
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            value
        }
    }

    /**
     * Generates standard repayment plans based on loan parameters
     */
    fun generateRepaymentPlans(
        loanId: Long,
        principal: Double,
        totalPeriods: Int,
        repaymentMethod: String,
        totalInterest: Double,
        startDate: String
    ): List<RepaymentPlan> {
        return com.example.util.RepaymentPlanGenerator.generate(
            loanId = loanId,
            principal = principal,
            totalPeriods = totalPeriods,
            repaymentMethod = repaymentMethod,
            totalInterest = totalInterest,
            startDate = startDate
        )
    }

    suspend fun addLoanWithPlans(loan: Loan, customPlans: List<RepaymentPlan>? = null) {
        val loanId = loanDao.insertLoan(loan)
        val plans = customPlans?.map { it.copy(loanId = loanId) }
            ?: generateRepaymentPlans(
                loanId = loanId,
                principal = loan.principal,
                totalPeriods = loan.totalPeriods,
                repaymentMethod = loan.repaymentMethod,
                totalInterest = loan.totalInterest,
                startDate = loan.loanDate
            )
        loanDao.insertRepaymentPlans(plans)
        checkAndUpdateLoanStatus(loanId)
    }

    suspend fun updateLoanWithPlans(loan: Loan, plans: List<RepaymentPlan>) {
        loanDao.updateLoan(loan)
        loanDao.deleteRepaymentPlansForLoan(loan.id)
        val updatedPlans = plans.map { it.copy(loanId = loan.id) }
        loanDao.insertRepaymentPlans(updatedPlans)
        checkAndUpdateLoanStatus(loan.id)
    }

    suspend fun updateRepaymentPlan(plan: RepaymentPlan) {
        loanDao.updateRepaymentPlan(plan)
        checkAndUpdateLoanStatus(plan.loanId)
    }

    suspend fun deleteLoanWithPlans(loan: Loan) {
        loanDao.deleteLoan(loan)
    }

    suspend fun insertSyncRecord(record: SyncRecord) {
        loanDao.insertSyncRecord(record)
    }

    suspend fun clearAllAndRestore(loans: List<Loan>, plans: List<RepaymentPlan>) {
        loanDao.clearAllAndRestore(loans, plans)
    }

    /**
     * Helper to automatically compute loan status based on its repayment plans.
     * If all repayment plans are marked "已收", the loan becomes "已结清", else "进行中".
     */
    suspend fun checkAndUpdateLoanStatus(loanId: Long) {
        val plans = loanDao.getRepaymentPlansForLoanOneShot(loanId)
        val loan = loanDao.getLoanByIdOneShot(loanId) ?: return

        val allPaid = plans.isNotEmpty() && plans.all { it.status == "已收" }
        val newStatus = if (allPaid) "已结清" else "进行中"

        if (loan.status != newStatus) {
            val updatedLoan = loan.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            loanDao.updateLoan(updatedLoan)
        }
    }
}
