package com.projeto.codeinsights.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.identity.dto.ContaAtivadaDTO;
import com.projeto.codeinsights.application.identity.dto.TokenRedefinicaoDTO;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Caminho manual usado quando a entrega de e-mail falha durante a coleta de dados.
 */
@ExtendWith(MockitoExtension.class)
class FluxoAdministrativoDeContaTest {

    private static final String EMAIL = "aluno@exemplo.com";

    private Usuario usuarioPendente() {
        return new Usuario(UUID.randomUUID(), "aluno", EMAIL, "hash");
    }

    @Nested
    class AtivacaoPeloAdmin {

        @Mock
        private UsuarioRepository usuarioRepository;

        @InjectMocks
        private AtivarContaPeloAdminUseCase useCase;

        @Test
        void ativaContaPendenteEDevolveNovoStatus() {
            Usuario usuario = usuarioPendente();
            assertThat(usuario.getStatus()).isEqualTo(StatusConta.PENDENTE_VERIFICACAO);
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.salvar(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            ContaAtivadaDTO dto = useCase.execute(EMAIL);

            assertThat(dto.status()).isEqualTo(StatusConta.ATIVO);
            assertThat(dto.email()).isEqualTo(EMAIL);
            verify(usuarioRepository).salvar(usuario);
        }

        @Test
        void falhaQuandoEmailNaoExiste() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(EMAIL))
                    .isInstanceOf(NegocioException.class);

            verify(usuarioRepository, never()).salvar(any());
        }
    }

    @Nested
    class GeracaoDeTokenDeRedefinicao {

        @Mock
        private UsuarioRepository usuarioRepository;
        @Mock
        private TokenVerificacaoRepository tokenVerificacaoRepository;

        @InjectMocks
        private GerarTokenRedefinicaoSenhaUseCase useCase;

        @Test
        void persisteTokenDeRedefinicaoEDevolveOMesmoValorGerado() {
            Usuario usuario = usuarioPendente();
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuario));

            TokenRedefinicaoDTO dto = useCase.execute(EMAIL);

            ArgumentCaptor<TokenVerificacao> captor = ArgumentCaptor.forClass(TokenVerificacao.class);
            verify(tokenVerificacaoRepository).salvar(captor.capture());
            TokenVerificacao persistido = captor.getValue();

            // O token entregue ao admin tem de ser o mesmo gravado, senao o link nao abre.
            assertThat(dto.token()).isEqualTo(persistido.getToken());
            assertThat(persistido.getTipo()).isEqualTo(TipoToken.REDEFINICAO_SENHA);
            assertThat(persistido.isUtilizado()).isFalse();
        }

        @Test
        void tokenExpiraEmVinteEQuatroHorasComoOFluxoPorEmail() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.of(usuarioPendente()));

            TokenRedefinicaoDTO dto = useCase.execute(EMAIL);

            assertThat(dto.expiraEm())
                    .isAfter(LocalDateTime.now().plusHours(23))
                    .isBefore(LocalDateTime.now().plusHours(25));
        }

        /**
         * O fluxo publico responde neutro para nao revelar quem tem conta; aqui quem chama ja e
         * um admin autenticado, entao esconder o erro so mascararia um e-mail digitado errado.
         */
        @Test
        void falhaQuandoEmailNaoExisteEmVezDeResponderNeutro() {
            when(usuarioRepository.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(EMAIL))
                    .isInstanceOf(NegocioException.class);

            verify(tokenVerificacaoRepository, never()).salvar(any());
        }
    }
}
