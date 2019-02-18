package org.unbrokendome.spring.blobstore.gcs.util

import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.*


internal class RetryExchangeFilterFunction(
    private val reactiveRetryFunction: ReactiveRetryFunction
) : ExchangeFilterFunction {

    constructor(
        numAttempts: Long = ReactiveRetryFunction.DefaultNumAttempts,
        retryable: RetryablePredicate = ReactiveRetryFunction.DefaultRetryablePredicate,
        backOffSelector: BackOffSelector = ReactiveRetryFunction.DefaultBackOffSelector
    ) : this(ReactiveRetryFunction(numAttempts, retryable, backOffSelector))


    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> =
        Mono.defer {
            next.exchange(request)
        }.retryWhen(reactiveRetryFunction)
}
