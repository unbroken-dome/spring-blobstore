package org.unbrokendome.spring.blobstore.filesystem

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.getBeanProvider
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.context.ApplicationContext
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.unbrokendome.spring.blobstore.autoconfigure.BlobStoreConfigurationFactory
import java.nio.file.Paths
import java.time.Clock


class FileSystemBlobStoreConfigurationFactory : BlobStoreConfigurationFactory<FileSystemBlobStoreProperties> {

    override val type: String
        get() = "filesystem"


    override fun configurationBindable(): Bindable<FileSystemBlobStoreProperties> =
            Bindable.of(FileSystemBlobStoreProperties::class.java)


    override fun beanDefinition(
        configuration: FileSystemBlobStoreProperties,
        applicationContext: ApplicationContext
    ): BeanDefinition =
        BeanDefinitionBuilder.genericBeanDefinition(FileSystemBlobStore::class.java) {

            val dataBufferFactoryProvider = applicationContext.getBeanProvider<DataBufferFactory>()
            val clockProvider = applicationContext.getBeanProvider<Clock>()

            DefaultFileSystemBlobStore(
                basePath = Paths.get(configuration.basePath),
                dataBufferFactory = dataBufferFactoryProvider.getIfAvailable { DefaultDataBufferFactory() },
                clock = clockProvider.getIfAvailable { Clock.systemUTC() },
                bufferSize = configuration.bufferSize,
                digestAlgorithm = configuration.digestAlgorithm
            )
        }.beanDefinition
}
