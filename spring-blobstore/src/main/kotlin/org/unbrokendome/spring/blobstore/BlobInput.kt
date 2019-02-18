package org.unbrokendome.spring.blobstore

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.util.MimeType
import java.util.OptionalLong


interface BlobInput {

    val size: OptionalLong

    val contentType: MimeType

    val data: Publisher<DataBuffer>


    companion object {
        operator fun invoke(
            size: OptionalLong = OptionalLong.empty(),
            contentType: MimeType = MimeType.valueOf("application/octet-stream"),
            data: Publisher<DataBuffer>
        ): BlobInput =
            DefaultBlobInput(size, contentType, data)

        data class DefaultBlobInput(
            override val size: OptionalLong,
            override val contentType: MimeType,
            override val data: Publisher<DataBuffer>
        ) : BlobInput
    }
}
