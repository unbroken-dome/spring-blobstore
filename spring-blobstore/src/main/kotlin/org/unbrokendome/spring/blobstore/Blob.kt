package org.unbrokendome.spring.blobstore

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer


interface Blob : BlobMetadataBase {

    val data: Publisher<DataBuffer>
}
