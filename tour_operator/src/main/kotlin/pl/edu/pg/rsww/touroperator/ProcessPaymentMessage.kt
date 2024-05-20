package pl.edu.pg.rsww.touroperator

import kotlinx.serialization.Serializable

@Serializable
public data class ProcessPaymentMessage(
    val triggeredBy: String,
)
