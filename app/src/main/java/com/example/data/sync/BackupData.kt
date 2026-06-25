package com.example.data.sync

import com.example.data.model.Loan
import com.example.data.model.RepaymentPlan
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val loans: List<Loan>,
    val repaymentPlans: List<RepaymentPlan>
)
