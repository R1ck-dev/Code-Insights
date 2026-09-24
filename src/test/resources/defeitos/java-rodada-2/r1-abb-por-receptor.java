import java.util.Scanner;
class No {
    int valor;
    No esq, dir;
    No(int valor) { this.valor = valor; }
    void insere(int x) {
        if (x < valor) {
            if (esq == null) esq = new No(x);
            else esq.insere(x);
        } else {
            if (dir == null) dir = new No(x);
            else dir.insere(x);
        }
    }
    int altura() {
        int he = (esq == null) ? 0 : esq.altura();
        int hd = (dir == null) ? 0 : dir.altura();
        return 1 + Math.max(he, hd);
    }
}
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        No raiz = new No(sc.nextInt());
        for (int i = 1; i < n; i++) raiz.insere(sc.nextInt());
        System.out.println(raiz.altura());
    }
}
