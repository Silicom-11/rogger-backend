package com.utp.roggerexpress.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas periodicas del sistema (reintento de avisos al cliente).
 */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
