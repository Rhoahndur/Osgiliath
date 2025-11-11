package com.osgiliath.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async configuration
 * Enables asynchronous method execution for email sending
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
