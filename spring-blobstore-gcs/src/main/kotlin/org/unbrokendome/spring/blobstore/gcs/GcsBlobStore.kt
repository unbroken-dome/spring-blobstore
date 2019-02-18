package org.unbrokendome.spring.blobstore.gcs

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.unbrokendome.spring.blobstore.Blob
import org.unbrokendome.spring.blobstore.BlobAlreadyExistsException
import org.unbrokendome.spring.blobstore.BlobInput
import org.unbrokendome.spring.blobstore.BlobMetadata
import org.unbrokendome.spring.blobstore.BlobNotFoundException
import org.unbrokendome.spring.blobstore.BlobStore
import org.unbrokendome.spring.blobstore.gcs.util.checkStatusCode
import reactor.core.publisher.*
import java.nio.file.Path
import java.time.Instant


interface GcsBlobStore : BlobStore {

    val bucketName: String


    companion object {

        @JvmStatic
        fun builder(): GcsBlobStoreBuilder =
            GcsBlobStoreBuilder()
    }
}


internal class DefaultGcsBlobStore(
    webClientBuilder: WebClient.Builder,
    override val bucketName: String
) : GcsBlobStore {

    private val webClient = webClientBuilder
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs { codecs ->
                val objectMapper = ObjectMapper()
                    .registerModules(JavaTimeModule(), KotlinModule())
                    .setInjectableValues(InjectableValues.Std(mapOf("blobStore" to this)))

                codecs.defaultCodecs().jackson2JsonDecoder(
                    Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON)
                )
            }
            .build()
        )
        .build()


    override fun getMetadata(path: Path): Mono<BlobMetadata> {
        return webClient.get()
            .uri { b ->
                b.path("/storage/v1/b/{bucket}/o/{object}")
                    .queryParam("fields", "name,size,contentType,etag,updated,mediaLink")
                    .build(mapOf("bucket" to bucketName, "object" to path))
            }
            .retrieve()
            .bodyToMono<GcsBlobMetadata>()
            .cast()
    }


    override fun retrieve(metadata: BlobMetadata): Mono<Blob> {
        if (metadata !is GcsBlobMetadata || metadata.blobStore != this) {
            throw IllegalArgumentException("This BlobMetadata was created by a different BlobStore")
        }
        return webClient.get()
            .uri(metadata.mediaLink)
            .exchange()
            .checkStatusCode()
            .map<Blob> { clientResponse ->
                GcsBlob(this, metadata.path, clientResponse)
            }
            .onErrorMap(WebClientResponseException.NotFound::class) { error ->
                BlobNotFoundException(metadata.path, error)
            }
    }


    override fun retrieveDirect(
        path: Path,
        ifNoneMatch: Iterable<String>?,
        ifModifiedSince: Instant?
    ): Mono<Blob> =
        webClient.get()
            .uri { b ->
                b.path("storage/v1/b/{bucket}/o/{object}")
                    .queryParam("alt", "media")
                    .build(bucketName, path)
            }
            .headers { h ->
                if (ifNoneMatch != null) {
                    h.ifNoneMatch = ifNoneMatch.toList()
                }
                if (ifModifiedSince != null) {
                    h.ifModifiedSince = ifModifiedSince.toEpochMilli()
                }
            }
            .exchange()
            .checkStatusCode()
            .map { clientResponse ->
                GcsBlob(this, path, clientResponse)
            }


    override fun store(path: Path, input: BlobInput, failIfExists: Boolean): Mono<Void> {

        require(input.contentType.isConcrete) { "Blob content type must not contain wildcards" }

        return webClient.post()
            .uri { b ->
                b.path("upload/storage/v1/b/{bucket}/o")
                    .queryParam("uploadType", "media")
                    .queryParam("name", "{object}")
                    .apply {
                        if (failIfExists) {
                            queryParam("ifGenerationMatch", 0)
                        }
                    }
                    .build(mapOf("bucket" to bucketName, "object" to path))
            }
            .headers { h ->
                h.contentType = MediaType.asMediaType(input.contentType)
                input.size.let { optSize ->
                    if (optSize.isPresent) {
                        h.contentLength = optSize.asLong
                    } else {
                        h.add(HttpHeaders.TRANSFER_ENCODING, "chunked")
                    }
                }
            }
            .retrieve()
            .bodyToMono<Void>()
            .run {
                if (failIfExists) {
                    onErrorMap({ error ->
                        error is WebClientResponseException && error.statusCode == HttpStatus.PRECONDITION_FAILED
                    }) { error ->
                        BlobAlreadyExistsException(path, error)
                    }

                } else this
            }
            .then()
    }


    override fun delete(path: Path): Mono<Void> =
        webClient.delete()
            .uri { b ->
                b.path("storage/v1/b/{bucket}/o/{object}")
                    .build(bucketName, path)
            }
            .retrieve()
            .bodyToMono<Void>()
            .then()
}
