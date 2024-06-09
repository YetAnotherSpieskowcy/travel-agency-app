package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.Serializable

@Serializable
public data class ChangeTripMultiplierMessage(
    val triggeredBy: String,
    val tripId: String,
    val newValue: Double,
)
