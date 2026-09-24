void main() {
    java.util.Scanner sc = new java.util.Scanner(System.in);
    int n = sc.nextInt();
    int[] v = new int[n];
    for (int i = 0; i < n; i++) v[i] = sc.nextInt();
    int pares = 0;
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++)
            if (v[i] + v[j] == 0) pares++;
    System.out.println(pares);
}
