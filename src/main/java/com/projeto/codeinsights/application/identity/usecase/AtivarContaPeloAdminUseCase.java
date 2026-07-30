package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.ContaAtivadaDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Ativa uma conta sem passar pelo e-mail de confirmacao.
 * <p>
 * Existe porque a entrega de e-mail e o elo mais fragil do piloto: a mensagem cai em spam,
 * o aluno apaga sem ver, o limite diario do remetente estoura. Sem esta saida, um aluno
 * nessa situacao fica travado fora da plataforma no dia da coleta e nao ha o que fazer.
 * <p>
 * Diferente de {@link AtivarContaUseCase}, aqui nao ha token: quem responde pela identidade
 * do aluno e o administrador, que so aciona isto para alguem que ele conhece. Por isso a
 * rota vive sob {@code /api/admin/**} e nunca pode ser exposta ao proprio usuario — seria
 * autoativacao, e o e-mail deixaria de provar coisa alguma.
 */
@Service
@RequiredArgsConstructor
public class AtivarContaPeloAdminUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ContaAtivadaDTO execute(String email) {
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new NegocioException("Nenhum usuario cadastrado com o e-mail informado."));

        usuario.ativarConta();
        Usuario salvo = usuarioRepository.salvar(usuario);

        return new ContaAtivadaDTO(salvo.getId(), salvo.getUsername(), salvo.getEmail(), salvo.getStatus());
    }
}
