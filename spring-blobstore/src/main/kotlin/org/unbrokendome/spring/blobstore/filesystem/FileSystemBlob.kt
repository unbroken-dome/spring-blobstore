package org.unbrokendome.spring.blobstore.filesystem

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.util.MimeType
import org.unbrokendome.spring.blobstore.Blob
import org.unbrokendome.spring.blobstore.BlobMetadata
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.Properties


internal class FileSystemBlob(
    val blobStore: FileSystemBlobStore,
    override val path: Path,
    private val metadata: Properties,
    private val dataBufferFactory: DataBufferFactory,
    private val bufferSize: Int
) : Blob, BlobMetadata {

    override val size: Long
        get() = Files.size(path)


    override val contentType: MimeType
        get() = MimeType.valueOf(metadata.getProperty("content-type"))


    override val etag: String?
        get() = metadata.getProperty("etag")


    override val lastModified: Instant?
        get() = Instant.parse(metadata.getProperty("last-modified"))


    override val data: Publisher<DataBuffer>
        get() = DataBufferUtils.readAsynchronousFileChannel(
            { AsynchronousFileChannel.open(path, StandardOpenOption.READ) },
            dataBufferFactory, bufferSize
        )
}
