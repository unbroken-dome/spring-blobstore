package org.unbrokendome.spring.blobstore.gcs.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.unbrokendome.spring.blobstore.autoconfigure.BlobStoreDefinition


@Suppress("ConfigurationProperties")
@ConfigurationProperties
class GcsBlobStoreDefinition : BlobStoreDefinition() {

    @NestedConfigurationProperty
    var gcs: GcsBlobStoreProperties? = null
}
