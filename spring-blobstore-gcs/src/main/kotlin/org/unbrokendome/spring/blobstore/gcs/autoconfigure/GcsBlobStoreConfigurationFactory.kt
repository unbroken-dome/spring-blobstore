package org.unbrokendome.spring.blobstore.gcs.autoconfigure

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.getBeanProvider
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import org.unbrokendome.spring.blobstore.autoconfigure.BlobStoreConfigurationFactory
import org.unbrokendome.spring.blobstore.gcs.GcsBlobStore
import java.time.Clock


class GcsBlobStoreConfigurationFactory : BlobStoreConfigurationFactory<GcsBlobStoreProperties> {

    override val type: String
        get() = "gcs"


    override fun configurationBindable(): Bindable<GcsBlobStoreProperties> =
        Bindable.of(GcsBlobStoreProperties::class.java)


    override fun beanDefinition(
        configuration: GcsBlobStoreProperties,
        applicationContext: ApplicationContext
    ): BeanDefinition =
        BeanDefinitionBuilder.genericBeanDefinition(GcsBlobStore::class.java) {

            val webClientBuilderProvider = applicationContext.getBeanProvider<WebClient.Builder>()
            val clockProvider = applicationContext.getBeanProvider<Clock>()

            GcsBlobStore.builder().run {
                configuration.configureBuilder(this)
                webClientBuilderProvider.ifAvailable { withWebClientBuilder(it) }
                clockProvider.ifAvailable { withClock(it) }
                build()
            }
        }.beanDefinition
}
