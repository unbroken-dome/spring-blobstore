package org.unbrokendome.spring.blobstore.gcs.util

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.*
import java.util.function.Predicate


internal object WebClientErrorFilters {

    fun forStatusCode(statusCodePredicate: Predicate<HttpStatus>): Predicate<Throwable> =
        Predicate { error ->
            error is WebClientResponseException && statusCodePredicate.test(error.statusCode)
        }


    val DEFAULT = forStatusCode(Predicate { status ->
        status == HttpStatus.INTERNAL_SERVER_ERROR || status == HttpStatus.SERVICE_UNAVAILABLE
    })
}


fun <T> Mono<T>.retryWithBackoff(
    errorFilter: Predicate<Throwable> = WebClientErrorFilters.DEFAULT,
    numRetries: Long = 10L
): Mono<T> {
    return retryWhen(RandomExponentialBackoff(errorFilter, numRetries))
}
