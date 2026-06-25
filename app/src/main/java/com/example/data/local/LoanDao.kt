package com.example.data.local

import androidx.room.*
import com.example.data.model.Loan
import com.example.data.model.RepaymentPlan
import com.example.data.model.SyncRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    // --- Loan Queries ---
    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun getLoanById(id: Long): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanByIdOneShot(id: Long): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    // --- RepaymentPlan Queries ---
    @Query("SELECT * FROM repayment_plans ORDER BY dueDate ASC")
    fun getAllRepaymentPlans(): Flow<List<RepaymentPlan>>

    @Query("SELECT * FROM repayment_plans WHERE loanId = :loanId ORDER BY periodNumber ASC")
    fun getRepaymentPlansForLoan(loanId: Long): Flow<List<RepaymentPlan>>

    @Query("SELECT * FROM repayment_plans WHERE loanId = :loanId ORDER BY periodNumber ASC")
    suspend fun getRepaymentPlansForLoanOneShot(loanId: Long): List<RepaymentPlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepaymentPlans(plans: List<RepaymentPlan>)

    @Update
    suspend fun updateRepaymentPlan(plan: RepaymentPlan)

    @Query("DELETE FROM repayment_plans WHERE loanId = :loanId")
    suspend fun deleteRepaymentPlansForLoan(loanId: Long)

    // --- SyncRecord Queries ---
    @Query("SELECT * FROM sync_records ORDER BY id DESC")
    fun getAllSyncRecords(): Flow<List<SyncRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncRecord(record: SyncRecord)

    // --- Sync / Backup helpers ---
    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("DELETE FROM repayment_plans")
    suspend fun clearRepaymentPlans()

    @Query("DELETE FROM sync_records")
    suspend fun clearSyncRecords()

    @Transaction
    suspend fun clearAllAndRestore(loans: List<Loan>, plans: List<RepaymentPlan>) {
        clearLoans()
        clearRepaymentPlans()
        // We re-insert all
        loans.forEach { insertLoanWithId(it) }
        insertRepaymentPlans(plans)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanWithId(loan: Loan): Long
}
