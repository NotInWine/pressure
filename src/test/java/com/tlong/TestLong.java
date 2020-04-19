package com.tlong;


import java.util.concurrent.atomic.AtomicInteger;

public class TestLong {

    private static final Object COUNTER = new Object();
    private static final AtomicInteger ATOMICINTEGER = new AtomicInteger(0); // 数量

    private static class Foodie implements Runnable {

        private final int TIME_OUT; // 0 为不会超时

        public Foodie(int timeOut) {
            TIME_OUT = timeOut;
        }

        public Foodie() {
            TIME_OUT = 0;
        }

        @Override
        public void run() {
            synchronized (COUNTER) {
                System.out.println("e1" + Thread.currentThread().getName());
                try {
                    COUNTER.wait(TIME_OUT);
                    int c = ATOMICINTEGER.getAndAdd(-1);
                    if (c > 0) {
                        System.out.println("e1 " + Thread.currentThread().getName() + " 2 ");
                    } else {
                        ATOMICINTEGER.getAndAdd(1);
                        System.out.println("e1 " + Thread.currentThread().getName() + " 3 ");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class Boss implements Runnable {

        private final int COUNT; // 0 为不会超时

        public Boss(int count) {
            COUNT = count;
        }

        public Boss() {
            COUNT = 1;
        }

        @Override
        public void run() {
            System.out.println("b1" + Thread.currentThread().getName());
            synchronized (COUNTER) {
                if (COUNT == Integer.MAX_VALUE) {
                    COUNTER.notifyAll();
                    System.out.println("b1" + Thread.currentThread().getName() + " all");
                } else {
                    for (int i = 0; i < COUNT; i++) {
                        ATOMICINTEGER.addAndGet(1);
                        COUNTER.notify();
                    }
                    System.out.println("b1" + Thread.currentThread().getName() + " " + COUNT);
                }
            }
            System.out.println("b1" + Thread.currentThread().getName() + " 11");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(new Foodie()).start();
        new Thread(new Foodie(200)).start();
        new Thread(new Foodie()).start();
        new Thread(new Foodie()).start();
        new Thread(new Foodie(1000)).start();

        new Thread(new Boss(1)).start();

        Thread.sleep(500);
        new Thread(new Boss(1)).start();

        System.out.println(System.getProperty("file.encoding"));
    }
}
