package com.projeto.codeinsights.application.identity.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.enums.Role;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;

/**
 * Uma conta como a administracao precisa ve-la: papel e status visiveis, porque sao eles que dizem
 * qual acao faz sentido — promover so cabe a quem nao e pesquisador, ativar so a quem esta pendente.
 * <p>
 * Leva <b>e-mail</b>, ao contrario do {@code UsuarioPublicoDTO}: e o identificador que as rotas
 * administrativas aceitam, e sem ele a tela obrigaria o admin a saber o endereco de cor — que e
 * exatamente o atrito que ela existe para remover. Esta rota ja esta atras de {@code hasRole(ADMIN)}.
 * <p>
 * Nao leva {@code senhaHash}, obviamente, nem a visibilidade do perfil: nenhuma acao desta tela
 * depende dela, e campo que ninguem usa e campo que so aumenta a superficie do que vaza.
 */
public record UsuarioAdminDTO(
        UUID id,
        String username,
        String email,
        Role role,
        StatusConta status,
        OffsetDateTime criadoEm) {
}
