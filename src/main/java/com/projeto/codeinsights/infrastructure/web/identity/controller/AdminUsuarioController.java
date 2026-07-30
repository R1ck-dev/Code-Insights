package com.projeto.codeinsights.infrastructure.web.identity.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.identity.dto.ContaAtivadaDTO;
import com.projeto.codeinsights.application.identity.dto.TokenRedefinicaoDTO;
import com.projeto.codeinsights.application.identity.usecase.AtivarContaPeloAdminUseCase;
import com.projeto.codeinsights.application.identity.usecase.GerarTokenRedefinicaoSenhaUseCase;
import com.projeto.codeinsights.infrastructure.web.identity.dto.LinkRedefinicaoResponse;
import com.projeto.codeinsights.infrastructure.web.identity.dto.UsuarioPorEmailRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Area administrativa de contas: o caminho manual para quando a entrega de e-mail falha.
 * <p>
 * O e-mail e a peca menos confiavel do sistema — cai em spam, esbarra no limite diario do
 * remetente, e o aluno as vezes simplesmente nao acha a mensagem. Durante a coleta de dados
 * isso significaria um participante travado na tela de login sem saida. Estas duas rotas sao
 * essa saida, operadas por quem conduz a pesquisa.
 * <p>
 * Vivem sob {@code /api/admin/**}, que o {@code SecurityConfig} restringe a
 * {@code hasRole("ADMIN")}. Essa restricao <b>e</b> o modelo de seguranca aqui: sem ela, gerar
 * link de redefinicao a partir de um e-mail seria tomada de conta por qualquer visitante.
 */
@RestController
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final AtivarContaPeloAdminUseCase ativarContaPeloAdminUseCase;
    private final GerarTokenRedefinicaoSenhaUseCase gerarTokenRedefinicaoSenhaUseCase;

    @Value("${app.web.base-url}")
    private String webBaseUrl;

    /** Ativa a conta sem o e-mail de confirmacao, para o aluno que nunca recebeu a mensagem. */
    @PostMapping("/api/admin/usuarios/ativar")
    public ResponseEntity<ContaAtivadaDTO> ativarConta(@Valid @RequestBody UsuarioPorEmailRequest request) {
        return ResponseEntity.ok(ativarContaPeloAdminUseCase.execute(request.email()));
    }

    /**
     * Devolve um link de redefinicao de senha valido por 24 horas, para o administrador
     * entregar ao aluno por outro canal. A URL e montada aqui, e nao no caso de uso, porque
     * o endereco publico do front e configuracao de infraestrutura — a mesma
     * {@code app.web.base-url} que o adapter de e-mail usa, para os dois canais nunca
     * divergirem.
     */
    @PostMapping("/api/admin/usuarios/link-redefinicao-senha")
    public ResponseEntity<LinkRedefinicaoResponse> gerarLinkRedefinicao(
            @Valid @RequestBody UsuarioPorEmailRequest request) {
        TokenRedefinicaoDTO dto = gerarTokenRedefinicaoSenhaUseCase.execute(request.email());

        String link = webBaseUrl + "/definir-senha?token=" + dto.token();

        return ResponseEntity.ok(
                new LinkRedefinicaoResponse(dto.username(), dto.email(), link, dto.expiraEm()));
    }
}
