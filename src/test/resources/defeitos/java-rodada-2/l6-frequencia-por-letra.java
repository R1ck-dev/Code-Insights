class Solucao {
    static int[] freq(String s) {
        int[] f = new int[26];
        for (char c = 'a'; c <= 'z'; c++) {
            for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) f[c - 'a']++;
        }
        return f;
    }
}
