package org.unbrokendome.spring.blobstore.gcs.auth

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.*
import java.time.Clock
import java.time.Duration
import java.time.Instant


internal interface AccessTokenManager {

    val accessToken: Mono<AccessToken>


    fun provideRequestHeaders(): Mono<HttpHeaders> =
        accessToken
            .map { token ->
                HttpHeaders()
                    .apply { add(HttpHeaders.AUTHORIZATION, "Bearer ${token.encodedToken}") }
            }
            .switchIfEmpty { HttpHeaders().toMono() }
}


internal abstract class AbstractAccessTokenManager(
    protected val clock: Clock
) : AccessTokenManager {

    protected abstract fun retrieveAccessToken(): Mono<AccessToken>


    override val accessToken: Mono<AccessToken>
        get() = Mono.defer { retrieveAccessToken() }
            .cache(
                // ttlForValue
                { accessToken -> accessToken.expiresIn },
                // ttlForError
                { Duration.ofMillis(Long.MAX_VALUE) },
                // ttlForEmpty - irrelevant; retrieveAccessToken will never return an empty Mono
                { Duration.ZERO }
            )


    protected fun parseAccessTokenResponse(response: ClientResponse): Mono<AccessToken> {
        val responseDate = response.headers().asHttpHeaders()
            .date.takeUnless { it < 0 }?.let { Instant.ofEpochMilli(it) } ?: clock.instant()

        return response.bodyToMono<AccessTokenResponse>()
            .map { body ->

                val expiresIn = Duration.ofSeconds(body.expiresIn.toLong())

                AccessToken(
                    encodedToken = body.accessToken,
                    expiresIn = expiresIn,
                    expiration = responseDate.plus(expiresIn)
                )
            }
    }
}
