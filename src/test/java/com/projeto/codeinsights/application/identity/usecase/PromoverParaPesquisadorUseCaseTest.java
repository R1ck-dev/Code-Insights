package com.projeto.codeinsights.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.identity.dto.PapelAlteradoDTO;
import com.projeto.codeinsights.domain.identity.enums.Role;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Unica porta de entrada para a area de pesquisa. O papel e um campo so — nao ha soma de papeis —,
 * entao esta rota <b>troca</b> o papel de quem ela toca.
 */
@ExtendWith(MockitoExtension.class)
class PromoverParaPesquisadorUseCaseTest {

    private static final String EMAIL = "pesquisador@exemplo.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PromoverParaPesquisadorUseCase useCase;

    private Usuario aluno() {
        return new Usuario(UUID.randomUUID(), "aluno", EMAIL, "hash");
    }

    private Usuario admin() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "admin", EMAIL, "hash");
        usuario.promoverParaAdmin();
        return usuario;
    }

    @Test
    void promoveAlunoEDevolveOPapelNovo() {
        Usuario usuario = aluno();
        assertThat(usuario.getRole()).isEqualTo(Role.ALUNO);
        when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.salvar(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PapelAlteradoDTO dto = useCase.execute(EMAIL);

        assertThat(dto.role()).isEqualTo(Role.PESQUISADOR);
        assertThat(dto.email()).isEqualTo(EMAIL);
        verify(usuarioRepository).salvar(usuario);
    }

    /**
     * O caso que motivou a guarda: ADMIN ja alcanca {@code /api/pesquisa/**}, e PESQUISADOR nao
     * alcanca {@code /api/admin/**}. Sem a recusa, aplicar a rota a conta administradora a
     * destruiria em silencio — nao ha rota de volta pela API, e o semeador so recria o admin quando
     * o e-mail ainda nao esta no banco. A recuperacao seria UPDATE manual no Postgres.
     */
    @Test
    void recusaRebaixarUmAdminEDeixaOPapelIntacto() {
        Usuario usuario = admin();
        when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> useCase.execute(EMAIL))
                .isInstanceOf(NegocioException.class);

        assertThat(usuario.getRole()).isEqualTo(Role.ADMIN);
        verify(usuarioRepository, never()).salvar(any());
    }

    @Test
    void falhaQuandoEmailNaoExiste() {
        when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(EMAIL))
                .isInstanceOf(NegocioException.class);

        verify(usuarioRepository, never()).salvar(any());
    }
}
