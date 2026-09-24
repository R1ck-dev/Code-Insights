import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
class Solucao {
    List<List<Integer>> tresSoma(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> resposta = new ArrayList<>();
        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            int esq = i + 1, dir = nums.length - 1;
            while (esq < dir) {
                int soma = nums[i] + nums[esq] + nums[dir];
                if (soma == 0) {
                    resposta.add(Arrays.asList(nums[i], nums[esq], nums[dir]));
                    esq++;
                    dir--;
                } else if (soma < 0) {
                    esq++;
                } else {
                    dir--;
                }
            }
        }
        return resposta;
    }
}
