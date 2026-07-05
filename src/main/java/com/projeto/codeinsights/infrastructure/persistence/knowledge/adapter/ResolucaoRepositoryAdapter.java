package com.projeto.codeinsights.infrastructure.persistence.knowledge.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.AtividadeRecente;
import com.projeto.codeinsights.domain.knowledge.model.PontoEvolucaoMensal;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;
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
    public Pagina<Resolucao> listarPublicasPorDesafio(UUID desafioId, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(pagina, tamanho);
        Page<ResolucaoJpaEntity> page = springDataResolucaoRepository.findByDesafioIdAndVisibilidade(
                desafioId, Visibilidade.PUBLICO, pageRequest);
        List<Resolucao> itens = page.getContent().stream().map(resolucaoMapper::toDomain).toList();
        return new Pagina<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    @Override
    public long contarPorDesafio(UUID desafioId) {
        return springDataResolucaoRepository.countByDesafioId(desafioId);
    }

    @Override
    public long contarPublicasPorDesafio(UUID desafioId) {
        return springDataResolucaoRepository.countByDesafioIdAndVisibilidade(desafioId, Visibilidade.PUBLICO);
    }

    @Override
    public long contarPorAutor(UUID autorId) {
        return springDataResolucaoRepository.countByAutorId(autorId);
    }

    @Override
    public long contarAnalisadasPorAutor(UUID autorId) {
        return springDataResolucaoRepository.countByAutorIdAndAnalisadaTrue(autorId);
    }

    @Override
    public Double mediaAutonomiaPorAutor(UUID autorId) {
        return springDataResolucaoRepository.mediaAutonomiaPorAutor(autorId);
    }

    @Override
    public List<PontoEvolucaoMensal> evolucaoMensalPorAutor(UUID autorId) {
        return springDataResolucaoRepository.evolucaoMensalPorAutor(autorId).stream()
                .map(r -> new PontoEvolucaoMensal(
                        ((Number) r[0]).intValue(),
                        ((Number) r[1]).intValue(),
                        r[2] == null ? null : ((Number) r[2]).doubleValue(),
                        ((Number) r[3]).longValue(),
                        r[4] == null ? null : ((Number) r[4]).doubleValue()))
                .toList();
    }

    @Override
    public List<AtividadeRecente> listarAtividadeRecentePorAutor(UUID autorId, int limite) {
        return springDataResolucaoRepository
                .atividadeRecentePorAutor(autorId, TipoMetrica.BIG_O_TEMPO, PageRequest.of(0, limite)).stream()
                .map(r -> new AtividadeRecente(
                        (UUID) r[0],
                        (UUID) r[1],
                        (String) r[2],
                        (LinguagemProgramacao) r[3],
                        ((Number) r[4]).intValue(),
                        (Boolean) r[5],
                        (String) r[6],
                        r[7] == null ? null : ((Number) r[7]).intValue(),
                        (OffsetDateTime) r[8]))
                .toList();
    }

    @Override
    public void remover(UUID id) {
        springDataResolucaoRepository.deleteById(id);
    }
}
