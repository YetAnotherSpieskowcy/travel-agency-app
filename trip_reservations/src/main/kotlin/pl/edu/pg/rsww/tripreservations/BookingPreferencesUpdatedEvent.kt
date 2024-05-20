package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.Serializable

@Serializable
public data class BookingPreferencesUpdatedEvent(
    val triggeredBy: String,
)
