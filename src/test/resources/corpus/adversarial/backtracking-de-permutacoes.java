class Solucao {
    void permuta(int[] v, int k) {
        if (k == v.length) return;
        for (int i = k; i < v.length; i++) {
            int t = v[k];
            v[k] = v[i];
            v[i] = t;
            permuta(v, k + 1);
            t = v[k];
            v[k] = v[i];
            v[i] = t;
        }
    }
}
