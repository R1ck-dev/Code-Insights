package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.ExcluirMinhaContaInput;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.PasswordEncoderPort;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Apaga a propria conta e tudo que ela produziu.
 * <p>
 * <b>Apaga de verdade</b>, e nao anonimiza: desafios, resolucoes, metricas, snippets e o
 * consentimento saem do banco junto com o usuario, por cascata do schema. E o que "excluir minha
 * conta" significa para quem clica, e o que sustenta o direito de retirar-se da pesquisa a qualquer
 * momento — prometer reter dado de quem pediu para sair seria uma promessa que a plataforma nao tem
 * como justificar.
 * <p>
 * A contrapartida foi aceita de olhos abertos: uma analise ja publicada deixa de ser reproduzivel a
 * partir do banco se alguem se retirar depois. A reprodutibilidade se apoia no <b>CSV exportado na
 * data do corte</b>, que e um retrato imutavel, e nao na permanencia das contas.
 */
@Service
@RequiredArgsConstructor
public class ExcluirMinhaContaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    @Transactional
    public void execute(ExcluirMinhaContaInput input) {
        Usuario usuario = usuarioRepository.buscarPorId(input.usuarioId())
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        // O JWT prova quem e, mas nao prova que a pessoa esta diante do teclado agora. Para um ato
        // irreversivel isso nao basta.
        if (!passwordEncoderPort.matches(input.senhaAtual(), usuario.getSenhaHash())) {
            throw new NegocioException("Senha incorreta.");
        }

        // Sem esta guarda, a conta administradora poderia se apagar e deixar a plataforma sem
        // ninguem capaz de ativar contas ou disparar reanalise — e o AdminSeeder so a recria se
        // ADMIN_PASSWORD estiver definida no ambiente, o que em producao pode nao estar.
        if (usuario.ehAdmin()) {
            throw new NegocioException(
                    "Conta administradora nao pode ser excluida por aqui: promova outra conta a admin antes.");
        }

        usuarioRepository.remover(usuario.getId());
    }
}
