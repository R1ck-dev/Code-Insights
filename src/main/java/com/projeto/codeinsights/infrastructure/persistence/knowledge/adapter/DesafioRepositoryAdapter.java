package com.projeto.codeinsights.infrastructure.persistence.knowledge.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.DesafioJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper.DesafioMapper;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.repository.SpringDataDesafioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DesafioRepositoryAdapter implements DesafioRepository {

    private final SpringDataDesafioRepository springDataDesafioRepository;
    private final DesafioMapper desafioMapper;

    @Override
    public Desafio salvar(Desafio desafio) {
        DesafioJpaEntity entity = desafioMapper.toEntity(desafio);
        DesafioJpaEntity salvo = springDataDesafioRepository.save(entity);
        return desafioMapper.toDomain(salvo);
    }

    @Override
    public Optional<Desafio> buscarPorId(UUID id) {
        return springDataDesafioRepository.findById(id).map(desafioMapper::toDomain);
    }

    @Override
    public Pagina<Desafio> listarPorAutor(UUID autorId, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho);
        Page<DesafioJpaEntity> page = springDataDesafioRepository.findByAutorId(autorId, pageRequest);
        List<Desafio> itens = page.getContent().stream().map(desafioMapper::toDomain).toList();
        return new Pagina<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    @Override
    public Pagina<Desafio> listarPublicosPorAutor(UUID autorId, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho);
        Page<DesafioJpaEntity> page = springDataDesafioRepository.findByAutorIdAndVisibilidade(
                autorId, Visibilidade.PUBLICO, pageRequest);
        List<Desafio> itens = page.getContent().stream().map(desafioMapper::toDomain).toList();
        return new Pagina<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    @Override
    public void remover(UUID id) {
        springDataDesafioRepository.deleteById(id);
    }
}
