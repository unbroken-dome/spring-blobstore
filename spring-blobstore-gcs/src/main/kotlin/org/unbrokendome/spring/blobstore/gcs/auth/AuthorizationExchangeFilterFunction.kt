package org.unbrokendome.spring.blobstore.gcs.auth

import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.*


internal class AuthorizationExchangeFilterFunction(
    private val accessTokenManager: AccessTokenManager
) : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> =
        accessTokenManager.provideRequestHeaders()
            .flatMap { authHeaders ->
                val authorizedRequest = ClientRequest.from(request)
                    .headers { h ->
                        h.addAll(authHeaders)
                    }
                    .build()
                next.exchange(authorizedRequest)
            }
}
