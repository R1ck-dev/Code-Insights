package com.projeto.codeinsights.infrastructure.web.pesquisa.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.pesquisa.dto.ArquivoExportadoDTO;
import com.projeto.codeinsights.application.pesquisa.dto.ParticipanteIdentificadoDTO;
import com.projeto.codeinsights.application.pesquisa.dto.QualidadeDaCoorteDTO;
import com.projeto.codeinsights.application.pesquisa.dto.ResolucaoDaCoorteDTO;
import com.projeto.codeinsights.application.pesquisa.usecase.ExportarCoorteCsvUseCase;
import com.projeto.codeinsights.application.pesquisa.usecase.IdentificarParticipanteUseCase;
import com.projeto.codeinsights.application.pesquisa.usecase.ListarCoorteUseCase;
import com.projeto.codeinsights.application.pesquisa.usecase.ObterQualidadeDaCoorteUseCase;
import com.projeto.codeinsights.infrastructure.web.pesquisa.dto.PseudonimoRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Area de pesquisa: os dados de toda a plataforma, pseudonimizados.
 * <p>
 * Nao ha {@code @CurrentUserId} em nenhum metodo, de proposito — a operacao nao e sobre o portfolio
 * de quem chama, e sim sobre a coorte inteira. Quem pode chamar e decidido no {@code SecurityConfig},
 * que restringe {@code /api/pesquisa/**} a {@code PESQUISADOR} e {@code ADMIN}. <b>Essa restricao e
 * o modelo de seguranca aqui</b>: sem ela, qualquer aluno autenticado leria o corpus dos colegas.
 * <p>
 * Nenhum destes endpoints e paginado. Tanto o relatorio de qualidade quanto o export precisam da
 * amostra inteira para significar alguma coisa — cobertura calculada sobre uma pagina nao e
 * cobertura.
 */
@RestController
@RequiredArgsConstructor
public class PesquisaController {

    private final ObterQualidadeDaCoorteUseCase obterQualidadeDaCoorteUseCase;
    private final ListarCoorteUseCase listarCoorteUseCase;
    private final ExportarCoorteCsvUseCase exportarCoorteCsvUseCase;
    private final IdentificarParticipanteUseCase identificarParticipanteUseCase;

    /** Cobertura da analise, distribuicoes e buracos da amostra — a leitura que serve durante a coleta. */
    @GetMapping("/api/pesquisa/qualidade")
    public ResponseEntity<QualidadeDaCoorteDTO> qualidade() {
        return ResponseEntity.ok(obterQualidadeDaCoorteUseCase.execute());
    }

    /** Todas as resolucoes da plataforma como linhas comparaveis, com pseudonimo no lugar do autor. */
    @GetMapping("/api/pesquisa/coorte")
    public ResponseEntity<List<ResolucaoDaCoorteDTO>> coorte() {
        return ResponseEntity.ok(listarCoorteUseCase.execute());
    }

    /**
     * O mesmo dado da listagem, em CSV. O {@code Content-Disposition} carrega o nome com a data para
     * que dois exports nao se sobrescrevam na pasta de downloads — versionar a amostra por data e o
     * minimo para conseguir refazer uma analise depois.
     */
    @GetMapping(value = "/api/pesquisa/coorte.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv() {
        ArquivoExportadoDTO arquivo = exportarCoorteCsvUseCase.execute();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"".formatted(arquivo.nomeDoArquivo()))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(arquivo.conteudo().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Revela quem esta por tras de um pseudonimo. E {@code POST} — e nao {@code GET} com o
     * pseudonimo na URL — porque identificar participante nao deve ficar no historico do navegador
     * nem no log de acesso do servidor.
     */
    @PostMapping("/api/pesquisa/participantes/identificar")
    public ResponseEntity<ParticipanteIdentificadoDTO> identificar(@Valid @RequestBody PseudonimoRequest request) {
        return ResponseEntity.ok(identificarParticipanteUseCase.execute(request.pseudonimo()));
    }
}
