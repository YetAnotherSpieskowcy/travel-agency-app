package pl.edu.pg.rsww.pricecalculator

import kotlinx.serialization.Serializable

@Serializable
public data class ResponseMessage(
    val status: Int,
    val headers: Map<String, String>,
    val body: String,
)
