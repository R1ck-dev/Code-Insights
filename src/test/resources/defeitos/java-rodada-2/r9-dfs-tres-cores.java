class Grafo {
    void visita(int[][] adjacencia, int[] cor, int atual) {
        if (cor[atual] != 0) return;
        cor[atual] = 1;
        for (int proximo = 0; proximo < adjacencia.length; proximo++)
            if (adjacencia[atual][proximo] == 1) visita(adjacencia, cor, proximo);
        cor[atual] = 2;
    }
}
