class Solucao {
    boolean temDuplicada(int[] v) {
        for (int i = 0; i < v.length; i++)
            for (int j = i + 1; j < v.length; j++)
                if (v[i] == v[j]) return true;
        return false;
    }
}
