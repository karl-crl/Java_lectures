package examples_1;

import examples_1.ex_reflection.B;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class Main {
    public static void main(String[] args) {
        Class<Integer> integ = int.class;
        Class<int[]> intArray = int[].class;
        Class<Integer> intBox = Integer.class;
        Class<Integer> intBoxs = Integer.TYPE; // equals integ

        B.C c = new B.C();
        Field[] fields = c.getClass().getFields();


        A a = A.create();
        // ex_reflection.A privateA = new ex_reflection.A(10); ошибка компиляции
        int i = 30000;
        byte b = (byte) i;
        char c1 = 1;
        char c2 = 1;
        char c3 = (char) (c1 + c2);
    }
}
