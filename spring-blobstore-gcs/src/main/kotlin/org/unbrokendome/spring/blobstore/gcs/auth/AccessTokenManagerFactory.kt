package org.unbrokendome.spring.blobstore.gcs.auth

import org.springframework.web.reactive.function.client.WebClient
import java.time.Clock


internal interface AccessTokenManagerFactory {

    fun createAccessTokenManager(credentials: Credentials): AccessTokenManager
}


internal class DefaultAccessTokenManagerFactory(
    private val webClientBuilder: WebClient.Builder,
    private val clock: Clock
) : AccessTokenManagerFactory {

    override fun createAccessTokenManager(credentials: Credentials): AccessTokenManager =

        when (credentials) {
            is ServiceAccountCredentials ->
                ServiceAccountAccessTokenManager(
                    webClientBuilder.build(),
                    credentials,
                    clock
                )
            is AuthorizedUserCredentials ->
                AuthorizedUserAccessTokenManager(
                    webClientBuilder.build(),
                    credentials,
                    clock
                )
            else ->
                throw IllegalArgumentException("Unsupported credentials type")
        }
}
