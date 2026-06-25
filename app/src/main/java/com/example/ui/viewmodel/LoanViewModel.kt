package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SyncPreferences
import com.example.data.model.Loan
import com.example.data.model.RepaymentPlan
import com.example.data.model.SyncRecord
import com.example.data.repository.LoanRepository
import com.example.data.sync.BackupData
import com.example.data.sync.WebDavSyncManager
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LoanRepository(db.loanDao())
    private val syncManager = WebDavSyncManager()
    val prefs = SyncPreferences(application)

    // Exposed Flows from DB
    val loans: StateFlow<List<Loan>> = repository.allLoans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val repaymentPlans: StateFlow<List<RepaymentPlan>> = repository.allRepaymentPlans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncRecords: StateFlow<List<SyncRecord>> = repository.allSyncRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Credentials / Settings UI states
    private val _jianguoUser = MutableStateFlow(prefs.jianguoUser)
    val jianguoUser = _jianguoUser.asStateFlow()

    private val _jianguoPass = MutableStateFlow(prefs.jianguoPass)
    val jianguoPass = _jianguoPass.asStateFlow()

    private val _jianguoUrl = MutableStateFlow(prefs.jianguoUrl)
    val jianguoUrl = _jianguoUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.lastSyncTime)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    init {
        // Auto-seed sample loans if empty
        viewModelScope.launch {
            val currentLoans = repository.allLoans.first()
            if (currentLoans.isEmpty()) {
                seedSampleData()
            }
        }
    }

    fun updateCredentials(username: String, appPass: String, serverUrl: String) {
        prefs.jianguoUser = username
        prefs.jianguoPass = appPass
        prefs.jianguoUrl = serverUrl
        _jianguoUser.value = username
        _jianguoPass.value = appPass
        _jianguoUrl.value = serverUrl
    }

    private suspend fun seedSampleData() {
        // Loan 1: 素 10000
        val l1 = Loan(
            borrowerName = "素",
            principal = 10000.0,
            loanDate = "2026-03-15",
            totalPeriods = 6,
            repaymentMethod = "每月等额",
            totalInterest = 0.0,
            status = "进行中",
            note = "初始对外借款1"
        )
        // Loan 2: 素 15000
        val l2 = Loan(
            borrowerName = "素",
            principal = 15000.0,
            loanDate = "2026-04-01",
            totalPeriods = 12,
            repaymentMethod = "每月等额",
            totalInterest = 600.0,
            status = "进行中",
            note = "初始对外借款2"
        )
        // Loan 3: 李家辉 40000
        val l3 = Loan(
            borrowerName = "李家辉",
            principal = 40000.0,
            loanDate = "2026-01-10",
            totalPeriods = 12,
            repaymentMethod = "先息后本",
            totalInterest = 2400.0,
            status = "进行中",
            note = "初始对外借款3"
        )
        // Loan 4: 辉新 85000
        val l4 = Loan(
            borrowerName = "辉新",
            principal = 85000.0,
            loanDate = "2026-05-10",
            totalPeriods = 24,
            repaymentMethod = "每月等额",
            totalInterest = 5000.0,
            status = "进行中",
            note = "初始对外借款4"
        )

        repository.addLoanWithPlans(l1)
        repository.addLoanWithPlans(l2)
        repository.addLoanWithPlans(l3)
        repository.addLoanWithPlans(l4)

        // Mark some historic payments as received automatically to show realistic history
        val plans = repository.allRepaymentPlans.first()
        viewModelScope.launch {
            plans.forEach { plan ->
                if (DateUtils.isOverdue(plan.dueDate, "待收") && plan.periodNumber <= 2) {
                    repository.updateRepaymentPlan(
                        plan.copy(
                            status = "已收",
                            actualReceivedDate = plan.dueDate,
                            actualReceivedAmount = plan.totalAmount
                        )
                    )
                }
            }
        }
    }

    // --- Loan Operations ---

    fun addLoan(
        borrowerName: String,
        principal: Double,
        loanDate: String,
        totalPeriods: Int,
        repaymentMethod: String,
        totalInterest: Double,
        note: String
    ) {
        viewModelScope.launch {
            val loan = Loan(
                borrowerName = borrowerName,
                principal = principal,
                loanDate = loanDate,
                totalPeriods = totalPeriods,
                repaymentMethod = repaymentMethod,
                totalInterest = totalInterest,
                status = "进行中",
                note = note
            )
            repository.addLoanWithPlans(loan)
        }
    }

    fun updateLoan(
        loan: Loan,
        plans: List<RepaymentPlan>
    ) {
        viewModelScope.launch {
            repository.updateLoanWithPlans(loan, plans)
        }
    }

    fun updateLoanWithRegeneratedPlans(loan: Loan) {
        viewModelScope.launch {
            val plans = repository.generateRepaymentPlans(
                loanId = loan.id,
                principal = loan.principal,
                totalPeriods = loan.totalPeriods,
                repaymentMethod = loan.repaymentMethod,
                totalInterest = loan.totalInterest,
                startDate = loan.loanDate
            )
            repository.updateLoanWithPlans(loan, plans)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoanWithPlans(loan)
        }
    }

    fun updateRepaymentPlanStatus(
        plan: RepaymentPlan,
        status: String,
        actualReceivedDate: String?,
        actualReceivedAmount: Double?
    ) {
        viewModelScope.launch {
            val updatedPlan = plan.copy(
                status = status,
                actualReceivedDate = actualReceivedDate,
                actualReceivedAmount = actualReceivedAmount
            )
            repository.updateRepaymentPlan(updatedPlan)
        }
    }

    // --- WebDAV Sync Actions ---

    fun testConnection(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Loading("正在测试连接...")
            val result = syncManager.testConnection(
                username = _jianguoUser.value,
                appPassword = _jianguoPass.value,
                serverUrl = _jianguoUrl.value
            )
            if (result.isSuccess) {
                _syncStatus.value = SyncStatus.Success("连接成功！")
            } else {
                _syncStatus.value = SyncStatus.Error("连接失败: ${result.exceptionOrNull()?.message}")
            }
            onResult(result)
        }
    }

    fun backupToCloud() {
        if (!prefs.isConfigured) {
            _syncStatus.value = SyncStatus.Error("请先在设置中配置坚果云同步账户。")
            return
        }

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Loading("正在备份数据...")
            try {
                val currentLoans = loans.value
                val currentPlans = repaymentPlans.value
                val backupData = BackupData(loans = currentLoans, repaymentPlans = currentPlans)

                val result = syncManager.uploadBackup(
                    username = _jianguoUser.value,
                    appPassword = _jianguoPass.value,
                    serverUrl = _jianguoUrl.value,
                    backupData = backupData
                )

                val nowStr = DateUtils.getCurrentDateTime()
                if (result.isSuccess) {
                    prefs.lastSyncTime = nowStr
                    _lastSyncTime.value = nowStr
                    _syncStatus.value = SyncStatus.Success("备份成功！")
                    repository.insertSyncRecord(
                        SyncRecord(
                            syncTime = nowStr,
                            syncType = "上传",
                            fileName = "loantracker_sync.json",
                            status = "成功"
                        )
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知错误"
                    _syncStatus.value = SyncStatus.Error("备份失败: $errorMsg")
                    repository.insertSyncRecord(
                        SyncRecord(
                            syncTime = nowStr,
                            syncType = "上传",
                            fileName = "loantracker_sync.json",
                            status = "失败",
                            errorMessage = errorMsg
                        )
                    )
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("备份失败: ${e.message}")
            }
        }
    }

    fun restoreFromCloud() {
        if (!prefs.isConfigured) {
            _syncStatus.value = SyncStatus.Error("请先在设置中配置坚果云同步账户。")
            return
        }

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Loading("正在恢复数据...")
            try {
                val result = syncManager.downloadBackup(
                    username = _jianguoUser.value,
                    appPassword = _jianguoPass.value,
                    serverUrl = _jianguoUrl.value
                )

                val nowStr = DateUtils.getCurrentDateTime()
                if (result.isSuccess) {
                    val backupData = result.getOrNull()
                    if (backupData != null) {
                        repository.clearAllAndRestore(backupData.loans, backupData.repaymentPlans)

                        prefs.lastSyncTime = nowStr
                        _lastSyncTime.value = nowStr
                        _syncStatus.value = SyncStatus.Success("恢复成功！")
                        repository.insertSyncRecord(
                            SyncRecord(
                                syncTime = nowStr,
                                syncType = "下载",
                                fileName = "loantracker_sync.json",
                                status = "成功"
                            )
                        )
                    } else {
                        _syncStatus.value = SyncStatus.Error("备份文件为空")
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知错误"
                    _syncStatus.value = SyncStatus.Error("恢复失败: $errorMsg")
                    repository.insertSyncRecord(
                        SyncRecord(
                            syncTime = nowStr,
                            syncType = "下载",
                            fileName = "loantracker_sync.json",
                            status = "失败",
                            errorMessage = errorMsg
                        )
                    )
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error("恢复异常: ${e.message}")
            }
        }
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoanViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Loading(val message: String) : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
