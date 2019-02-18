package org.unbrokendome.spring.blobstore.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BlobStoreAutoConfiguration {

    @Bean
    public static BlobStoreConfigurationPostProcessor blobStoreConfigurationPostProcessor() {
        return new BlobStoreConfigurationPostProcessor();
    }
}
