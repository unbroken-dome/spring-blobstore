package org.unbrokendome.spring.blobstore

import org.springframework.util.MimeType
import java.nio.file.Path
import java.time.Instant


/**
 * Defines common metadata for a [Blob].
 */
interface BlobMetadataBase {

    /**
     * The path of the blob within this BlobStore.
     */
    val path: Path

    /**
     * The size, in bytes, of the blob data.
     */
    val size: Long

    /**
     * The content type of the blob data.
     */
    val contentType: MimeType

    /**
     * An ETag value (for example, a digest) for the blob data.
     *
     * May be `null` if no ETag is available.
     */
    val etag: String?
        get() = null

    /**
     * The point in time when this blob was last modified, as an [Instant].
     */
    val lastModified: Instant?
        get() = null
}
