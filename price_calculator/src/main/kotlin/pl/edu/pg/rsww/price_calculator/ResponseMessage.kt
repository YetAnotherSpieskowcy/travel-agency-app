package pl.edu.pg.rsww.price_calculator

import kotlinx.serialization.Serializable

@Serializable
public data class ResponseMessage(
        val status: Int,
        val headers: Map<String, String>,
        val body: String,
)