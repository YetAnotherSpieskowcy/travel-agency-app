package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.Serializable

@Serializable
public data class CancelBookTripMessage(
    val triggeredBy: String,
    val userId: String,
    val tripId: String,
)
