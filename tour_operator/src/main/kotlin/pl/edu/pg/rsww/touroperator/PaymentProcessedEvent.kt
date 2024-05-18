package pl.edu.pg.rsww.touroperator

import kotlinx.serialization.Serializable

@Serializable
public data class PaymentProcessedEvent(
    val triggeredBy: String,
    val success: Boolean,
)
