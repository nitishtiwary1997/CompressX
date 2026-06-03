package com.hire.smartcompress.domain.usecase

import com.hire.smartcompress.domain.model.DashboardStats
import com.hire.smartcompress.domain.repository.ICompressionRepository
import javax.inject.Inject

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: ICompressionRepository
) {
    suspend operator fun invoke(): DashboardStats = repository.getDashboardStats()
}
