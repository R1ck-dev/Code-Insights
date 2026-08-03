package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * O analisador de Java: as tres metricas, por analise da AST. Faz o parse do codigo uma unica vez e
 * delega a cada {@link AnalisadorMetricaJava} registrado como bean.
 * <p>
 * Quando o codigo nao parseia, devolve lista vazia — e a resolucao fica <i>analisada e sem
 * metrica</i>, que a tela de qualidade le como FALHA, e nao como escopo. A distincao importa: aqui o
 * motor prometeu o numero e nao entregou.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JavaParserAnalisadorMetricas implements AnalisadorDeLinguagem {

    /** Nivel de linguagem alvo: os alunos submetem Java moderno (switch com seta, records, etc.). */
    private static final ParserConfiguration CONFIGURACAO = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

    private final List<AnalisadorMetricaJava> analisadores;

    @Override
    public LinguagemProgramacao linguagem() {
        return LinguagemProgramacao.JAVA;
    }

    /** Derivado dos beans registrados, e nao de uma lista a parte que envelheceria em silencio. */
    @Override
    public Set<TipoMetrica> metricasSuportadas() {
        return analisadores.stream().map(AnalisadorMetricaJava::tipo)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<ResultadoMetrica> analisar(Resolucao resolucao) {
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
                            calculada.valor(), calculada.rotulo(), calculada.detalhe(), calculada.confianca());
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
