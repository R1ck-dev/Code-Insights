class Solucao {
    long potenciaMod(long base, long expoente, long modulo) {
        long resultado = 1;
        base = base % modulo;
        while (expoente > 0) {
            if ((expoente & 1) == 1) {
                resultado = (resultado * base) % modulo;
            }
            expoente = expoente >> 1;
            base = (base * base) % modulo;
        }
        return resultado;
    }
}
