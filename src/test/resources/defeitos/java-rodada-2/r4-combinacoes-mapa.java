import java.util.Map;
class Solucao {
    static int comb(Map<Integer, Boolean> usado, int n, int i, int k) {
        if (k == 0) return 1;
        int total = 0;
        for (int j = i; j < n; j++)
            if (!usado.getOrDefault(j, false)) {
                usado.put(j, true);
                total += comb(usado, n, j + 1, k - 1);
                usado.put(j, false);
            }
        return total;
    }
}
