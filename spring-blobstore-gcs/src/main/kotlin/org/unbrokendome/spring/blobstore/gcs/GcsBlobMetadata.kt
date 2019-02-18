package org.unbrokendome.spring.blobstore.gcs

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.OptBoolean
import org.springframework.http.MediaType
import org.unbrokendome.spring.blobstore.BlobMetadata
import java.net.URI
import java.nio.file.Path
import java.time.Instant


@JsonIgnoreProperties(ignoreUnknown = true)
internal class GcsBlobMetadata(
    @JacksonInject(useInput = OptBoolean.FALSE)
    val blobStore: GcsBlobStore,
    @JsonProperty("name", required = true)
    override val path: Path,
    @JsonProperty("size", required = true)
    override val size: Long,
    @JsonProperty("contentType", required = true)
    override val contentType: MediaType,
    @JsonProperty("etag", required = true)
    override val etag: String,
    @JsonProperty("updated", required = true)
    override val lastModified: Instant,
    @JsonProperty("mediaLink", required = true)
    val mediaLink: URI
) : BlobMetadata
