package pl.edu.pg.rsww.touroperator

import kotlinx.serialization.Serializable

@Serializable
public data class BookTripMessage(
    val triggeredBy: String,
    val userId: String,
    val tripId: String,
)
