package ex_sem_2;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.Executors;

public class ThreadExample {
    public static void main(String[] args) {

    }

    private static void threadpoolExample() {
        Executors.newFixedThreadPool() // конечное количество потоков
        Executors.newCachedThreadPool() // использует несколько потоков, но пытается не создавать новые потоки, а
        //переиспользовать уже существующие
    }


    // TODO: add example
//    private static Runnable createTask(String message, long delay) {
//        return () -> {
//            File file = new File("input.txt");
//            OutputStreamWriter writer = null;
//            try {
//                writer = OutputStreamWriter(new File)
//                Thread.sleep(delay * 1000);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    writer.close();
//                }
//            }
//        }
//    }
}
