package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.TokenRedefinicaoDTO;
import com.projeto.codeinsights.domain.identity.enums.TipoToken;
import com.projeto.codeinsights.domain.identity.model.TokenVerificacao;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.TokenVerificacaoRepository;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * Gera um token de redefinicao de senha e o devolve a quem chamou, em vez de envia-lo por
 * e-mail. O administrador entrega o link ao aluno por outro canal (presencialmente, mensagem).
 * <p>
 * O mecanismo e o mesmo de {@link SolicitarRedefinicaoSenhaUseCase} — mesmo
 * {@link TokenVerificacao}, mesma expiracao de 24h, mesmo {@link RedefinirSenhaUseCase} do
 * outro lado. Muda so o <b>canal de entrega</b>, e e por isso que este caminho continua seguro:
 * o token vai para um administrador autenticado, nao para quem digitou um e-mail.
 * <p>
 * <b>Nao existe versao self-service disto.</b> Um endpoint publico que devolvesse o token de
 * redefinicao a partir do e-mail entregaria qualquer conta a qualquer pessoa que soubesse o
 * endereco. A prova de identidade tem de vir de fora da aplicacao: ou o e-mail, ou o admin.
 * <p>
 * Ao contrario do fluxo publico, aqui um e-mail inexistente levanta erro em vez de resposta
 * neutra. A resposta neutra de la protege contra enumeracao de contas por anonimos; para um
 * admin autenticado, o silencio so esconderia um erro de digitacao.
 */
@Service
@RequiredArgsConstructor
public class GerarTokenRedefinicaoSenhaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;

    @Transactional
    public TokenRedefinicaoDTO execute(String email) {
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new NegocioException("Nenhum usuario cadastrado com o e-mail informado."));

        TokenVerificacao token = new TokenVerificacao(usuario, TipoToken.REDEFINICAO_SENHA);
        tokenVerificacaoRepository.salvar(token);

        return new TokenRedefinicaoDTO(
                usuario.getUsername(), usuario.getEmail(), token.getToken(), token.getDataExpiracao());
    }
}
