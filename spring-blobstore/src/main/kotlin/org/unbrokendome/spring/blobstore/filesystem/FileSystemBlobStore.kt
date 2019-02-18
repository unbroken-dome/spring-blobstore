package org.unbrokendome.spring.blobstore.filesystem

import org.reactivestreams.Publisher
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.unbrokendome.spring.blobstore.Blob
import org.unbrokendome.spring.blobstore.BlobAlreadyExistsException
import org.unbrokendome.spring.blobstore.BlobInput
import org.unbrokendome.spring.blobstore.BlobMetadata
import org.unbrokendome.spring.blobstore.BlobNotFoundException
import org.unbrokendome.spring.blobstore.BlobStore
import reactor.core.publisher.*
import reactor.core.scheduler.Schedulers
import java.io.ByteArrayOutputStream
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption.*
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import java.util.Properties


interface FileSystemBlobStore : BlobStore {

    val basePath: Path

}


class DefaultFileSystemBlobStore(
    override val basePath: Path,
    private val dataBufferFactory: DataBufferFactory,
    private val bufferSize: Int,
    private val clock: Clock,
    private val digestAlgorithm: String = "SHA-1"
) : FileSystemBlobStore {

    private val tempDir = basePath.resolve(".tmp")
        .also { Files.createDirectories(it) }


    override fun getMetadata(path: Path): Mono<BlobMetadata> {
        val resolvedPath = basePath.resolve(path)
        return Mono.defer {
            if (!Files.exists(resolvedPath)) {
                BlobNotFoundException(path).toMono()

            } else {
                DataBufferUtils.read(FileSystemResource(metadataFile(resolvedPath)), dataBufferFactory, bufferSize)
                    .let(DataBufferUtils::join)
                    .map { dataBuffer ->
                        val metadataProps = Properties().also { props ->
                            dataBuffer.asInputStream(true).use { input ->
                                props.load(input)
                            }
                        }

                        FileSystemBlob(this, resolvedPath, metadataProps, dataBufferFactory, bufferSize)
                    }
                    .onErrorMap(NoSuchFileException::class) { error ->
                        BlobNotFoundException(path, error)
                    }
            }
        }
    }


    override fun retrieve(metadata: BlobMetadata): Mono<Blob> {
        if (metadata is FileSystemBlob && metadata.blobStore == this) {
            return Mono.just(metadata)
        } else {
            throw IllegalArgumentException("This BlobMetadata was created by a different BlobStore")
        }
    }


    override fun store(path: Path, input: BlobInput, failIfExists: Boolean): Mono<Void> {
        require(input.contentType.isConcrete) { "Blob content type must not contain wildcards" }

        val resolvedPath = basePath.resolve(path)

        return Mono.fromCallable {

            if (failIfExists && Files.exists(resolvedPath)) {
                throw BlobAlreadyExistsException(path)
            }

            Files.createDirectories(resolvedPath.parent)

            Files.createTempFile(tempDir, "blob", null)

        }.subscribeOn(Schedulers.parallel())
            .flatMap { tempFile ->

                val metadataTempFile = metadataFile(tempFile)

                writeBlobData(input, tempFile)
                    .flatMap { digest -> writeMetadata(input, digest, metadataTempFile) }
                    .then(moveFile(tempFile, resolvedPath, !failIfExists))
                    .then(moveFile(metadataTempFile, metadataFile(resolvedPath), !failIfExists))
                    .doFinally {
                        Files.deleteIfExists(tempFile)
                        Files.deleteIfExists(metadataTempFile)
                    }
            }
    }


    override fun delete(path: Path): Mono<Void> {
        val resolvedPath = basePath.resolve(path)
        return Mono.fromRunnable<Void> {
            Files.delete(resolvedPath)
            Files.delete(metadataFile(resolvedPath))

        }.onErrorMap(NoSuchFileException::class) { error ->
            BlobNotFoundException(path, error)
        }
    }


    private fun writeBlobData(blobInput: BlobInput, targetFile: Path): Mono<String> =
        Mono.fromCallable { MessageDigest.getInstance(digestAlgorithm) }
            .flatMap { digester ->
                blobInput.data.writeTo(targetFile)
                    .doOnNext { buffer ->
                        digester.update(buffer.asByteBuffer())
                    }
                    .doOnNext(DataBufferUtils.releaseConsumer())
                    .then(Mono.fromCallable {
                        val digest = digester.digest()
                        Base64.getEncoder().encodeToString(digest)
                    })
            }


    private fun writeMetadata(blobInput: BlobInput, digest: String, targetFile: Path): Mono<Void> {
        val metadataProps = Properties().apply {
            setProperty("content-type", blobInput.contentType.toString())
            setProperty("etag", digest)
            setProperty("last-modified", clock.instant().toString())
        }

        val metadataBytes = ByteArrayOutputStream().use { metadataOut ->
            metadataProps.store(metadataOut, null)
            metadataOut.toByteArray()
        }

        return dataBufferFactory.wrap(metadataBytes).toMono()
            .writeTo(targetFile)
            .doOnNext(DataBufferUtils.releaseConsumer())
            .then()
    }


    private fun Publisher<DataBuffer>.writeTo(file: Path) =
        Flux.using(
            { AsynchronousFileChannel.open(file, WRITE, CREATE, TRUNCATE_EXISTING) },
            { channel ->
                DataBufferUtils.write(this, channel)
            },
            AsynchronousFileChannel::close
        )


    private fun moveFile(sourcePath: Path, targetPath: Path, replaceExisting: Boolean): Mono<Void> {
        return if (replaceExisting) {
            Mono.fromRunnable {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } else {
            Mono.fromRunnable {
                Files.copy(sourcePath, targetPath)
            }
        }
    }


    private fun metadataFile(path: Path) =
        path.resolveSibling(path.fileName.toString() + ".metadata")
}
