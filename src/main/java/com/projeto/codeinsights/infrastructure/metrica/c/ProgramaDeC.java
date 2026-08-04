package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.List;
import java.util.Optional;

/**
 * Um programa em C visto pelo motor de metricas: as funcoes definidas no arquivo e a sua
 * estrutura de fluxo de controle.
 *
 * @param integro {@code false} quando o parser nao conseguiu fechar a estrutura (chave ou
 *        parentese sem par). O motor precisa <b>saber</b> que falhou, em vez de seguir com uma
 *        arvore truncada: uma chave a menos engoliria o resto do arquivo dentro do ultimo laco e
 *        produziria um Big-O inventado, sem nenhum sinal de que algo deu errado.
 * @param motivo explicacao da falha, que vai para o campo {@code detalhe} do resultado
 */
public record ProgramaDeC(List<FuncaoDeC> funcoes, boolean integro, String motivo) {

    /**
     * @param parametros nomes dos parametros, na ordem. Precisam ser conhecidos porque as regras de
     *        recursao perguntam <b>como o argumento encolhe</b>: sem saber que {@code n} e
     *        parametro, {@code f(n / 2)} e {@code f(k / 2)} seriam indistinguiveis.
     */
    public record FuncaoDeC(String nome, List<String> parametros, NoDeC corpo) {

        public FuncaoDeC {
            parametros = List.copyOf(parametros);
        }

        /** Identidade da funcao: nome + aridade, como {@code AstUtils.chaveDoMetodo} no lado Java. */
        public String chave() {
            return nome + "/" + parametros.size();
        }
    }

    public static ProgramaDeC intacto(List<FuncaoDeC> funcoes) {
        return new ProgramaDeC(List.copyOf(funcoes), true, null);
    }

    public static ProgramaDeC quebrado(String motivo) {
        return new ProgramaDeC(List.of(), false, motivo);
    }

    public boolean vazio() {
        return funcoes.isEmpty();
    }

    public Optional<FuncaoDeC> porChave(String chave) {
        return funcoes.stream().filter(funcao -> funcao.chave().equals(chave)).findFirst();
    }

    /**
     * Funcao de mesmo nome, qualquer aridade. Em C uma chamada nao carrega o tipo dos argumentos, e
     * um argumento pode ser uma expressao com virgula dentro de parenteses; contar argumentos por
     * virgulas erra nesses casos. Quando a aridade nao casa, cair no nome ainda acerta o alvo,
     * porque C nao tem sobrecarga de funcao.
     */
    public Optional<FuncaoDeC> porNome(String nome) {
        return funcoes.stream().filter(funcao -> funcao.nome().equals(nome)).findFirst();
    }
}
