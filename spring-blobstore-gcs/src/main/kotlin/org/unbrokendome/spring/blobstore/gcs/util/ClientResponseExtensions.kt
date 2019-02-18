package org.unbrokendome.spring.blobstore.gcs.util

import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.*


internal fun ClientResponse.checkStatusCode(): Mono<ClientResponse> =
    if (statusCode().isError) {
        bodyToMono<ByteArray>()
            .defaultIfEmpty(byteArrayOf())
            .flatMap { bodyBytes ->
                WebClientResponseException.create(
                    rawStatusCode(),
                    statusCode().reasonPhrase,
                    headers().asHttpHeaders(),
                    bodyBytes,
                    headers().contentType().map { it.charset }.orElse(null)
                )
                    .toMono<ClientResponse>()
            }

    } else {
        toMono()
    }


internal fun Mono<ClientResponse>.checkStatusCode(): Mono<ClientResponse> =
    flatMap { clientResponse ->
        clientResponse.checkStatusCode()
    }
