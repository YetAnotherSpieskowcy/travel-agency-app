package pl.edu.pg.rsww.tripreservations

import kotlinx.serialization.Serializable

@Serializable
public data class UpdateBookingPreferencesMessage(
    val triggeredBy: String,
)
