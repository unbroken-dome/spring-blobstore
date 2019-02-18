package org.unbrokendome.spring.blobstore.gcs

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.unbrokendome.spring.blobstore.gcs.auth.AuthorizationExchangeFilterFunction
import org.unbrokendome.spring.blobstore.gcs.auth.Credentials
import org.unbrokendome.spring.blobstore.gcs.auth.DefaultAccessTokenManagerFactory
import org.unbrokendome.spring.blobstore.gcs.util.ReactiveBackOff
import org.unbrokendome.spring.blobstore.gcs.util.RetryExchangeFilterFunction
import java.time.Clock


class GcsBlobStoreBuilder {

    private var webClientBuilder: WebClient.Builder? = null
    private lateinit var bucketName: String

    private var clock: Clock? = null
    private var credentials: Credentials? = null


    fun withWebClientBuilder(webClientBuilder: WebClient.Builder) = apply {
        this.webClientBuilder = webClientBuilder
    }


    fun withBucketName(bucketName: String) = apply {
        this.bucketName = bucketName
    }


    fun withCredentials(credentials: Credentials) = apply {
        this.credentials = credentials
    }


    fun withClock(clock: Clock) = apply {
        this.clock = clock
    }


    fun build(): GcsBlobStore {

        val webClientBuilder = webClientBuilder ?: WebClient.builder()

        val webClient = webClientBuilder.clone()

        credentials?.let { credentials ->

            val authRetry = RetryExchangeFilterFunction(
                retryable = ::authRetryableFilter,
                backOffSelector = ::backOffSelector
            )

            val authWebClient = webClientBuilder.clone()
                .filter(authRetry)

            val accessTokenManagerFactory = DefaultAccessTokenManagerFactory(
                authWebClient,
                clock = clock ?: Clock.systemUTC()
            )

            val accessTokenManager = accessTokenManagerFactory.createAccessTokenManager(credentials)

            webClient.filter(AuthorizationExchangeFilterFunction(accessTokenManager))
        }

        val mainRetry = RetryExchangeFilterFunction(
            retryable = ::mainRetryableFilter,
            backOffSelector = ::backOffSelector
        )
        webClient.filter(mainRetry)

        return DefaultGcsBlobStore(
            webClientBuilder = webClient,
            bucketName = bucketName
        )
    }


    private companion object {

        private fun mainRetryableFilter(error: Throwable): Boolean =
            error is WebClientResponseException && error.statusCode.is5xxServerError


        private fun authRetryableFilter(error: Throwable): Boolean =
            mainRetryableFilter(error) || error is WebClientResponseException.Forbidden


        @Suppress("UNUSED_PARAMETER")
        private fun backOffSelector(error: Throwable): ReactiveBackOff =
            ReactiveBackOff.exponential().jitter()
    }
}
