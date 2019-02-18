package org.unbrokendome.spring.blobstore.gcs.auth

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
internal class AccessTokenResponse
@JsonCreator constructor(
    @param:JsonProperty("access_token", required = true)
    val accessToken: String,
    @param:JsonProperty("expires_in", required = true)
    val expiresIn: Int
)
