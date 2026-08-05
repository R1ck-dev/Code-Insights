package com.projeto.codeinsights.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.identity.dto.ExcluirMinhaContaInput;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.PasswordEncoderPort;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import org.assertj.core.api.Assertions;

/**
 * A exclusao apaga <b>de verdade</b>: nao ha anonimizacao, nao ha copia e nao ha desfazer.
 * <p>
 * Por isso os testes daqui guardam menos "o caminho feliz funciona" e mais <b>as duas travas</b>:
 * senha errada nao apaga, e conta administradora nao se apaga sozinha. Uma falha em qualquer uma
 * delas custa dado de participante que ninguem recupera.
 */
@ExtendWith(MockitoExtension.class)
class ExcluirMinhaContaUseCaseTest {

    private static final String SENHA = "Senha@2026";
    private static final String HASH = "hash-da-senha";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private ExcluirMinhaContaUseCase useCase;

    private Usuario aluno() {
        return new Usuario(UUID.randomUUID(), "aluno", "aluno@exemplo.com", HASH);
    }

    private Usuario admin() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "admin", "admin@exemplo.com", HASH);
        usuario.promoverParaAdmin();
        return usuario;
    }

    @Test
    @DisplayName("com a senha certa, a conta e removida")
    void removeAContaQuandoASenhaConfere() {
        Usuario usuario = aluno();
        when(usuarioRepository.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoderPort.matches(SENHA, HASH)).thenReturn(true);

        useCase.execute(new ExcluirMinhaContaInput(usuario.getId(), SENHA));

        verify(usuarioRepository).remover(usuario.getId());
    }

    /**
     * O JWT prova quem e, mas nao prova quem esta diante do teclado. Sem esta trava, um navegador
     * deixado aberto num laboratorio bastaria para apagar meses de trabalho de outra pessoa.
     */
    @Test
    @DisplayName("senha errada nao apaga nada")
    void naoRemoveComSenhaErrada() {
        Usuario usuario = aluno();
        when(usuarioRepository.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoderPort.matches("errada", HASH)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ExcluirMinhaContaInput(usuario.getId(), "errada")))
                .isInstanceOf(NegocioException.class);

        verify(usuarioRepository, never()).remover(any());
    }

    /**
     * A cascata levaria a conta administradora junto com tudo, e o AdminSeeder so a recria quando
     * {@code ADMIN_PASSWORD} esta no ambiente — em producao pode nao estar. A recuperacao seria
     * INSERT manual no Postgres.
     */
    @Test
    @DisplayName("conta administradora nao se exclui por aqui")
    void naoRemoveAContaAdministradora() {
        Usuario usuario = admin();
        when(usuarioRepository.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoderPort.matches(SENHA, HASH)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ExcluirMinhaContaInput(usuario.getId(), SENHA)))
                .isInstanceOf(NegocioException.class);

        verify(usuarioRepository, never()).remover(any());
    }

    @Test
    @DisplayName("conta inexistente nao explode em NullPointer")
    void falhaQuandoAContaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new ExcluirMinhaContaInput(id, SENHA)))
                .isInstanceOf(NegocioException.class);

        verify(usuarioRepository, never()).remover(any());
    }

    /**
     * O caso de uso apaga UMA linha e confia no {@code ON DELETE CASCADE} para o resto. Se alguem
     * criar uma tabela nova apontando para {@code usuarios} sem cascata, a exclusao passaria a
     * falhar por violacao de chave estrangeira — em producao, na cara do participante.
     * <p>
     * Este teste le as migrations e exige que <b>toda</b> referencia a {@code usuarios} declare a
     * cascata. E o unico lugar do projeto que guarda essa expectativa.
     */
    @Test
    @DisplayName("toda referencia a usuarios cascateia na exclusao")
    void todaChaveEstrangeiraParaUsuariosTemCascata() throws Exception {
        Path migrations = Path.of("src", "main", "resources", "db", "migration");

        try (var arquivos = Files.list(migrations)) {
            arquivos.filter(caminho -> caminho.toString().endsWith(".sql")).forEach(caminho -> {
                String sql = ler(caminho);
                sql.lines()
                        .filter(linha -> linha.toLowerCase().contains("references usuarios"))
                        .forEach(linha -> Assertions.assertThat(linha.toUpperCase())
                                .as("%s declara uma FK para usuarios sem ON DELETE CASCADE: a exclusao "
                                        + "de conta passaria a falhar por violacao de chave estrangeira.%n  %s",
                                        caminho.getFileName(), linha.trim())
                                .contains("ON DELETE CASCADE"));
            });
        }
    }

    private static String ler(Path caminho) {
        try {
            return Files.readString(caminho);
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel ler " + caminho, e);
        }
    }
}
