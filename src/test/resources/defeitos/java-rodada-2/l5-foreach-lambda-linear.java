import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < n; i++) nums.add(sc.nextInt());

        List<Integer> repetidos = new ArrayList<>();
        nums.forEach(x -> {
            if (Collections.frequency(nums, x) > 1 && !repetidos.contains(x)) {
                repetidos.add(x);
            }
        });
        System.out.println(repetidos);
    }
}
