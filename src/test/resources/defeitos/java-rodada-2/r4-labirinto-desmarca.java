class Solucao {
    int caminhos(int[][] lab, boolean[][] vis, int x, int y) {
        int n = lab.length;
        if (x < 0 || y < 0 || x >= n || y >= n || lab[x][y] == 1 || vis[x][y]) return 0;
        if (x == n - 1 && y == n - 1) return 1;
        vis[x][y] = true;
        int total = caminhos(lab, vis, x + 1, y) + caminhos(lab, vis, x - 1, y)
                + caminhos(lab, vis, x, y + 1) + caminhos(lab, vis, x, y - 1);
        vis[x][y] = false;
        return total;
    }
}
