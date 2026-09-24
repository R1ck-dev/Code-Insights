class Grafo {
    void visita(int[][] adjacencia, boolean[] visitado, int atual) {
        visitado[atual] = true;
        for (int proximo = 0; proximo < adjacencia.length; proximo++)
            if (adjacencia[atual][proximo] == 1 && !visitado[proximo]) visita(adjacencia, visitado, proximo);
    }
}
