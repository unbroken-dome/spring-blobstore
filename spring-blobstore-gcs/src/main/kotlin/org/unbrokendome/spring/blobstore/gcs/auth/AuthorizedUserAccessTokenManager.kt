package org.unbrokendome.spring.blobstore.gcs.auth

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.unbrokendome.spring.blobstore.gcs.util.retryWithBackoff
import reactor.core.publisher.*
import java.time.Clock


internal class AuthorizedUserAccessTokenManager(
    private val webClient: WebClient,
    private val credentials: AuthorizedUserCredentials,
    clock: Clock
) : AbstractAccessTokenManager(clock) {

    override fun retrieveAccessToken(): Mono<AccessToken> =
        Mono.defer {
            webClient.post()
                .uri(credentials.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                    BodyInserters.fromFormData("client_id", credentials.clientId)
                        .with("client_secret", credentials.clientSecret)
                        .with("refresh_token", credentials.refreshToken)
                        .with("grant_type", "refresh_token")
                )
                .exchange()
                .flatMap { response ->
                    parseAccessTokenResponse(response)
                }
        }.retryWithBackoff()
}
