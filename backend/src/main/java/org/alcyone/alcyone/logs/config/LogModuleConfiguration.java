package org.alcyone.alcyone.logs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Active la liaison de {@link LogProperties} sur {@code alcyone.logs.*}.
 */
@Configuration
@EnableConfigurationProperties(LogProperties.class)
public class LogModuleConfiguration {
}
