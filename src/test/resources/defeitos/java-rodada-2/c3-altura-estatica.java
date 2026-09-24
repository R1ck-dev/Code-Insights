class ArvoreBinaria {
    static class No {
        int valor;
        No esq, dir;
    }
    static int altura(No no) {
        if (no == null) return 0;
        return 1 + Math.max(altura(no.esq), altura(no.dir));
    }
}
