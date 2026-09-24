class Solucao {
    public int maiorLucro(int[] p) {
        int menor = Integer.MAX_VALUE, lucro = 0;
        for (int x : p) { menor = Math.min(menor, x); lucro = Math.max(lucro, x - menor); }
        return lucro;
    }
    private int maiorLucroForcaBruta(int[] p) {
        int lucro = 0;
        for (int i = 0; i < p.length; i++)
            for (int j = i + 1; j < p.length; j++)
                lucro = Math.max(lucro, p[j] - p[i]);
        return lucro;
    }
}
