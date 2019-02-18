package org.unbrokendome.spring.blobstore.autoconfigure

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.AutowireCandidateQualifier
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.ResolvableType
import org.unbrokendome.spring.blobstore.BlobStore
import java.util.ServiceLoader


internal class BlobStoreConfigurationPostProcessor : BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    private val factories = ServiceLoader.load(BlobStoreConfigurationFactory::class.java)
        .associateBy { it.type }


    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {

        val binder = Binder.get(applicationContext.environment)

        binder.bind("blobstore", blobStoresMapBindable())
            .orElse(emptyMap())
            .forEach { blobStoreName, blobStoreSpec ->

                require(blobStoreSpec.size == 1) {
                    "Blob store configuration must have a single key that indicates the type of the blob store, " +
                            "but found ${blobStoreSpec.size}: ${blobStoreSpec.keys}"
                }
                val blobStoreType = blobStoreSpec.keys.single()

                @Suppress("UNCHECKED_CAST") val factory =
                    requireNotNull(factories[blobStoreType]) {
                        "Unknown blob store type: \"$blobStoreType\" for blob store \"$blobStoreName\""
                    } as BlobStoreConfigurationFactory<Any>

                val blobStoreConfig: Any = binder.bind(
                    "blobstore.$blobStoreName.$blobStoreType",
                    factory.configurationBindable()
                ).orElseThrow {
                    IllegalArgumentException("Could not bind blob store configuration of type $blobStoreType")
                }

                val beanDefinition = factory.beanDefinition(blobStoreConfig, applicationContext)
                if (beanDefinition is AbstractBeanDefinition) {
                    beanDefinition.addQualifier(AutowireCandidateQualifier(Qualifier::class.java, blobStoreName))
                }

                registry.registerBeanDefinition(
                    BlobStore::class.java.name + ".$blobStoreName",
                    beanDefinition
                )
            }
    }


    private fun blobStoresMapBindable(): Bindable<Map<String, Map<String, Any>>> =
        Bindable.of<Map<String, Map<String, Any>>>(
            ResolvableType.forClassWithGenerics(
                Map::class.java,
                ResolvableType.forClass(String::class.java),
                ResolvableType.forClassWithGenerics(Map::class.java, String::class.java, Any::class.java)
            )
        )


    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    }
}
