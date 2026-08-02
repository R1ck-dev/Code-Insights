package com.projeto.codeinsights.application.pesquisa.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.projeto.codeinsights.domain.identity.enums.Role;
import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.pesquisa.model.PseudonimoDeAluno;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

@ExtendWith(MockitoExtension.class)
class IdentificarParticipanteUseCaseTest {

    @Mock
    private CoorteRepository coorteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private IdentificarParticipanteUseCase useCase;

    private final UUID anaId = UUID.randomUUID();

    private Usuario ana() {
        return new Usuario(anaId, "ana.dev", "ana@exemplo.com", "hash", Role.ALUNO,
                Visibilidade.PUBLICO, StatusConta.ATIVO, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private void anaEhParticipante() {
        when(coorteRepository.listarCoorte()).thenReturn(List.of(CoorteDeTeste.analisada(anaId, 4)));
        when(usuarioRepository.buscarPorIds(any())).thenReturn(List.of(ana()));
    }

    @Test
    void devolveOParticipanteCorrespondenteAoPseudonimo() {
        anaEhParticipante();

        assertThat(useCase.execute(PseudonimoDeAluno.de(anaId))).satisfies(participante -> {
            assertThat(participante.usuarioId()).isEqualTo(anaId);
            assertThat(participante.username()).isEqualTo("ana.dev");
            assertThat(participante.email()).isEqualTo("ana@exemplo.com");
        });
    }

    /** O pesquisador vai digitar o codigo lendo da tela; espaco e caixa nao deveriam derrubar. */
    @Test
    void aceitaOPseudonimoComEspacoOuEmMinusculas() {
        anaEhParticipante();

        assertThat(useCase.execute("  " + PseudonimoDeAluno.de(anaId).toLowerCase() + " ").usuarioId())
                .isEqualTo(anaId);
    }

    @Test
    void falhaQuandoNenhumParticipanteCorresponde() {
        when(coorteRepository.listarCoorte()).thenReturn(List.of(CoorteDeTeste.analisada(anaId, 4)));
        when(usuarioRepository.buscarPorIds(any())).thenReturn(List.of(ana()));

        assertThatThrownBy(() -> useCase.execute("A-000000"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Nenhum participante");
    }

    /** Pseudonimo em branco nao deve virar uma varredura do banco. */
    @Test
    void recusaPseudonimoVazioSemConsultarNada() {
        assertThatThrownBy(() -> useCase.execute("  "))
                .isInstanceOf(NegocioException.class);

        verify(coorteRepository, never()).listarCoorte();
        verify(usuarioRepository, never()).buscarPorIds(any());
    }
}
