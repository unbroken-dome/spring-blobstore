package org.unbrokendome.spring.blobstore

import org.springframework.util.MimeType
import java.nio.file.Path
import java.time.Instant


interface BlobMetadataBase {

    val path: Path

    val size: Long

    val contentType: MimeType

    val etag: String?
        get() = null

    val lastModified: Instant?
        get() = null
}
