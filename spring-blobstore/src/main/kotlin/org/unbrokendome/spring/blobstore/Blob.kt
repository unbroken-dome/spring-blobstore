package org.unbrokendome.spring.blobstore

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.PooledDataBuffer
import reactor.core.publisher.*


/**
 * Represents a blob that was retrieved from a [BlobStore].
 */
interface Blob : BlobMetadataBase {

    /**
     * The blob's data as a [Publisher] of [DataBuffer]s.
     *
     * In general, this [Publisher] may be subscribed to only once. If a client needs the data more than once,
     * it should use an appropriate caching/buffering mechanism (e.g. [reactor.core.publisher.Flux.share]).
     */
    val data: Publisher<DataBuffer>

    /**
     * Discards and releases the [data] of the blob. This *must* be called by a client after it retrieves a [Blob]
     * and then decides not to use the data after all. Note that this situation can (and should) be avoided in many
     * cases by retrieving only the blob's metadata first.
     *
     * If the [data] publisher signals an error, it will be suppressed by the returned [Mono], and it will still
     * complete empty.
     *
     * The default implementation subscribes to [data] and releases all the [DataBuffer]s if they are
     * [PooledDataBuffer]s. Implementing classes should override this method if discarding can be handled
     * differently. (For example, it may be a no-op if no resources are attached to the [Blob] before the
     * [data] publisher is subscribed to.)
     *
     * @return a [Mono] that completes empty after all the [data] has been discarded
     */
    @JvmDefault
    fun discard(): Mono<Void> =
            Flux.from(data)
                .doOnNext(DataBufferUtils.releaseConsumer())
                .onErrorResume { Mono.empty() }
                .then()
}
