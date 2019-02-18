package org.unbrokendome.spring.blobstore.filesystem

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSameContentAs
import assertk.assertions.hasText
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isRegularFile
import assertk.assertions.prop
import assertk.assertions.support.expected
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.util.MimeType
import org.unbrokendome.spring.blobstore.Blob
import org.unbrokendome.spring.blobstore.BlobInput
import org.unbrokendome.spring.blobstore.BlobMetadata
import org.unbrokendome.spring.blobstore.BlobStore
import reactor.core.publisher.*
import reactor.test.StepVerifier
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.OptionalLong


class FileSystemBlobStoreTest {

    private val dataBufferFactory: DataBufferFactory = DefaultDataBufferFactory()
    private val clock: Clock = Clock.fixed(Instant.parse("2019-03-19T15:36:04.295Z"), ZoneOffset.UTC)
    private lateinit var tempDir: Path
    private lateinit var blobStore: BlobStore

    private val testDataString = "Lorem ipsum dolor sit amet"
    private val mediaTypeTextPlain = MimeType.valueOf("text/plain;charset=UTF-8")

    @BeforeEach
    fun setup() {
        tempDir = Files.createTempDirectory("blobstore")
        blobStore = DefaultFileSystemBlobStore(
            basePath = tempDir,
            dataBufferFactory = DefaultDataBufferFactory(),
            bufferSize = 512,
            clock = clock
        )
    }


    @AfterEach
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }


    private fun testDataBuffer() = dataBufferFactory.wrap(testDataString.toByteArray())


    private fun blobInput(): BlobInput =
        BlobInput(
            data = Mono.just(testDataBuffer()),
            size = OptionalLong.of(testDataString.length.toLong()),
            contentType = mediaTypeTextPlain
        )


    @Test
    fun `store operation should create a blob file`() {

        val path = Paths.get("foo/bar")

        StepVerifier.create(blobStore.store(path, blobInput()))
            .verifyComplete()

        assertThat(tempDir.resolve(path)).all {
            isRegularFile()
            transform { it.toFile() }.hasText(testDataString)
        }
    }


    @Test
    fun `should retrieve metadata of stored blob`() {

        val path = Paths.get("foo/bar")

        val metadataResult = blobStore.store(path, blobInput())
            .then(blobStore.getMetadata(path))

        StepVerifier.create(metadataResult)
            .assertNext { metadata ->
                assertThat(metadata).all {
                    prop(BlobMetadata::contentType).isEqualTo(mediaTypeTextPlain)
                    prop(BlobMetadata::size).isEqualTo(testDataString.length.toLong())
                    prop(BlobMetadata::etag).isNotNull()
                    prop(BlobMetadata::lastModified).isEqualTo(clock.instant())
                }
            }
            .verifyComplete()
    }


    @Test
    fun `should retrieve contents of stored blob`() {

        val path = Paths.get("foo/bar")

        val blobResult = blobStore.store(path, blobInput())
            .then(blobStore.getMetadata(path)
                .flatMap { metadata -> blobStore.retrieve(metadata) })

        StepVerifier.create(blobResult)
            .assertNext { blob ->
                assertThat(blob).all {
                    prop(Blob::contentType).isEqualTo(mediaTypeTextPlain)
                    prop(Blob::size).isEqualTo(testDataString.length.toLong())
                    prop(Blob::etag).isNotNull()
                    prop(Blob::lastModified).isEqualTo(clock.instant())
                }

                StepVerifier.create(DataBufferUtils.join(blob.data))
                    .assertNext { buffer ->
                        assertThat(buffer.asInputStream(true))
                            .hasSameContentAs(ByteArrayInputStream(testDataString.toByteArray()))
                    }
                    .verifyComplete()
            }
            .verifyComplete()
    }


    @Test
    fun `should delete stored blob`() {

        val path = Paths.get("foo/bar")

        val deleteResult = blobStore.store(path, blobInput())
            .then(blobStore.delete(path))

        StepVerifier.create(deleteResult)
            .verifyComplete()

        assertThat(tempDir.resolve(path)).all {
            transform { it.toFile() }.given {
                if (!it.exists()) return@given
                expected("to not exist")
            }
        }
    }
}
