package com.projeto.codeinsights.application.identity.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.UsuarioAdminDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * O diretorio da administracao: todas as contas, com papel e status a vista.
 * <p>
 * Existe para que promover alguem deixe de exigir saber o e-mail de cor. As rotas administrativas
 * identificam a pessoa por e-mail — o que e certo como contrato de API e pessimo como interface —, e
 * sem uma listagem o administrador teria de consultar o banco antes de usar a propria tela.
 * <p>
 * <b>Paginado, ao contrario da coorte.</b> Aqui a operacao e sobre uma pessoa de cada vez; nao ha
 * agregado a calcular que exija a base inteira, e uma lista de contas cresce sem teto.
 */
@Service
@RequiredArgsConstructor
public class ListarUsuariosParaAdminUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Pagina<UsuarioAdminDTO> execute(String busca, int pagina, int tamanho) {
        Pagina<Usuario> encontrados = usuarioRepository.listarTodos(busca, pagina, tamanho);

        return new Pagina<>(
                encontrados.itens().stream().map(this::paraDTO).toList(),
                encontrados.paginaAtual(),
                encontrados.totalPaginas(),
                encontrados.totalItens());
    }

    private UsuarioAdminDTO paraDTO(Usuario usuario) {
        return new UsuarioAdminDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getStatus(),
                usuario.getCriadoEm());
    }
}
