class Solucao {
    static void gnome(int[] v) {
        int n = v.length;
        int i = 0;
        while (i < n) {
            if (i == 0 || v[i - 1] <= v[i]) i++;
            else { int t = v[i]; v[i] = v[i - 1]; v[i - 1] = t; i--; }
        }
    }
}
