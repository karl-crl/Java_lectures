package ex_reflection;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(A.class.getFields()));
        //System.out.println(Arrays.toString(A.class.getField("priv_field")); //java.lang.NoSuchFieldException
        System.out.println(Arrays.toString(A.class.getDeclaredFields()));
    }
}
