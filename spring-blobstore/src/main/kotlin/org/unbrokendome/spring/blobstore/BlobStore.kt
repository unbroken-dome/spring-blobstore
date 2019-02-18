package org.unbrokendome.spring.blobstore

import reactor.core.publisher.*
import java.nio.file.Path
import java.time.Instant


interface BlobStore {

    fun getMetadata(path: Path): Mono<BlobMetadata>


    fun retrieve(metadata: BlobMetadata): Mono<Blob>


    @JvmDefault
    fun retrieveDirect(
        path: Path,
        ifNoneMatch: Iterable<String>? = null,
        ifModifiedSince: Instant? = null
    ): Mono<Blob> =
        getMetadata(path)
            .flatMap { metadata ->
                ifNoneMatch?.toSet()?.takeIf { it.isNotEmpty() }
                    ?.let { ifNoneMatch ->
                        val etag = metadata.etag
                        if (etag != null && etag in ifNoneMatch) {
                            return@flatMap Mono.empty<Blob>()
                        }
                    }

                if (ifModifiedSince != null) {
                    val lastModified = metadata.lastModified
                    if (lastModified != null && lastModified < ifModifiedSince) {
                        return@flatMap Mono.empty<Blob>()
                    }
                }

                retrieve(metadata)
            }


    fun store(path: Path, input: BlobInput, failIfExists: Boolean = false): Mono<Void>


    fun delete(path: Path): Mono<Void>
}
