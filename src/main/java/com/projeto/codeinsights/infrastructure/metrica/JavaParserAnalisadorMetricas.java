package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Motor de analise estatica que implementa a porta de dominio {@link AnalisadorMetricas}.
 * Faz o parse do codigo uma unica vez e delega a cada {@link AnalisadorMetricaJava}
 * registrado como bean (a lista cresce por adicao). So a linguagem Java e suportada
 * por ora; para outras linguagens, ou quando o codigo nao parseia, retorna lista
 * vazia (a resolucao e marcada como analisada, sem metricas).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JavaParserAnalisadorMetricas implements AnalisadorMetricas {

    /** Nivel de linguagem alvo: os alunos submetem Java moderno (switch com seta, records, etc.). */
    private static final ParserConfiguration CONFIGURACAO = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

    private final List<AnalisadorMetricaJava> analisadores;

    @Override
    public List<ResultadoMetrica> analisar(Resolucao resolucao) {
        if (resolucao.getLinguagem() != LinguagemProgramacao.JAVA) {
            log.info("Linguagem {} ainda nao suportada para analise de metricas (resolucao {}).",
                    resolucao.getLinguagem(), resolucao.getId());
            return List.of();
        }

        Optional<CompilationUnit> unidade = parsear(resolucao.getCodigoFonte());
        if (unidade.isEmpty()) {
            log.warn("Nao foi possivel parsear o codigo Java da resolucao {}; nenhuma metrica gerada.",
                    resolucao.getId());
            return List.of();
        }

        return analisadores.stream()
                .map(analisador -> {
                    MetricaCalculada calculada = analisador.analisar(unidade.get());
                    return new ResultadoMetrica(null, resolucao.getId(), analisador.tipo(),
                            calculada.valor(), calculada.rotulo(), calculada.detalhe());
                })
                .toList();
    }

    /** Tenta parsear como compilation unit; se falhar, tenta envolver em uma classe (codigo colado sem classe). */
    private Optional<CompilationUnit> parsear(String codigo) {
        JavaParser parser = new JavaParser(CONFIGURACAO);
        ParseResult<CompilationUnit> direto = parser.parse(codigo);
        if (direto.isSuccessful() && direto.getResult().isPresent()) {
            return direto.getResult();
        }
        ParseResult<CompilationUnit> envolvido = parser.parse("class CodeInsightsWrapper {\n" + codigo + "\n}");
        return envolvido.isSuccessful() ? envolvido.getResult() : Optional.empty();
    }
}
