package com.zebra.ai.barcodebatchinventory.application.domain.services.capture

/**
 * Decides when a dynamic-timeout capture session should end.
 *
 * Feed it one call to [onFrame] per processed camera frame, plus [onTick] from a timer so the
 * deadline still fires if frames stop arriving. It is not thread-safe; call it from a single
 * thread (the ViewModel's main scope).
 *
 * Rules:
 * - The max timeout starts at capture start and is re-armed exactly once, on the first frame
 *   that localizes anything (or decodes, whichever comes first). Later frames do NOT extend it.
 * - No decode by the deadline -> [Outcome.NO_BARCODES] (empty scene).
 * - Otherwise the capture ends early once every barcode entity in the frame is decoded, for a
 *   stability window of consecutive frames, with no new unique value in that window.
 *   Completion is judged per frame (entities in view that are decoded against entities in view),
 *   so duplicate values and scene changes do not skew it. The gate collects the unique values
 *   itself, from the frames it is fed, so a new decode counts at the frame that carried it. The
 *   unique count is only used to notice new decodes and to tell an empty scene from a timed-out one.
 * - Non-barcode regions that were detected can never decode. When only
 *   [Config.unresolvedTolerance] or fewer entities in the frame stay undecoded, a longer
 *   stability window is required before exiting instead of waiting for the deadline.
 */
class DynamicCaptureGate(
    private val startTimeMs: Long,
    private val config: Config = Config()
) {
    data class Config(
        /** Max time from capture start (or from first localization) before the capture ends. */
        val maxTimeoutMs: Long = 3000L,
        /** Frames that must be seen before an early exit is allowed. */
        val minFramesBeforeExit: Int = 10,
        /** Consecutive frames with every entity decoded, and no new unique decode, to exit. */
        val stabilityFrames: Int = 8,
        /** Undecoded entities per frame (likely non-barcode regions) tolerated for early exit. */
        val unresolvedTolerance: Int = 2,
        /** Consecutive frames within the tolerance, and no new unique decode, to exit. */
        val toleranceStabilityFrames: Int = 24
    )

    enum class Outcome {
        /** Every entity in view stayed decoded for the stability window. */
        ALL_DECODED,
        /** Early exit with a few detections that never decoded (likely non-barcode regions). */
        ALL_DECODED_WITH_UNRESOLVED,
        /** Deadline reached after at least one decode. */
        TIMED_OUT,
        /** Deadline reached with zero decodes: treated as a scene without barcodes. */
        NO_BARCODES
    }

    var outcome: Outcome? = null
        private set

    val isFinished: Boolean get() = outcome != null

    private var frameIndex = 0
    private val decodedValuesSeen = HashSet<String>()
    private val uniqueDecoded: Int get() = decodedValuesSeen.size
    private var lastNewDecodeFrame = 0
    private var firstLocalizationMs: Long? = null
    private var firstDecodeMs: Long? = null
    private var lastNewDecodeMs: Long? = null
    private var finishedMs: Long? = null

    private var resolvedRun = 0
    private var tolerantRun = 0

    /** Entities in the latest frame that are localized but not decoded. */
    var unresolvedCount = 0
        private set

    /**
     * @param detectedCount all barcode entities in this frame (decoded + localized-only)
     * @param decodedValues the decoded value of each decoded entity in this frame (duplicates kept)
     * @return the outcome if this frame ended the capture, otherwise null
     */
    fun onFrame(nowMs: Long, detectedCount: Int, decodedValues: List<String>): Outcome? {
        if (isFinished) return outcome
        frameIndex++

        unresolvedCount = (detectedCount - decodedValues.size).coerceAtLeast(0)
        // A frame with nothing in view says nothing about the scene, so it breaks both runs
        val hasScene = detectedCount > 0
        resolvedRun = if (hasScene && unresolvedCount == 0) resolvedRun + 1 else 0
        tolerantRun = if (hasScene && unresolvedCount <= config.unresolvedTolerance) tolerantRun + 1 else 0

        if (firstLocalizationMs == null && (detectedCount > 0 || decodedValues.isNotEmpty())) {
            firstLocalizationMs = nowMs
        }
        val uniqueBefore = uniqueDecoded
        decodedValuesSeen.addAll(decodedValues)
        if (uniqueDecoded > uniqueBefore) {
            lastNewDecodeFrame = frameIndex
            lastNewDecodeMs = nowMs
            if (firstDecodeMs == null) firstDecodeMs = nowMs
        }

        deadlineOutcome(nowMs)?.let { return finish(it, nowMs) }

        if (uniqueDecoded == 0 || frameIndex < config.minFramesBeforeExit) return null

        val framesSinceNewDecode = frameIndex - lastNewDecodeFrame
        return when {
            resolvedRun >= config.stabilityFrames && framesSinceNewDecode >= config.stabilityFrames ->
                finish(Outcome.ALL_DECODED, nowMs)
            tolerantRun >= config.toleranceStabilityFrames &&
                    framesSinceNewDecode >= config.toleranceStabilityFrames ->
                finish(Outcome.ALL_DECODED_WITH_UNRESOLVED, nowMs)
            else -> null
        }
    }

    /** Checks only the deadline; call periodically so a stalled frame stream still ends. */
    fun onTick(nowMs: Long): Outcome? {
        if (isFinished) return outcome
        return deadlineOutcome(nowMs)?.let { finish(it, nowMs) }
    }

    private fun deadlineOutcome(nowMs: Long): Outcome? {
        val deadline = (firstLocalizationMs ?: startTimeMs) + config.maxTimeoutMs
        if (nowMs < deadline) return null
        return if (uniqueDecoded == 0) Outcome.NO_BARCODES else Outcome.TIMED_OUT
    }

    private fun finish(result: Outcome, nowMs: Long): Outcome {
        outcome = result
        finishedMs = nowMs
        return result
    }
}
