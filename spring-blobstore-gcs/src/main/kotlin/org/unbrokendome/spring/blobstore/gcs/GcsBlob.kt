package org.unbrokendome.spring.blobstore.gcs

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.ClientResponse
import org.unbrokendome.spring.blobstore.Blob
import java.nio.file.Path
import java.time.Instant


internal class GcsBlob(
    val blobStore: GcsBlobStore,
    override val path: Path,
    clientResponse: ClientResponse
) : Blob {

    override val size: Long =
        clientResponse.headers().contentLength().asLong

    override val contentType: MediaType =
        clientResponse.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM)

    override val data: Publisher<DataBuffer> =
        clientResponse.body(BodyExtractors.toDataBuffers())

    override val etag: String? =
        clientResponse.headers().asHttpHeaders().eTag

    override val lastModified: Instant? =
        clientResponse.headers().asHttpHeaders().lastModified
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it) }
}
