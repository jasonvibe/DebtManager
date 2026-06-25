package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
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

data class LoanCalculatedStats(
    val paidSum: Double,
    val remainingPrincipal: Double,
    val paidPeriods: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loans: List<Loan>,
    repaymentPlans: List<RepaymentPlan>,
    onLoanClick: (Long) -> Unit,
    onAddLoanClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("全部") } // "全部", "进行中", "已结清"
    var sortBy by remember { mutableStateOf("下期还款日") } // "下期还款日", "借款人", "借款金额"
    var sortAscending by remember { mutableStateOf(true) }

    // State for Filter / Sort sheets or menus
    var showFilterMenu by remember { mutableStateOf(false) }

    // Helper map to find next plan for each loan
    val nextPlanMap = remember(repaymentPlans) {
        repaymentPlans
            .filter { it.status != "已收" }
            .groupBy { it.loanId }
            .mapValues { (_, plans) ->
                plans.minByOrNull { it.dueDate }
            }
    }

    // Helper to check if a loan is overdue
    val loanOverdueMap = remember(repaymentPlans) {
        repaymentPlans.groupBy { it.loanId }.mapValues { (_, plans) ->
            plans.any { (it.status == "逾期" || (it.status == "待收" && DateUtils.isOverdue(it.dueDate, it.status))) }
        }
    }

    // Pre-calculate stats per loan for fluid scrolling
    val loanStatsMap = remember(loans, repaymentPlans) {
        repaymentPlans.groupBy { it.loanId }.mapValues { (loanId, plans) ->
            val loan = loans.find { it.id == loanId }
            val principal = loan?.principal ?: 0.0
            val paidSum = plans.filter { it.status == "已收" }.sumOf { it.principalPart }
            val remainingPrincipal = (principal - paidSum).coerceAtLeast(0.0)
            val paidPeriods = plans.count { it.status == "已收" }
            LoanCalculatedStats(paidSum, remainingPrincipal, paidPeriods)
        }
    }

    // Filtered and sorted loans
    val processedLoans = remember(loans, nextPlanMap, loanOverdueMap, searchQuery, statusFilter, sortBy, sortAscending) {
        loans
            .filter { loan ->
                // Search query filter
                loan.borrowerName.contains(searchQuery, ignoreCase = true) ||
                loan.note.contains(searchQuery, ignoreCase = true) ||
                loan.loanSource.contains(searchQuery, ignoreCase = true)
            }
            .filter { loan ->
                // Status filter
                when (statusFilter) {
                    "进行中" -> loan.status == "进行中"
                    "已结清" -> loan.status == "已结清"
                    else -> true
                }
            }
            .sortedWith { l1, l2 ->
                val comparison = when (sortBy) {
                    "借款金额" -> l1.principal.compareTo(l2.principal)
                    "借款人" -> l1.borrowerName.compareTo(l2.borrowerName)
                    else -> { // "下期还款日"
                        val p1 = nextPlanMap[l1.id]
                        val p2 = nextPlanMap[l2.id]
                        when {
                            p1 == null && p2 == null -> 0
                            p1 == null -> 1 // Settled loans go to the end
                            p2 == null -> -1
                            else -> p1.dueDate.compareTo(p2.dueDate)
                        }
                    }
                }
                if (sortAscending) comparison else -comparison
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("借款台账", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            sortAscending = !sortAscending
                        },
                        modifier = Modifier.testTag("loans_sort_direction_button")
                    ) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "排序方向"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddLoanClick,
                icon = { Icon(Icons.Default.Add, "新增借款") },
                text = { Text("新增借款") },
                modifier = Modifier.testTag("loans_add_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar & Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索借款人/备注...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("loans_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                IconButton(
                    onClick = { showFilterMenu = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .testTag("loans_filter_button")
                ) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = "过滤与排序")
                }

                // Dropdown menu for Filters and Sorters
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("全部状态", fontWeight = if (statusFilter == "全部") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { statusFilter = "全部"; showFilterMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("进行中", fontWeight = if (statusFilter == "进行中") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { statusFilter = "进行中"; showFilterMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("已结清", fontWeight = if (statusFilter == "已结清") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { statusFilter = "已结清"; showFilterMenu = false }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("按下期还款日排序", fontWeight = if (sortBy == "下期还款日") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { sortBy = "下期还款日"; showFilterMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("按借款金额排序", fontWeight = if (sortBy == "借款金额") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { sortBy = "借款金额"; showFilterMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("按借款人排序", fontWeight = if (sortBy == "借款人") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { sortBy = "借款人"; showFilterMenu = false }
                    )
                }
            }

            // Active Filters indicator
            if (statusFilter != "全部" || sortBy != "下期还款日") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { statusFilter = "全部" },
                        label = { Text("状态: $statusFilter") }
                    )
                    SuggestionChip(
                        onClick = { sortBy = "下期还款日" },
                        label = { Text("排序: $sortBy") }
                    )
                }
            }

            if (processedLoans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💡",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "没有找到符合条件的借款记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("loans_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(processedLoans, key = { it.id }) { loan ->
                        val nextPlan = nextPlanMap[loan.id]
                        val isOverdue = loanOverdueMap[loan.id] ?: false

                        // Calculate total paid and remaining using high performance cached map
                        val stats = loanStatsMap[loan.id] ?: LoanCalculatedStats(0.0, loan.principal, 0)
                        val paidSum = stats.paidSum
                        val remainingPrincipal = stats.remainingPrincipal
                        val paidPeriods = stats.paidPeriods

                        Card(
                            onClick = { onLoanClick(loan.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_card_${loan.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header: Borrower Name and Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = loan.borrowerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (loan.loanSource.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = loan.loanSource,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                            if (loan.note.isNotEmpty()) {
                                                Text(
                                                    text = loan.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isOverdue) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFEBEE))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "逾期",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                            }
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
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = loan.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (loan.status) {
                                                    "已结清" -> Color(0xFF2E7D32)
                                                    else -> Color(0xFFEF6C00)
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Financial Progress Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "借款本金",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "¥${String.format("%,.2f", loan.principal)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "剩余待还本金",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "¥${String.format("%,.2f", remainingPrincipal)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Progress Line Indicator
                                val progress = if (loan.principal > 0) (paidSum / loan.principal).toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Next payment details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "已还: $paidPeriods/${loan.totalPeriods} 期 (${loan.repaymentMethod})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (nextPlan != null) {
                                        Text(
                                            text = "下期还款: ${DateUtils.formatDisplayDate(nextPlan.dueDate)} / ¥${String.format("%.2f", nextPlan.totalAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "全部款项已结清",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
