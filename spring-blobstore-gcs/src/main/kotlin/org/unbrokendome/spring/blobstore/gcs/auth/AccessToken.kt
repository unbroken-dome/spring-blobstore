package org.unbrokendome.spring.blobstore.gcs.auth

import java.time.Duration
import java.time.Instant


internal data class AccessToken(
    val encodedToken: String,
    val expiresIn: Duration,
    val expiration: Instant
)
