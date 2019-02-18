package org.unbrokendome.spring.blobstore.gcs.autoconfigure

import org.springframework.core.io.Resource
import org.unbrokendome.spring.blobstore.gcs.GcsBlobStoreBuilder
import kotlin.reflect.KProperty0
import org.unbrokendome.spring.blobstore.gcs.auth.Credentials as GcsCredentials


class GcsBlobStoreProperties {

    var bucketName: String? = null

    var credentials: Credentials? = null


    class Credentials {
        var json: String? = null
        var jsonResource: Resource? = null

        internal fun buildCredentials(): GcsCredentials {
            json?.let {
                return GcsCredentials.fromJson(it)
            }
            jsonResource?.let {
                return GcsCredentials.fromJson(it)
            }
            throw IllegalStateException("Either \"json\" or \"jsonResource\" is required")
        }
    }


    internal fun configureBuilder(builder: GcsBlobStoreBuilder) {

        builder.withBucketName(checkRequiredProperty(this::bucketName))

        credentials?.let { credentials ->
            builder.withCredentials(credentials.buildCredentials())
        }
    }
}


private fun <R : Any> checkRequiredProperty(prop: KProperty0<R?>): R =
    checkNotNull(prop.get()) { "Property \"${prop.name}\" is required" }
