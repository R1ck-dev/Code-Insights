class ArvoreBinaria {
    static class No {
        int valor;
        No esq, dir;
        No(int valor) { this.valor = valor; }
        int altura() {
            int he = (esq == null) ? 0 : esq.altura();
            int hd = (dir == null) ? 0 : dir.altura();
            return 1 + Math.max(he, hd);
        }
    }
}
