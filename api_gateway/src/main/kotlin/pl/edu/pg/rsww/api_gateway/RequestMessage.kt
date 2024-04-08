package pl.edu.pg.rsww.api_gateway

import kotlinx.serialization.Serializable


@Serializable
public data class RequestMessage(
    val serviceName: String,
    val path: String,
    val params: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
)
