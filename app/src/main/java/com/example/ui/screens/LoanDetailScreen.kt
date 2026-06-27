package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Loan
import com.example.data.model.RepaymentPlan
import com.example.util.DateUtils

data class LoanDetailStats(
    val totalInterest: Double,
    val totalPrincipal: Double,
    val totalAmount: Double,
    val paidPrincipal: Double,
    val paidInterest: Double,
    val paidTotal: Double,
    val remainingPrincipal: Double,
    val remainingInterest: Double,
    val remainingTotal: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loan: Loan?,
    repaymentPlans: List<RepaymentPlan>,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onUpdatePlanStatus: (RepaymentPlan, String, String?, Double?) -> Unit
) {
    if (loan == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("借款详情") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("借款信息不存在或已被删除")
            }
        }
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedPlanForPayment by remember { mutableStateOf<RepaymentPlan?>(null) }

    // Dialog state for payment input
    var actualDate by remember { mutableStateOf("") }
    var actualAmount by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("已收") } // "已收" or "待收"

    // Statistics calculations
    val detailStats = remember(loan, repaymentPlans) {
        val totalInterest = loan.totalInterest
        val totalPrincipal = loan.principal
        val totalAmount = totalPrincipal + totalInterest

        val paidPrincipal = repaymentPlans.filter { it.status == "已收" }.sumOf { it.principalPart }
        val paidInterest = repaymentPlans.filter { it.status == "已收" }.sumOf { it.interestPart }
        val paidTotal = repaymentPlans.filter { it.status == "已收" }.sumOf { it.actualReceivedAmount ?: it.totalAmount }

        val remainingPrincipal = (totalPrincipal - paidPrincipal).coerceAtLeast(0.0)
        val remainingInterest = (totalInterest - paidInterest).coerceAtLeast(0.0)
        val remainingTotal = (totalAmount - paidTotal).coerceAtLeast(0.0)

        LoanDetailStats(
            totalInterest = totalInterest,
            totalPrincipal = totalPrincipal,
            totalAmount = totalAmount,
            paidPrincipal = paidPrincipal,
            paidInterest = paidInterest,
            paidTotal = paidTotal,
            remainingPrincipal = remainingPrincipal,
            remainingInterest = remainingInterest,
            remainingTotal = remainingTotal
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${loan.borrowerName} 的借款详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick, modifier = Modifier.testTag("detail_edit_button")) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("detail_delete_button")) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- Loan Summary Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "借款总本金",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "¥${String.format("%,.2f", loan.principal)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        when (loan.status) {
                                            "已结清" -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFFFF3E0)
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = loan.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (loan.status) {
                                        "已结清" -> Color(0xFF2E7D32)
                                        else -> Color(0xFFEF6C00)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Secondary specifications
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                DetailField(label = "借款日期", value = DateUtils.formatDisplayDate(loan.loanDate))
                                DetailField(label = "还款方式", value = loan.repaymentMethod)
                                if (loan.repaymentMethod == "固定金额" && loan.monthlyRepaymentAmount > 0.0) {
                                    DetailField(label = "固定月还", value = "¥${String.format("%.2f", loan.monthlyRepaymentAmount)}")
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val totalCount = if (repaymentPlans.isNotEmpty()) repaymentPlans.size else loan.totalPeriods
                                val remainingCount = if (repaymentPlans.isNotEmpty()) repaymentPlans.count { it.status != "已收" } else loan.totalPeriods
                                DetailField(label = "还款期数", value = "$remainingCount/$totalCount")
                                DetailField(label = "约定利息", value = "¥${String.format("%.2f", loan.totalInterest)}")
                                DetailField(label = "每月还款日", value = "${loan.repaymentDay}日")
                            }
                        }

                        if (loan.loanSource.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailField(label = "借款资金来源", value = loan.loanSource)
                        }

                        if (loan.note.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = loan.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // --- Payment Ledger (Stats) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "还款进度明细",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("已收回本息", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${String.format("%,.2f", detailStats.paidTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("本金: ¥${String.format("%.2f", detailStats.paidPrincipal)}", style = MaterialTheme.typography.bodySmall)
                                Text("利息: ¥${String.format("%.2f", detailStats.paidInterest)}", style = MaterialTheme.typography.bodySmall)
                            }

                            VerticalDivider(modifier = Modifier.height(70.dp).padding(horizontal = 16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("剩余待收本息", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${String.format("%,.2f", detailStats.remainingTotal)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("本金: ¥${String.format("%.2f", detailStats.remainingPrincipal)}", style = MaterialTheme.typography.bodySmall)
                                Text("利息: ¥${String.format("%.2f", detailStats.remainingInterest)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // --- Repayment Plan List ---
            item {
                Text(
                    text = "还款计划书 (点击条目记录回款)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (repaymentPlans.isEmpty()) {
                item {
                    Text(
                        text = "无还款计划",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(repaymentPlans, key = { it.id }) { plan ->
                    val isPlanOverdue = plan.status == "待收" && DateUtils.isOverdue(plan.dueDate, plan.status)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_item_${plan.periodNumber}")
                            .clickable {
                                selectedPlanForPayment = plan
                                paymentStatus = plan.status
                                actualDate = plan.actualReceivedDate ?: plan.dueDate
                                actualAmount = (plan.actualReceivedAmount ?: plan.totalAmount).toString()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (plan.status == "已收") {
                                Color(0xFFE8F5E9).copy(alpha = 0.2f)
                            } else if (isPlanOverdue) {
                                Color(0xFFFFEBEE).copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (plan.status == "已收") {
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.5f))
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${plan.periodNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "第 ${plan.periodNumber} 期还款",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = DateUtils.formatDisplayDate(plan.dueDate),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "本金¥${plan.principalPart} + 利息¥${plan.interestPart}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (plan.status == "已收" && plan.actualReceivedDate != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "于 ${DateUtils.formatDisplayDate(plan.actualReceivedDate)} 实收 ¥${String.format("%.2f", plan.actualReceivedAmount ?: plan.totalAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            // Payment Right Side
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "¥${String.format("%.2f", plan.totalAmount)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                plan.status == "已收" -> Color(0xFFE8F5E9)
                                                isPlanOverdue -> Color(0xFFFFEBEE)
                                                else -> Color(0xFFFFF3E0)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isPlanOverdue) "逾期" else plan.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            plan.status == "已收" -> Color(0xFF2E7D32)
                                            isPlanOverdue -> Color(0xFFC62828)
                                            else -> Color(0xFFEF6C00)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("您确定要删除这笔借款记录吗？删除后，该借款及其关联的所有还款计划将永久丢失，且无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    },
                    modifier = Modifier.testTag("detail_delete_confirm_ok")
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // --- Mark Payment Dialog ---
    selectedPlanForPayment?.let { plan ->
        AlertDialog(
            onDismissRequest = { selectedPlanForPayment = null },
            title = { Text("记账：第 ${plan.periodNumber} 期回款", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("调整该账期的还款状态和实际到账账务。")

                    // Status segment selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = paymentStatus == "已收",
                            onClick = { paymentStatus = "已收" },
                            label = { Text("标记为已收") },
                            modifier = Modifier.weight(1f).testTag("detail_chip_paid")
                        )
                        FilterChip(
                            selected = paymentStatus == "待收",
                            onClick = { paymentStatus = "待收" },
                            label = { Text("标记为待收") },
                            modifier = Modifier.weight(1f).testTag("detail_chip_unpaid")
                        )
                    }

                    if (paymentStatus == "已收") {
                        OutlinedTextField(
                            value = actualDate,
                            onValueChange = { actualDate = it },
                            label = { Text("实际到账日期 (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth().testTag("detail_actual_date_field"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                        )

                        OutlinedTextField(
                            value = actualAmount,
                            onValueChange = { actualAmount = it },
                            label = { Text("实际回款金额 (元)") },
                            modifier = Modifier.fillMaxWidth().testTag("detail_actual_amount_field"),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val statusToSave = paymentStatus
                        val dateToSave = if (statusToSave == "已收") actualDate else null
                        val amountToSave = if (statusToSave == "已收") {
                            actualAmount.toDoubleOrNull() ?: plan.totalAmount
                        } else null

                        onUpdatePlanStatus(plan, statusToSave, dateToSave, amountToSave)
                        selectedPlanForPayment = null
                    },
                    modifier = Modifier.testTag("detail_payment_confirm_ok")
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlanForPayment = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
