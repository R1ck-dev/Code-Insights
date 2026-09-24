import java.util.Scanner;
public class Main {
    static char[][] grade;
    static int n, m;
    static void apaga(int i, int j) {
        if (i < 0 || j < 0 || i >= n || j >= m || grade[i][j] != '#') return;
        grade[i][j] = '.';
        apaga(i + 1, j);
        apaga(i - 1, j);
        apaga(i, j + 1);
        apaga(i, j - 1);
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        n = sc.nextInt();
        m = sc.nextInt();
        grade = new char[n][];
        for (int i = 0; i < n; i++) grade[i] = sc.next().toCharArray();
        int ilhas = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                if (grade[i][j] == '#') { ilhas++; apaga(i, j); }
        System.out.println(ilhas);
    }
}
