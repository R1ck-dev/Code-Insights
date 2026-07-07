package com.projeto.codeinsights.infrastructure.persistence.identity.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.identity.enums.StatusConta;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.infrastructure.persistence.identity.entity.UsuarioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.identity.mapper.UsuarioMapper;
import com.projeto.codeinsights.infrastructure.persistence.identity.repository.SpringDataUsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final SpringDataUsuarioRepository springDataUsuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    public Usuario salvar(Usuario usuario) {
        UsuarioJpaEntity entity = usuarioMapper.toEntity(usuario);
        UsuarioJpaEntity salvo = springDataUsuarioRepository.save(entity);
        return usuarioMapper.toDomain(salvo);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return springDataUsuarioRepository.findById(id).map(usuarioMapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return springDataUsuarioRepository.findByEmail(normalizarEmail(email)).map(usuarioMapper::toDomain);
    }

    @Override
    public boolean existePorEmail(String email) {
        return springDataUsuarioRepository.existsByEmail(normalizarEmail(email));
    }

    @Override
    public boolean existePorUsername(String username) {
        return username != null && springDataUsuarioRepository.existsByUsername(username.trim());
    }

    @Override
    public List<Usuario> buscarPorIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return springDataUsuarioRepository.findAllById(ids).stream()
                .map(usuarioMapper::toDomain)
                .toList();
    }

    @Override
    public Pagina<Usuario> listarPublicos(UUID excluidoId, String filtroUsername, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "username"));
        String filtro = filtroUsername == null ? "" : filtroUsername.trim();
        Page<UsuarioJpaEntity> page = springDataUsuarioRepository
                .findByVisibilidadePerfilAndStatusAndIdNotAndUsernameContainingIgnoreCase(
                        Visibilidade.PUBLICO, StatusConta.ATIVO, excluidoId, filtro, pageRequest);
        List<Usuario> itens = page.getContent().stream().map(usuarioMapper::toDomain).toList();
        return new Pagina<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
