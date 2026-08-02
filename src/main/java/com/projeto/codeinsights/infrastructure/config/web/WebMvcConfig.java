package com.projeto.codeinsights.infrastructure.config.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.projeto.codeinsights.infrastructure.config.security.CurrentUserIdArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }

    /**
     * {@code exposedHeaders} e uma lista fechada: o navegador esconde do JavaScript todo cabecalho
     * de resposta que nao esteja aqui ou na safelist do CORS. {@code Content-Disposition} nao esta
     * na safelist, e o export da coorte le dele o nome do arquivo (o download vai por blob URL, que
     * ignora o cabecalho real). Sem esta linha o defeito e invisivel em desenvolvimento — o proxy do
     * Vite torna a chamada same-origin — e aparece so em producao, onde front e back estao em
     * origens diferentes: todo CSV baixaria com o nome padrao, sem a data que versiona a amostra.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
