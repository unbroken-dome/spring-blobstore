package org.unbrokendome.spring.blobstore.autoconfigure

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.context.ApplicationContext


interface BlobStoreConfigurationFactory<T : Any> {

    val type: String

    fun configurationBindable(): Bindable<T>

    fun beanDefinition(configuration: T, applicationContext: ApplicationContext): BeanDefinition
}
