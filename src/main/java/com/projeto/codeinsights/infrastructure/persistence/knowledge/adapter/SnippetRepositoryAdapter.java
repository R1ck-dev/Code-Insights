package com.projeto.codeinsights.infrastructure.persistence.knowledge.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Snippet;
import com.projeto.codeinsights.domain.knowledge.port.SnippetRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.SnippetJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper.SnippetMapper;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.repository.SpringDataSnippetRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SnippetRepositoryAdapter implements SnippetRepository {

    private final SpringDataSnippetRepository springDataSnippetRepository;
    private final SnippetMapper snippetMapper;

    @Override
    public Snippet salvar(Snippet snippet) {
        SnippetJpaEntity entity = snippetMapper.toEntity(snippet);
        SnippetJpaEntity salvo = springDataSnippetRepository.save(entity);
        return snippetMapper.toDomain(salvo);
    }

    @Override
    public Optional<Snippet> buscarPorId(UUID id) {
        return springDataSnippetRepository.findById(id).map(snippetMapper::toDomain);
    }

    @Override
    public Pagina<Snippet> listarPorAutor(UUID autorId, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho);
        Page<SnippetJpaEntity> page = springDataSnippetRepository.findByAutorId(autorId, pageRequest);
        List<Snippet> itens = page.getContent().stream().map(snippetMapper::toDomain).toList();
        return new Pagina<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    @Override
    public void remover(UUID id) {
        springDataSnippetRepository.deleteById(id);
    }
}
