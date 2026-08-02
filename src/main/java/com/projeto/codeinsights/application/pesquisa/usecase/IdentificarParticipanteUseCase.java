package com.projeto.codeinsights.application.pesquisa.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.pesquisa.dto.ParticipanteIdentificadoDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.pesquisa.model.PseudonimoDeAluno;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Desfaz o pseudonimo — deliberadamente como <b>um ato explicito</b>, e nao como campo escondido na
 * listagem. Ver o dado e o padrao; saber de quem ele e exige pedir.
 * <p>
 * Num piloto fechado isso e necessario: quando um participante trava, o pesquisador precisa saber
 * quem procurar. O que o desenho evita e o oposto — ler a coorte inteira ja sabendo o nome de cada
 * um, que e como o vies de interpretacao entra sem ninguem perceber.
 * <p>
 * A busca inverte o pseudonimo recalculando-o para cada participante, porque a derivacao e de mao
 * unica. So entram usuarios que <b>tem</b> resolucao: quem nunca submeteu nao e participante e nao
 * deveria ser identificavel por esta rota.
 */
@Service
@RequiredArgsConstructor
public class IdentificarParticipanteUseCase {

    private final CoorteRepository coorteRepository;
    private final ObterParticipantesConsentidosUseCase obterParticipantesConsentidosUseCase;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public ParticipanteIdentificadoDTO execute(String pseudonimo) {
        if (pseudonimo == null || pseudonimo.isBlank()) {
            throw new NegocioException("Informe o pseudonimo do participante.");
        }
        String procurado = pseudonimo.trim().toUpperCase();

        List<UUID> participantes = coorteRepository
                .listarCoorte(obterParticipantesConsentidosUseCase.execute()).stream()
                .map(ResolucaoDaCoorte::autorId)
                .distinct()
                .toList();

        return usuarioRepository.buscarPorIds(participantes).stream()
                .filter(usuario -> PseudonimoDeAluno.de(usuario.getId()).equals(procurado))
                .findFirst()
                .map(usuario -> paraDTO(procurado, usuario))
                .orElseThrow(() -> new NegocioException(
                        "Nenhum participante corresponde ao pseudonimo informado."));
    }

    private ParticipanteIdentificadoDTO paraDTO(String pseudonimo, Usuario usuario) {
        return new ParticipanteIdentificadoDTO(
                pseudonimo, usuario.getId(), usuario.getUsername(), usuario.getEmail());
    }
}
