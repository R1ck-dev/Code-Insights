package com.projeto.codeinsights.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.identity.dto.UsuarioAdminDTO;
import com.projeto.codeinsights.domain.identity.enums.Role;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

@ExtendWith(MockitoExtension.class)
class ListarUsuariosParaAdminUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ListarUsuariosParaAdminUseCase useCase;

    private Usuario usuario(String username, Role role, StatusConta status, Visibilidade visibilidade) {
        return new Usuario(UUID.randomUUID(), username, username + "@exemplo.com", "hash", role,
                visibilidade, status, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private void encontrados(Usuario... usuarios) {
        when(usuarioRepository.listarTodos(anyString(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(usuarios), 0, 1, usuarios.length));
    }

    /**
     * A tela decide quais acoes oferecer a partir do papel e do status — promover so cabe a quem
     * ainda nao e pesquisador, ativar so a quem esta pendente. Perder um dos dois campos no DTO nao
     * quebra nada visivelmente: some o botao, e o administrador conclui que a acao nao existe.
     */
    @Test
    void oDtoLevaPapelStatusEEmail() {
        encontrados(usuario("ana", Role.PESQUISADOR, StatusConta.ATIVO, Visibilidade.PRIVADO));

        UsuarioAdminDTO dto = useCase.execute("", 0, 20).itens().get(0);

        assertThat(dto.username()).isEqualTo("ana");
        assertThat(dto.email()).isEqualTo("ana@exemplo.com");
        assertThat(dto.role()).isEqualTo(Role.PESQUISADOR);
        assertThat(dto.status()).isEqualTo(StatusConta.ATIVO);
    }

    /**
     * Prende a porta usada. {@code listarPublicos} filtra por perfil PUBLICO e status ATIVO — trocar
     * uma pela outra "para reaproveitar" esconderia exatamente as contas pendentes de verificacao e
     * de perfil privado, que sao as que precisam de acao administrativa. A tela ficaria vazia
     * justamente para o caso que ela existe para resolver.
     */
    @Test
    void consultaTodasAsContasENaoApenasOsPerfisPublicos() {
        encontrados(usuario("bruno", Role.ALUNO, StatusConta.PENDENTE_VERIFICACAO, Visibilidade.PRIVADO));

        assertThat(useCase.execute("bru", 0, 20).itens()).hasSize(1);

        verify(usuarioRepository).listarTodos("bru", 0, 20);
        verify(usuarioRepository, never()).listarPublicos(any(), anyString(), anyInt(), anyInt());
    }

    @Test
    void preservaOsDadosDePaginacaoDaConsulta() {
        when(usuarioRepository.listarTodos(anyString(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 2, 7, 130));

        assertThat(useCase.execute("", 2, 20)).satisfies(pagina -> {
            assertThat(pagina.paginaAtual()).isEqualTo(2);
            assertThat(pagina.totalPaginas()).isEqualTo(7);
            assertThat(pagina.totalItens()).isEqualTo(130);
        });
    }
}
