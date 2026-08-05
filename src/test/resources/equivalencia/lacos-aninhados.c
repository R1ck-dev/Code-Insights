int somarGrade(int grade[], int n) {
    int total = 0;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            total = total + grade[i * n + j];
        }
    }
    return total;
}
