package io.thernal.navkit.sample.shared.results

/** What the review flow hands back. A result is a value, not a signal. */
data class ReviewDecision(
    val isApproved: Boolean,
    val note: String,
)
