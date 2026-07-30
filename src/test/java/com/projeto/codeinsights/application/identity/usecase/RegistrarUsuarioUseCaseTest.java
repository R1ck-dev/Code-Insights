package com.projeto.codeinsights.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.codeinsights.application.identity.dto.RegistrarUsuarioInput;
import com.projeto.codeinsights.application.identity.dto.RegistroDTO;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.EmailSenderPort;
import com.projeto.codeinsights.domain.identity.port.PasswordEncoderPort;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;

/**
 * O registro muda de forma conforme exista ou nao canal de e-mail. O caso sem e-mail e o modo real
 * do ambiente publico, nao um fallback de emergencia.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private TokenVerificacaoRepository tokenVerificacaoRepository;
    @Mock
    private EmailSenderPort emailSenderPort;

    @InjectMocks
    private RegistrarUsuarioUseCase useCase;

    private final RegistrarUsuarioInput input =
            new RegistrarUsuarioInput("aluno", "aluno@exemplo.com", "senha-forte");

    private void cenarioSemConflito() {
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existePorUsername(anyString())).thenReturn(false);
        when(passwordEncoderPort.encode(anyString())).thenReturn("hash");
    }

    /**
     * Sem e-mail, a conta precisa nascer ATIVO: o login recusa qualquer outro status, entao deixa-la
     * pendente prenderia o aluno fora da plataforma para sempre.
     */
    @Test
    void semProvedorDeEmailAContaJaNasceAtivaESemToken() {
        cenarioSemConflito();
        when(emailSenderPort.habilitado()).thenReturn(false);

        RegistroDTO dto = useCase.execute(input);

        assertThat(dto.precisaAtivarPorEmail()).isFalse();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).salvar(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusConta.ATIVO);

        verify(tokenVerificacaoRepository, never()).salvar(any());
        verify(emailSenderPort, never()).enviarEmailAtivacao(anyString(), anyString(), anyString());
    }

    @Test
    void comProvedorDeEmailAContaFicaPendenteEOTokenEEnviado() {
        cenarioSemConflito();
        when(emailSenderPort.habilitado()).thenReturn(true);
        when(usuarioRepository.salvar(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        RegistroDTO dto = useCase.execute(input);

        assertThat(dto.precisaAtivarPorEmail()).isTrue();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).salvar(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusConta.PENDENTE_VERIFICACAO);

        verify(tokenVerificacaoRepository).salvar(any());
        verify(emailSenderPort).enviarEmailAtivacao(anyString(), anyString(), anyString());
    }
}
