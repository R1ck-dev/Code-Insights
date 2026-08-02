package com.projeto.codeinsights.application.pesquisa.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.pesquisa.dto.ArquivoExportadoDTO;
import com.projeto.codeinsights.application.pesquisa.dto.ResolucaoDaCoorteDTO;

import lombok.RequiredArgsConstructor;

/**
 * O CSV que alimenta a analise fora da plataforma (R, pandas, SPSS).
 * <p>
 * Reusa {@link ListarCoorteUseCase} em vez de consultar de novo — <b>o arquivo e exatamente o que a
 * tela mostra</b>. Duas leituras independentes poderiam divergir (uma filtra, a outra nao; uma
 * pseudonimiza, a outra esquece) e a divergencia so apareceria conferindo linha a linha.
 * <p>
 * O <b>codigo-fonte nao sai aqui</b>, por decisao de projeto: comentario e nome de variavel
 * identificam o autor ({@code // resolvido pelo Joao}), e o CSV e o artefato que circula por
 * e-mail e pasta compartilhada. O codigo continua visivel na plataforma, onde o acesso e controlado.
 */
@Service
@RequiredArgsConstructor
public class ExportarCoorteCsvUseCase {

    /** Nomes em snake_case: e o que vira nome de coluna em `df$tempo_ordem` sem precisar renomear. */
    private static final List<String> CABECALHO = List.of(
            "pseudonimo", "resolucao_id", "desafio_id", "desafio", "linguagem", "autonomia_ia",
            "analisada", "tempo_rotulo", "tempo_ordem", "confianca_tempo",
            "espaco_rotulo", "espaco_ordem", "confianca_espaco", "ciclomatica", "submetida_em");

    private final ListarCoorteUseCase listarCoorteUseCase;

    @Transactional(readOnly = true)
    public ArquivoExportadoDTO execute() {
        List<List<String>> linhas = listarCoorteUseCase.execute().stream()
                .map(ExportarCoorteCsvUseCase::paraLinha)
                .toList();

        return new ArquivoExportadoDTO(
                "codeinsights-coorte-%s.csv".formatted(LocalDate.now()),
                EscritorDeCsv.escrever(CABECALHO, linhas));
    }

    /**
     * Campo nulo vira celula vazia, e nao {@code 0} nem {@code "null"}. Preservar o vazio e o que
     * permite a analise distinguir "sem metrica" de {@code O(1)} (ordem 0) e de "?" (ordem -1).
     */
    private static List<String> paraLinha(ResolucaoDaCoorteDTO resolucao) {
        List<String> campos = new ArrayList<>(CABECALHO.size());
        campos.add(resolucao.pseudonimo());
        campos.add(texto(resolucao.resolucaoId()));
        campos.add(texto(resolucao.desafioId()));
        campos.add(resolucao.desafioTitulo());
        campos.add(texto(resolucao.linguagem()));
        campos.add(String.valueOf(resolucao.indiceAutonomiaIA()));
        campos.add(String.valueOf(resolucao.analisada()));
        campos.add(resolucao.tempoRotulo());
        campos.add(texto(resolucao.tempoOrdem()));
        campos.add(texto(resolucao.confiancaTempo()));
        campos.add(resolucao.espacoRotulo());
        campos.add(texto(resolucao.espacoOrdem()));
        campos.add(texto(resolucao.confiancaEspaco()));
        campos.add(texto(resolucao.ciclomatica()));
        campos.add(texto(resolucao.submetidaEm()));
        return campos;
    }

    private static String texto(Object valor) {
        return valor == null ? null : Objects.toString(valor);
    }
}
