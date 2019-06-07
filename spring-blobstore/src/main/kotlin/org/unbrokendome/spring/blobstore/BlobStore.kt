package org.unbrokendome.spring.blobstore

import org.springframework.core.io.buffer.PooledDataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import reactor.core.publisher.*
import java.nio.file.Path
import java.time.Instant


/**
 * Interface for interacting with a blob store.
 */
interface BlobStore {

    /**
     * Gets the metadata of a blob without actually retrieving the blob data.
     *
     * If the blob is not found in the blob store, the returned [Mono] will fail with a [BlobNotFoundException].
     *
     * @param path the path of the blob within the blob store
     * @return a [Mono] that will return the [BlobMetadata] or fail with a [BlobStoreException]
     */
    fun getMetadata(path: Path): Mono<BlobMetadata>


    /**
     * Retrieves a [Blob] from the blob store after its metadata has been retrieved using [getMetadata].
     *
     * The returned [Mono] may produce a [BlobNotFoundException] error if the blob was deleted since the call
     * to [getMetadata], or it may return a valid [Blob] that only fails when its [data][Blob.data] publisher is
     * subscribed to. The exact behavior is implementation-dependent, and clients should be prepared for both
     * this [Mono] and the [Blob.data] to produce errors.
     *
     * @param metadata the [BlobMetadata] that was previously retrieved by [getMetadata]
     * @return a [Mono] that will return the [Blob] or fail with a [BlobStoreException]
     *
     * @throws IllegalArgumentException if the supplied [BlobMetadata] was created by a different [BlobStore]
     */
    fun retrieve(metadata: BlobMetadata): Mono<Blob>


    /**
     * Retrieves a blob's data and metadata directly.
     *
     * The default implementation first calls [getMetadata], compares the metadata with the [ifNoneMatch] and
     * [ifModifiedSince] arguments (if any), and then conditionally retrieves the actual blob data using [retrieve].
     * Concrete blob store implementations should override this method if they have a more efficient way of retrieving
     * the blob.
     *
     * If the blob does not exist in the blob store, the returned [Mono] will produce a [BlobNotFoundException]
     * rather than complete empty.
     *
     * If the [Mono] completes successfully and produces a [Blob], then the client *must* consume the blob's
     * [data][Blob.data] and [release][PooledDataBuffer.release] them after use if they are [PooledDataBuffer]s.
     * The [DataBufferUtils.releaseConsumer] helper method is useful for this.
     *
     * @param path the path of the blob within the blob store
     * @param ifNoneMatch an optional list of etag values. If the blob's [etag][BlobMetadataBase.etag] matches one
     *        of these, then a retrieval should be skipped
     * @param ifModifiedSince an optional timestamp indicating that a retrieval should be skipped if the
     *        blob's [BlobMetadataBase.lastModified] timestamp is older than this
     *
     * @return a [Mono] that will return the [Blob] when subscribed to, or fail with a [BlobStoreException]. If
     *         retrieval of the blob was skipped because of the values of the [ifNoneMatch] or [ifModifiedSince]
     *         parameters, then the [Mono] will complete empty.
     */
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


    /**
     * Stores a blob in the blob store.
     *
     * If a blob already exists at the given [path], then the behavior depends on the [failIfExists] parameter:
     * If it is `true`, then the returned [Mono] produces a [BlobAlreadyExistsException]
     * error; if it is `false` then the existing blob will be overwritten with the new one.
     *
     * This operation is atomic in the sense that if it fails, the blob will _not_ exist (not even partially)
     * afterwards.
     *
     * @param path the path under which the blob should be placed
     * @param input a [BlobInput] describing the blob and its data; use [BlobInput.create] to produce an instance
     * @param failIfExists whether to fail (`true`) or overwrite (`false`) if a blob already exists at [path].
     * @return a [Mono] that completes empty if the operation succeeds, or produces a [BlobStoreException]
     *         error if the operation fails
     */
    fun store(path: Path, input: BlobInput, failIfExists: Boolean = false): Mono<Void>


    /**
     * Deletes a blob from the blob store.
     *
     * If no blob exists at the given path, the returned [Mono] produces a [BlobNotFoundException] error.
     * Use the [deleteIfExists] method if the operation should not fail in that case.
     *
     * @param path the path of the blob within the blob store
     *
     * @return a [Mono] that completes empty if the operation succeeds, or produces a [BlobStoreException]
     *         error if the operation fails
     */
    fun delete(path: Path): Mono<Void>


    /**
     * Deletes a blob from the blob store.
     *
     * This variant will not produce an error if the blob does not exist, but return the result as a [Boolean].
     * It will, however, still produce an error for other exceptional cases, for example in case of connectivity
     * issues.
     *
     * @param path the path of the blob within the blob store
     * @return a [Mono] that completes successfully with `true` if the blob was deleted,
     *         completes successfully with `false` if the blob did not exist, or
     *         produces a [BlobStoreException] error
     */
    @JvmDefault
    fun deleteIfExists(path: Path): Mono<Boolean> =
            delete(path)
                .thenReturn(true)
                .onErrorResume(BlobNotFoundException::class.java) { Mono.just(false) }
}
