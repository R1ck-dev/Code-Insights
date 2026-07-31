class Solucao {
    static void naoUsado(int n) {
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                int x = i * j;
            }
    }

    static int usado(int[] v) {
        int s = 0;
        for (int x : v) s += x;
        return s;
    }

    public static void main(String[] args) {
        int[] v = new int[8];
        System.out.println(usado(v));
    }
}
