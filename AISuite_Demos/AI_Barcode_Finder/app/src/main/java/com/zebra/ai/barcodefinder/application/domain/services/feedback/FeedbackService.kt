package com.zebra.ai.barcodefinder.application.domain.services.feedback

import com.zebra.ai.barcodefinder.sdkcoordinator.model.FeedbackType

/**
 * Domain interface for providing user feedback.
 * Implementation will be in the Data layer.
 */
interface FeedbackService {
    fun triggerFeedback(type: FeedbackType)
    fun pause()
    fun resume()
    fun release()
}