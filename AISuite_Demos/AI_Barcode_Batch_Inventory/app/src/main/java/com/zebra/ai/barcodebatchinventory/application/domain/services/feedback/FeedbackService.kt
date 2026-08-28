package com.zebra.ai.barcodebatchinventory.application.domain.services.feedback

import com.zebra.ai.barcodebatchinventory.sdkcoordinator.model.FeedbackType

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