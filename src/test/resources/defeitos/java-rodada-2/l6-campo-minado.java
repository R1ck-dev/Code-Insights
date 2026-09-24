import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        char[][] campo = new char[n][];
        for (int i = 0; i < n; i++) {
            String linha = sc.next();
            campo[i] = linha.toCharArray();
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (campo[i][j] == '*') {
                    System.out.print('*');
                    continue;
                }
                int minas = 0;
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        int x = i + di, y = j + dj;
                        if (x >= 0 && x < n && y >= 0 && y < n && campo[x][y] == '*') {
                            minas++;
                        }
                    }
                }
                System.out.print(minas);
            }
            System.out.println();
        }
    }
}
