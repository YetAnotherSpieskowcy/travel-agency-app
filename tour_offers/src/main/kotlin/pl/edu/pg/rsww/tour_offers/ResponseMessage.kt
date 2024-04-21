package pl.edu.pg.rsww.tour_offers

import kotlinx.serialization.Serializable

@Serializable
public data class ResponseMessage(
        val status: Int,
        val headers: Map<String, String>,
        val body: String,
)