package org.unbrokendome.spring.blobstore.gcs

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext


@SpringBootTest
class GcsBlobStoreAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    class TestConfig


    @Test
    fun test(applicationContext: ApplicationContext) {

        val blobStore = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
            applicationContext.autowireCapableBeanFactory, GcsBlobStore::class.java,
            "example"
        )

        assertThat(blobStore).all {
            isNotNull()
            prop(GcsBlobStore::bucketName).isEqualTo("exampleBucket")
        }
    }
}
