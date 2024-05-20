package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.Serializable

@Serializable
public data class PaymentProcessedEvent(
    val triggeredBy: String,
    val success: Boolean,
)
