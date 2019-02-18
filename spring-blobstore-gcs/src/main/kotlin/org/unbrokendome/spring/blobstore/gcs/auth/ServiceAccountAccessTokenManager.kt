package org.unbrokendome.spring.blobstore.gcs.auth

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.unbrokendome.jsonwebtoken.Claims
import org.unbrokendome.jsonwebtoken.Jwt
import org.unbrokendome.jsonwebtoken.signature.SignatureAlgorithms
import org.unbrokendome.spring.blobstore.gcs.util.RandomExponentialBackoff
import org.unbrokendome.spring.blobstore.gcs.util.WebClientErrorFilters
import org.unbrokendome.spring.blobstore.gcs.util.retryWithBackoff
import reactor.core.publisher.*
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Clock
import java.time.Duration
import java.util.function.Predicate


internal class ServiceAccountAccessTokenManager(
    private val webClient: WebClient,
    private val credentials: ServiceAccountCredentials,
    clock: Clock = Clock.systemUTC(),
    private val tokenLifetime: Duration = Duration.ofHours(1L),
    private val encoderScheduler: Scheduler = Schedulers.parallel()
) : AbstractAccessTokenManager(clock) {

    private val retryErrorFilter = WebClientErrorFilters.forStatusCode(Predicate { status ->
        // Server error --- includes timeout errors, which use 500 instead of 408
        status.is5xxServerError ||
                // Forbidden error --- for historical reasons, used for rate_limit_exceeded
                // errors instead of 429
                status == HttpStatus.FORBIDDEN
    })


    private val jwtProcessor = Jwt.processor()
        .encodeOnly()
        .signWith(SignatureAlgorithms.RS256, credentials.privateKey)
        .header {
            it.type = "JWT"
            it.keyId = credentials.privateKeyId
        }
        .build()


    private fun claims(): Claims {
        val now = clock.instant()
        return Jwt.claims()
            .setIssuer(credentials.clientEmail)
            .set("scope", credentials.scopes.joinToString(separator = " "))
            .setAudience(credentials.tokenUri.toString())
            .setIssuedAt(now)
            .setExpiration(now.plus(tokenLifetime))
    }


    private fun encodeAssertion(): Mono<String> =
        Mono.fromSupplier { jwtProcessor.encode(claims()) }
            .subscribeOn(encoderScheduler)


    override fun retrieveAccessToken(): Mono<AccessToken> =
        encodeAssertion()
            .flatMap { assertion ->
                Mono.defer {
                    webClient.post()
                        .uri(credentials.tokenUri)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(
                            BodyInserters
                                .fromFormData("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                .with("assertion", assertion)
                        )
                        .exchange()
                        .flatMap { response ->
                            parseAccessTokenResponse(response)
                        }
                }.retryWithBackoff(retryErrorFilter)
            }
}
