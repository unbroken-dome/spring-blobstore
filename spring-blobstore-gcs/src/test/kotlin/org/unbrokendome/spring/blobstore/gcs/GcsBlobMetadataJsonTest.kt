package org.unbrokendome.spring.blobstore.gcs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

class GcsBlobMetadataJsonTest {

    val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())

}
