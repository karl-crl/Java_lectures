package examples_1;

public class Main {
    public static void main(String[] args) {
        A a = A.create();
        // ex_reflection.A privateA = new ex_reflection.A(10); ошибка компиляции
        int i = 30000;
        byte b = (byte) i;
        char c1 = 1;
        char c2 = 1;
        char c3 = (char) (c1 + c2);
    }
}
