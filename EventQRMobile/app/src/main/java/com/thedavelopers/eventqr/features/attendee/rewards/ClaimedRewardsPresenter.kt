package com.thedavelopers.eventqr.features.attendee

import com.thedavelopers.eventqr.core.api.NetworkResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ClaimedRewardsPresenter(
    private var view: ClaimedRewardsContract.View?,
    private val repository: AttendeeRepository,
) {
    private var job: Job? = null

    fun detach() {
        job?.cancel()
        view = null
    }

    fun loadRedemptions(eventId: String) {
        view?.showLoading(true)
        job = kotlinx.coroutines.MainScope().launch {
            when (val redemptionsResult = repository.getRewardRedemptions(eventId)) {
                is NetworkResult.Success -> {
                    val rewardNames = when (val rewardsResult = repository.getRewardsByEvent(eventId)) {
                        is NetworkResult.Success -> rewardsResult.data.associate { it.rewardId.toString() to it.name }
                        else -> emptyMap()
                    }

                    val eventTitle = when (val eventResult = repository.getEvent(eventId)) {
                        is NetworkResult.Success -> eventResult.data.title
                        else -> null
                    }

                    view?.showLoading(false)
                    view?.renderRedemptions(redemptionsResult.data, eventTitle, rewardNames)
                }
                is NetworkResult.Error -> {
                    view?.showLoading(false)
                    view?.showError(redemptionsResult.message)
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}
