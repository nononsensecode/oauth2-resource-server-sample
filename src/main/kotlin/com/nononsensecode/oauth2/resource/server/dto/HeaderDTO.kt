package com.nononsensecode.oauth2.resource.server.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HeaderDTO(
    @JsonProperty(value = "alg")
    val algorithm: String,

    @JsonProperty(value = "kid")
    val keyId: String,

    @JsonProperty(value = "typ", required = false)
    val type: String
)