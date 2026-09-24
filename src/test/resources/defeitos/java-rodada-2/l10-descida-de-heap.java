class Solucao {
    static void descer(int[] v, int i, int tam) {
        while (2 * i + 1 < tam) {
            int filho = 2 * i + 1;
            if (filho + 1 < tam && v[filho + 1] > v[filho]) filho++;
            if (v[i] >= v[filho]) break;
            int t = v[i]; v[i] = v[filho]; v[filho] = t;
            i = filho;
        }
    }
}
