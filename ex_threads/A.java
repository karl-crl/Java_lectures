package ex_threads;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class A {
    public Lock lock = new ReentrantLock();
    public Condition cond = lock.newCondition();
    public void foo() throws InterruptedException {
        Integer a = 1;
        cond.await();
    }
}
