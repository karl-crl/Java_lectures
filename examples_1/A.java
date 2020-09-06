package examples_1;

public class A {
    public int a;
    private A () {
        a = 10;
    }
    public static A create() {
        return new A();
    }
    private A (int a) {
        this.a = a;
    }
}
