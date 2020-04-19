package com.tlong;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟DC
 */
public class ObjectTest {

    private static final Object BAR_COUNTER = new Object(); //
    private static final AtomicInteger COUNT = new AtomicInteger(0); //

    private static class Guest implements Runnable {

        private final long HOLD_TIME;

        public Guest() {
            HOLD_TIME = 0;
        }

        public Guest(long HOLD_TIME) {
            this.HOLD_TIME = HOLD_TIME;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            System.out.println("【" + name + "】 QBTPD");
            synchronized (BAR_COUNTER) {
                System.out.println("【" + name + "】 PDLWZ,BDLYFC,ZZDDQC");
                try {
                    BAR_COUNTER.wait(HOLD_TIME); // 进入等待
                } catch (InterruptedException e) {
                    // 线程被调用 interrupt() 中断方法， 这里不测试这个情况
                    e.printStackTrace();
                }
                if (COUNT.getAndAdd(-1) <= 0) {
                    COUNT.getAndAdd(+1);
                    System.out.println("【" + name + "】 HAZH,WBYL");
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Guest(2000), "0").start();
        new Thread(new Guest(5000), "1").start();
        new Thread(new Guest(5000), "2").start();
        new Thread(new Guest(5000), "3").start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 3; i++) {
            synchronized (BAR_COUNTER) {
                BAR_COUNTER.notify();
            }
        }
    }
}
