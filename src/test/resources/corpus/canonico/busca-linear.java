class Solucao {
    int indiceDe(int[] v, int alvo) {
        for (int i = 0; i < v.length; i++) {
            if (v[i] == alvo) return i;
        }
        return -1;
    }
}
