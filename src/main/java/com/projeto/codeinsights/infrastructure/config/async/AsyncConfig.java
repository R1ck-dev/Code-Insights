package com.projeto.codeinsights.infrastructure.config.async;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita a execucao assincrona e define o pool dedicado a analise de metricas,
 * isolando-a das threads de request. A analise roda em background apos o commit
 * da submissao; uma falha aqui nunca derruba a submissao do usuario.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "analiseExecutor")
    public Executor analiseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("analise-metrica-");
        executor.initialize();
        return executor;
    }
}
