package com.projeto.codeinsights.infrastructure.metrica;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;

/**
 * Mede um caso externo pelo mesmo caminho do corpus proprio ({@link MedidorDoCorpus}), com a
 * contabilidade do Adendo 7, item 1: cada caso tem um tempo limite (60 s por padrao), e excecao,
 * estouro de pilha ou estouro de tempo contam como <b>recusa</b>, com motivo em codigo curto. O caso
 * fica.
 * <p>
 * Nunca se guarda {@code getMessage()} nem a causa: o texto de uma excecao de parser pode citar o
 * fonte de terceiros. Fica so o nome simples da classe.
 * <p>
 * A analise roda numa thread daemon. Estourar o tempo nao a mata (uma analise que nao checa
 * interrupcao segue ate o fim), mas, sendo daemon, ela nao prende a JVM.
 */
final class MedidorExterno {

    static final Duration LIMITE_PADRAO = Duration.ofSeconds(60);

    private final Duration limite;
    private final BiFunction<String, LinguagemProgramacao, Medicao> analise;
    private final ExecutorService executor = Executors.newCachedThreadPool(MedidorExterno::threadDaemon);

    MedidorExterno() {
        this(LIMITE_PADRAO, MedidorDoCorpus::medir);
    }

    /** Ponto de injecao do teste: limite curto e uma analise que trava, sem mexer no motor. */
    MedidorExterno(Duration limite, BiFunction<String, LinguagemProgramacao, Medicao> analise) {
        this.limite = limite;
        this.analise = analise;
    }

    MedidaExterna medir(FonteVerificada fonte) {
        CasoExterno caso = fonte.caso();
        Future<Medicao> tarefa = executor.submit(() -> analise.apply(fonte.codigo(), caso.linguagem()));
        try {
            return MedidaExterna.respondida(caso, tarefa.get(limite.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            tarefa.cancel(true);
            return MedidaExterna.recusada(caso, MotivoRecusa.TEMPO_ESGOTADO, null);
        } catch (ExecutionException e) {
            return recusadaPorFalha(caso, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Medicao interrompida: " + caso);
        }
    }

    /**
     * Excecao, estouro de pilha e {@link AssertionError} (as assercoes do motor ficam ligadas no surefire,
     * e uma delas e defeito do caso, nao do ambiente) viram recusa; {@link OutOfMemoryError},
     * {@link LinkageError} e qualquer outro erro da JVM abortam, sem a causa (Adendo 7, item 1).
     */
    private static MedidaExterna recusadaPorFalha(CasoExterno caso, Throwable causa) {
        if (causa instanceof Exception || causa instanceof StackOverflowError || causa instanceof AssertionError) {
            return MedidaExterna.recusada(caso, MotivoRecusa.FALHA, causa.getClass().getSimpleName());
        }
        throw new IllegalStateException(
                "Erro fatal na analise de %s: %s".formatted(caso, causa.getClass().getSimpleName()));
    }

    private static Thread threadDaemon(Runnable tarefa) {
        Thread thread = new Thread(tarefa, "medidor-externo");
        thread.setDaemon(true);
        return thread;
    }
}
