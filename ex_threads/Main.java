package ex_threads;

public class Main {
    public static void main(String[] args) {
        Thread daemon = new Thread();
        daemon.setPriority(Thread.MAX_PRIORITY);
        System.out.println(daemon.getPriority());
        daemon.setDaemon(true);
        System.out.println(daemon.getPriority());
    }
}
