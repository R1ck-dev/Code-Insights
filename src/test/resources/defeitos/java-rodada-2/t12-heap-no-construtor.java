import java.util.PriorityQueue;
import java.util.Queue;
class KMaiores {
    private Queue<Integer> heap;
    KMaiores() {
        heap = new PriorityQueue<>();
    }
    int kEsimoMaior(int[] v, int k) {
        for (int x : v) {
            heap.add(x);
            if (heap.size() > k) heap.poll();
        }
        return heap.peek();
    }
}
