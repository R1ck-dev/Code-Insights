package com.projeto.codeinsights.infrastructure.web.identity.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.identity.dto.AlterarVisibilidadePerfilInput;
import com.projeto.codeinsights.application.identity.dto.AtualizarMeuPerfilInput;
import com.projeto.codeinsights.application.identity.dto.MeuPerfilDTO;
import com.projeto.codeinsights.application.identity.dto.ReenviarAtivacaoInput;
import com.projeto.codeinsights.application.identity.dto.RegistrarUsuarioInput;
import com.projeto.codeinsights.application.identity.dto.UsuarioPublicoDTO;
import com.projeto.codeinsights.application.identity.usecase.AlterarVisibilidadePerfilUseCase;
import com.projeto.codeinsights.application.identity.usecase.AtivarContaUseCase;
import com.projeto.codeinsights.application.identity.usecase.AtualizarMeuPerfilUseCase;
import com.projeto.codeinsights.application.identity.usecase.BuscarMeuPerfilUseCase;
import com.projeto.codeinsights.application.identity.usecase.BuscarUsuarioPublicoUseCase;
import com.projeto.codeinsights.application.identity.usecase.ReenviarAtivacaoUseCase;
import com.projeto.codeinsights.application.identity.usecase.RegistrarUsuarioUseCase;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;
import com.projeto.codeinsights.infrastructure.web.identity.dto.AlterarVisibilidadePerfilRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.AtualizarMeuPerfilRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.ReenviarAtivacaoRequest;
import com.projeto.codeinsights.infrastructure.web.identity.dto.RegistrarUsuarioRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AtivarContaUseCase ativarContaUseCase;
    private final ReenviarAtivacaoUseCase reenviarAtivacaoUseCase;
    private final BuscarMeuPerfilUseCase buscarMeuPerfilUseCase;
    private final AtualizarMeuPerfilUseCase atualizarMeuPerfilUseCase;
    private final AlterarVisibilidadePerfilUseCase alterarVisibilidadePerfilUseCase;
    private final BuscarUsuarioPublicoUseCase buscarUsuarioPublicoUseCase;

    @PostMapping
    public ResponseEntity<Void> registrar(@RequestBody @Valid RegistrarUsuarioRequest request) {
        RegistrarUsuarioInput input = new RegistrarUsuarioInput(
                request.username(),
                request.email(),
                request.password());
        registrarUsuarioUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/ativar")
    public ResponseEntity<Map<String, String>> ativarConta(@RequestParam String token) {
        ativarContaUseCase.execute(token);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Conta ativada com sucesso! Voce ja pode realizar o login."));
    }

    @PostMapping("/reenviar-ativacao")
    public ResponseEntity<Map<String, String>> reenviarAtivacao(
            @RequestBody @Valid ReenviarAtivacaoRequest request) {
        reenviarAtivacaoUseCase.execute(new ReenviarAtivacaoInput(request.email()));
        return ResponseEntity.ok(Map.of(
                "mensagem", "Se a conta existir e estiver pendente, um novo e-mail de ativacao foi enviado."));
    }

    @GetMapping("/me")
    public ResponseEntity<MeuPerfilDTO> meuPerfil(@CurrentUserId UUID usuarioId) {
        return ResponseEntity.ok(buscarMeuPerfilUseCase.execute(usuarioId));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> atualizarMeuPerfil(@CurrentUserId UUID usuarioId,
            @RequestBody @Valid AtualizarMeuPerfilRequest request) {
        atualizarMeuPerfilUseCase.execute(new AtualizarMeuPerfilInput(usuarioId, request.username()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/visibilidade")
    public ResponseEntity<Void> alterarVisibilidade(@CurrentUserId UUID usuarioId,
            @RequestBody @Valid AlterarVisibilidadePerfilRequest request) {
        alterarVisibilidadePerfilUseCase.execute(
                new AlterarVisibilidadePerfilInput(usuarioId, request.publico()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<UsuarioPublicoDTO> buscarPublico(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(buscarUsuarioPublicoUseCase.execute(usuarioId));
    }
}
