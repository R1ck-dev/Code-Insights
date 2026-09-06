// Catalogo de desafios e solucoes usado pelo seed.
//
// Cada desafio tem DUAS solucoes: uma "ingenua" e uma "refinada". Essa dupla e o ponto do
// conjunto inteiro — e ela que faz o motor de metricas produzir a queda de complexidade que a
// pesquisa investiga. Um seed com codigo aleatorio encheria as telas, mas os graficos de
// evolucao ficariam com ruido no lugar de tendencia.
//
// So ha codigo em JAVA e C: sao as unicas linguagens que o motor analisa. Uma submissao em
// Python entraria sem eixo de complexidade e abriria buraco nos graficos.

export const DESAFIOS = [
    {
        chave: 'two-sum',
        titulo: 'Two Sum',
        enunciado:
            'Dado um vetor de inteiros e um valor alvo, devolva os indices dos dois numeros cuja '
            + 'soma seja igual ao alvo. Existe exatamente uma resposta e o mesmo elemento nao pode '
            + 'ser usado duas vezes.',
        plataformaOrigem: 'LeetCode',
        identificadorExterno: '1',
        urlExterna: 'https://leetcode.com/problems/two-sum/',
    },
    {
        chave: 'maior-soma',
        titulo: 'Maior Soma de Subarray Contigua',
        enunciado:
            'Dado um vetor de inteiros (que pode conter negativos), encontre a subarray contigua '
            + 'de maior soma e devolva essa soma. Classico problema de Kadane.',
        plataformaOrigem: 'LeetCode',
        identificadorExterno: '53',
        urlExterna: 'https://leetcode.com/problems/maximum-subarray/',
    },
    {
        chave: 'busca-elemento',
        titulo: 'Busca em Vetor Ordenado',
        enunciado:
            'Dado um vetor ordenado de forma crescente e um valor procurado, devolva o indice do '
            + 'valor ou -1 caso ele nao exista no vetor.',
        plataformaOrigem: 'beecrowd',
        identificadorExterno: '1182',
        urlExterna: 'https://judge.beecrowd.com/pt/problems/view/1182',
    },
    {
        chave: 'fibonacci',
        titulo: 'N-esimo Termo de Fibonacci',
        enunciado:
            'Calcule o n-esimo termo da sequencia de Fibonacci, onde F(0) = 0, F(1) = 1 e '
            + 'F(n) = F(n-1) + F(n-2). O valor de n pode chegar a 90.',
        plataformaOrigem: 'beecrowd',
        identificadorExterno: '1151',
        urlExterna: 'https://judge.beecrowd.com/pt/problems/view/1151',
    },
    {
        chave: 'ordenacao',
        titulo: 'Ordenar Vetor de Inteiros',
        enunciado:
            'Implemente um algoritmo de ordenacao para um vetor de ate 100.000 inteiros e devolva '
            + 'o vetor em ordem crescente. Nao use a ordenacao pronta da biblioteca padrao.',
        plataformaOrigem: 'Codeforces',
        identificadorExterno: '339B',
        urlExterna: 'https://codeforces.com/problemset',
    },
    {
        chave: 'palindromo',
        titulo: 'Verificar Palindromo',
        enunciado:
            'Dada uma cadeia de caracteres, determine se ela e um palindromo, ignorando espacos e '
            + 'diferenca entre maiusculas e minusculas.',
        plataformaOrigem: 'beecrowd',
        identificadorExterno: '1235',
        urlExterna: 'https://judge.beecrowd.com/pt/problems/view/1235',
    },
    {
        chave: 'primos',
        titulo: 'Contar Primos ate N',
        enunciado:
            'Dado um inteiro N, conte quantos numeros primos existem no intervalo de 2 ate N. '
            + 'N pode chegar a 1.000.000.',
        plataformaOrigem: 'LeetCode',
        identificadorExterno: '204',
        urlExterna: 'https://leetcode.com/problems/count-primes/',
    },
    {
        chave: 'anagrama',
        titulo: 'Detectar Anagramas',
        enunciado:
            'Dadas duas cadeias de caracteres, determine se uma e anagrama da outra, ou seja, se '
            + 'ambas usam exatamente os mesmos caracteres com as mesmas frequencias.',
        plataformaOrigem: 'LeetCode',
        identificadorExterno: '242',
        urlExterna: 'https://leetcode.com/problems/valid-anagram/',
    },
    {
        chave: 'ilhas',
        titulo: 'Contar Ilhas na Matriz',
        enunciado:
            'Dada uma matriz de caracteres onde 1 representa terra e 0 representa agua, conte '
            + 'quantas ilhas existem. Uma ilha e um grupo de celulas de terra conectadas na '
            + 'horizontal ou na vertical.',
        plataformaOrigem: 'LeetCode',
        identificadorExterno: '200',
        urlExterna: 'https://leetcode.com/problems/number-of-islands/',
    },
    {
        chave: 'mochila',
        titulo: 'Problema da Mochila 0/1',
        enunciado:
            'Dados N itens, cada um com peso e valor, e uma mochila de capacidade W, escolha o '
            + 'subconjunto de itens de maior valor total que caiba na mochila. Cada item pode ser '
            + 'levado no maximo uma vez.',
        plataformaOrigem: 'beecrowd',
        identificadorExterno: '1487',
        urlExterna: 'https://judge.beecrowd.com/pt/problems/view/1487',
    },
];
