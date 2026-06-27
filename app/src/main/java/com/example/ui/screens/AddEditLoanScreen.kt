package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Loan
import com.example.data.model.RepaymentPlan
import com.example.data.repository.LoanRepository
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(
    loan: Loan?,
    onBackClick: () -> Unit,
    onSaveClick: (borrowerName: String, principal: Double, loanDate: String, totalPeriods: Int, repaymentMethod: String, totalInterest: Double, note: String, loanSource: String, repaymentDay: Int, monthlyRepaymentAmount: Double) -> Unit
) {
    val isEditMode = loan != null

    var borrowerName by remember { mutableStateOf(loan?.borrowerName ?: "") }
    var principalStr by remember { mutableStateOf(loan?.principal?.toString() ?: "") }
    var loanSource by remember { mutableStateOf(loan?.loanSource ?: "") }
    var loanDate by remember { mutableStateOf(loan?.loanDate ?: DateUtils.getCurrentDate()) }
    var repaymentDayStr by remember {
        mutableStateOf(
            loan?.repaymentDay?.toString() ?: DateUtils.getDayOfDate(loanDate).toString()
        )
    }
    var totalPeriodsStr by remember { mutableStateOf(loan?.totalPeriods?.toString() ?: "12") }
    var repaymentMethod by remember { mutableStateOf(loan?.repaymentMethod ?: "每月等额") }
    var totalInterestStr by remember { mutableStateOf(loan?.totalInterest?.toString() ?: "0.0") }
    var monthlyRepaymentAmountStr by remember {
        mutableStateOf(
            if (loan?.repaymentMethod == "固定金额" && loan.monthlyRepaymentAmount > 0) loan.monthlyRepaymentAmount.toString() else ""
        )
    }
    var note by remember { mutableStateOf(loan?.note ?: "") }

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val livePreviewPlans = remember(principalStr, totalPeriodsStr, repaymentMethod, totalInterestStr, loanDate, repaymentDayStr, monthlyRepaymentAmountStr) {
        val p = principalStr.toDoubleOrNull() ?: 0.0
        val periods = totalPeriodsStr.toIntOrNull() ?: 0
        val interest = totalInterestStr.toDoubleOrNull() ?: 0.0
        val rDay = repaymentDayStr.toIntOrNull() ?: DateUtils.getDayOfDate(loanDate)
        val mAmt = monthlyRepaymentAmountStr.toDoubleOrNull() ?: 0.0

        if (repaymentMethod == "固定金额") {
            if (p > 0 && mAmt > 0 && rDay in 1..31) {
                com.example.util.RepaymentPlanGenerator.generate(
                    loanId = 0L,
                    principal = p,
                    totalPeriods = 0,
                    repaymentMethod = repaymentMethod,
                    totalInterest = interest,
                    startDate = loanDate,
                    repaymentDay = rDay,
                    monthlyRepaymentAmount = mAmt
                )
            } else {
                emptyList()
            }
        } else {
            if (p > 0 && periods > 0 && rDay in 1..31) {
                com.example.util.RepaymentPlanGenerator.generate(
                    loanId = 0L,
                    principal = p,
                    totalPeriods = periods,
                    repaymentMethod = repaymentMethod,
                    totalInterest = interest,
                    startDate = loanDate,
                    repaymentDay = rDay
                )
            } else {
                emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑借款信息" else "录入新借款", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("add_edit_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val p = principalStr.toDoubleOrNull()
                            val periods = if (repaymentMethod == "固定金额") livePreviewPlans.size else totalPeriodsStr.toIntOrNull()
                            val interest = totalInterestStr.toDoubleOrNull() ?: 0.0
                            val rDay = repaymentDayStr.toIntOrNull()
                            val mAmt = monthlyRepaymentAmountStr.toDoubleOrNull() ?: 0.0

                            if (borrowerName.trim().isEmpty()) {
                                errorMessage = "请填写借款人姓名"
                                showError = true
                            } else if (p == null || p <= 0) {
                                errorMessage = "请填写有效的借款本金"
                                showError = true
                            } else if (repaymentMethod == "固定金额" && mAmt <= 0) {
                                errorMessage = "请填写有效的固定还款金额"
                                showError = true
                            } else if (periods == null || periods <= 0) {
                                errorMessage = "请填写有效的期数"
                                showError = true
                            } else if (rDay == null || rDay !in 1..31) {
                                errorMessage = "请填写有效的还款日 (1-31日)"
                                showError = true
                            } else {
                                onSaveClick(
                                    borrowerName.trim(),
                                    p,
                                    loanDate.trim(),
                                    periods,
                                    repaymentMethod,
                                    interest,
                                    note.trim(),
                                    loanSource.trim(),
                                    rDay,
                                    if (repaymentMethod == "固定金额") mAmt else 0.0
                                )
                            }
                        },
                        modifier = Modifier.testTag("add_edit_save_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error notice
            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Borrower input
            OutlinedTextField(
                value = borrowerName,
                onValueChange = { borrowerName = it; showError = false },
                label = { Text("借款人姓名 *") },
                placeholder = { Text("例如：素、李家辉、辉新") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_borrower"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Principal input
            OutlinedTextField(
                value = principalStr,
                onValueChange = { principalStr = it; showError = false },
                label = { Text("借款本金 (元) *") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_principal"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Loan Source Input (NEW)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("借款来源", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                // Quick chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("自有资金", "支付宝借呗", "微粒贷", "信用卡").forEach { sourceOption ->
                        FilterChip(
                            selected = loanSource == sourceOption,
                            onClick = { loanSource = sourceOption },
                            label = { Text(sourceOption) },
                            modifier = Modifier.weight(1f).testTag("chip_source_$sourceOption")
                        )
                    }
                }

                OutlinedTextField(
                    value = loanSource,
                    onValueChange = { loanSource = it },
                    label = { Text("自定义/具体来源") },
                    placeholder = { Text("例如：招商银行信用卡、京东白条") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_source"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Date input
            OutlinedTextField(
                value = loanDate,
                onValueChange = {
                    loanDate = it
                    showError = false
                    val day = DateUtils.getDayOfDate(it)
                    if (day in 1..31) {
                        repaymentDayStr = day.toString()
                    }
                },
                label = { Text("借款日期 (YYYY-MM-DD) *") },
                placeholder = { Text("2026-06-25") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_date"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Repayment Day input (NEW)
            OutlinedTextField(
                value = repaymentDayStr,
                onValueChange = {
                    repaymentDayStr = it
                    showError = false
                },
                label = { Text("每月还款日 (1-31日) *") },
                placeholder = { Text("例如：15") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_repayment_day"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Repayment Method
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("还款方式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("每月等额", "先息后本").forEach { mOption ->
                        FilterChip(
                            selected = repaymentMethod == mOption,
                            onClick = { repaymentMethod = mOption },
                            label = { Text(mOption) },
                            modifier = Modifier.weight(1f).testTag("chip_method_$mOption")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("固定金额", "自定义").forEach { mOption ->
                        FilterChip(
                            selected = repaymentMethod == mOption,
                            onClick = { repaymentMethod = mOption },
                            label = { Text(mOption) },
                            modifier = Modifier.weight(1f).testTag("chip_method_$mOption")
                        )
                    }
                }
            }

            // Repayment parameters conditional rendering
            if (repaymentMethod == "固定金额") {
                // Monthly Repayment Amount input (NEW)
                OutlinedTextField(
                    value = monthlyRepaymentAmountStr,
                    onValueChange = { monthlyRepaymentAmountStr = it; showError = false },
                    label = { Text("每月固定还款金额 (元) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_monthly_repayment_amount"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Info card showing auto-calculated periods
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "还款期数详情",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val periodsCount = livePreviewPlans.size
                            Text(
                                text = if (periodsCount > 0) "自动推算为共计 $periodsCount 期还款计划" else "请输入本金、还款期数和每月固定还款金额",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Total Periods Selector (Always visible so users can customize number of periods!)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (repaymentMethod == "固定金额") "最大还款期数限制 (自定义期数)" else "还款期数 *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("3", "6", "12", "24").forEach { pOption ->
                        FilterChip(
                            selected = totalPeriodsStr == pOption,
                            onClick = { totalPeriodsStr = pOption; showError = false },
                            label = { Text("${pOption}期") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = totalPeriodsStr,
                    onValueChange = { totalPeriodsStr = it; showError = false },
                    label = { Text(if (repaymentMethod == "固定金额") "自定义期数 (可选：限制最长多少期)" else "自定义期数 *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_periods"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Total Interest input
            OutlinedTextField(
                value = totalInterestStr,
                onValueChange = { totalInterestStr = it; showError = false },
                label = { Text("合同/约定总利息 (元)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_interest"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注说明 (选填)") },
                placeholder = { Text("记下借款用途、转账方式等额外信息") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("input_note"),
                shape = RoundedCornerShape(12.dp)
            )

            // --- Calculation Live Preview Banner ---
            if (livePreviewPlans.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "回款试算预览 (共 ${livePreviewPlans.size} 期)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "本息合计 ¥${String.format("%.2f", (principalStr.toDoubleOrNull() ?: 0.0) + (totalInterestStr.toDoubleOrNull() ?: 0.0))}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Show first 3 and last 1 as brief summary if too many periods
                            val itemsToShow = if (livePreviewPlans.size > 4) {
                                livePreviewPlans.take(3) + livePreviewPlans.last()
                            } else {
                                livePreviewPlans
                            }

                            itemsToShow.forEachIndexed { idx, plan ->
                                val isEllipsisNeeded = livePreviewPlans.size > 4 && idx == 3
                                if (isEllipsisNeeded) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("...... 省略中间 ${livePreviewPlans.size - 4} 个期次 ......", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("期次 ${plan.periodNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("回款日期 ${DateUtils.formatDisplayDate(plan.dueDate)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("¥${String.format("%.2f", plan.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
