package org.unbrokendome.spring.blobstore

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.util.MimeType
import java.util.OptionalLong


/**
 * Input for storing a [Blob] in a [BlobStore].
 */
interface BlobInput {

    /**
     * The size of the blob [data] in bytes, if known. May be an empty [OptionalLong] if the
     * size is not known in advance.
     */
    val size: OptionalLong

    /**
     * The content type of the blob data.
     */
    val contentType: MimeType

    /**
     * The blob data as a [Publisher] of [DataBuffer]s.
     */
    val data: Publisher<DataBuffer>


    companion object {

        /**
         * Creates a new [BlobInput].
         *
         * @param data the blob data as a [Publisher] of [DataBuffer]s
         * @param size the size of the blob [data] in bytes, or an empty [OptionalLong] if not known
         * @param contentType the content type of the blob data, defaults to `application/octet-stream`
         * @return a [BlobInput]
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            data: Publisher<DataBuffer>,
            size: OptionalLong,
            contentType: MimeType = MimeType.valueOf("application/octet-stream")
        ): BlobInput =
                DefaultBlobInput(size, contentType, data)


        /**
         * Creates a new [BlobInput].
         *
         * @param data the blob data as a [Publisher] of [DataBuffer]s
         * @param size the size of the blob [data] in bytes, or an empty [OptionalLong] if not known
         * @param contentType the content type of the blob data, defaults to `application/octet-stream`
         * @return a [BlobInput]
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            data: Publisher<DataBuffer>,
            size: Long? = null,
            contentType: MimeType = MimeType.valueOf("application/octet-stream")
        ): BlobInput =
            create(
                data = data,
                size = size?.let(OptionalLong::of) ?: OptionalLong.empty(),
                contentType = contentType
            )


        /**
         * Creates a new [BlobInput].
         *
         * @param data the blob data as a [Publisher] of [DataBuffer]s
         * @param size the size of the blob [data] in bytes, or an empty [OptionalLong] if not known
         * @param contentType the content type of the blob data, defaults to `application/octet-stream`
         * @return a [BlobInput]
         */
        operator fun invoke(
            data: Publisher<DataBuffer>,
            size: OptionalLong = OptionalLong.empty(),
            contentType: MimeType = MimeType.valueOf("application/octet-stream")
        ): BlobInput =
            create(data, size, contentType)


        /**
         * Creates a new [BlobInput].
         *
         * @param data the blob data as a [Publisher] of [DataBuffer]s.
         * @param size the size of the blob [data] in bytes, or `null` if not known
         * @param contentType the content type of the blob data
         * @return a [BlobInput]
         */
        operator fun invoke(
            data: Publisher<DataBuffer>,
            size: Long? = null,
            contentType: MimeType = MimeType.valueOf("application/octet-stream")
        ): BlobInput =
            create(data, size, contentType)
    }
}


private class DefaultBlobInput(
    override val size: OptionalLong,
    override val contentType: MimeType,
    override val data: Publisher<DataBuffer>
) : BlobInput
