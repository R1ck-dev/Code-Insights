package com.projeto.codeinsights.infrastructure.persistence.knowledge.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.entity.ResolucaoJpaEntity;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.mapper.ResolucaoMapper;
import com.projeto.codeinsights.infrastructure.persistence.knowledge.repository.SpringDataResolucaoRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResolucaoRepositoryAdapter implements ResolucaoRepository {

    private final SpringDataResolucaoRepository springDataResolucaoRepository;
    private final ResolucaoMapper resolucaoMapper;

    @Override
    public Resolucao salvar(Resolucao resolucao) {
        ResolucaoJpaEntity entity = resolucaoMapper.toEntity(resolucao);
        ResolucaoJpaEntity salvo = springDataResolucaoRepository.save(entity);
        return resolucaoMapper.toDomain(salvo);
    }

    @Override
    public Optional<Resolucao> buscarPorId(UUID id) {
        return springDataResolucaoRepository.findById(id).map(resolucaoMapper::toDomain);
    }

    @Override
    public Pagina<Resolucao> listarPorDesafio(UUID desafioId, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho);
        Page<ResolucaoJpaEntity> page = springDataResolucaoRepository.findByDesafioId(desafioId, pageRequest);
        List<Resolucao> itens = page.getContent().stream().map(resolucaoMapper::toDomain).toList();
        return new Pagina<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    @Override
    public long contarPorDesafio(UUID desafioId) {
        return springDataResolucaoRepository.countByDesafioId(desafioId);
    }

    @Override
    public void remover(UUID id) {
        springDataResolucaoRepository.deleteById(id);
    }
}
