package com.tlong;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * wait 方法， notify 方法测试
 * 限量供应无序排队的小面馆
 * @see Object#wait()
 * @see Object#notify()
 */
public class TestObject {

    private static final Object LOCK = new Object(); // 锁
    private static final AtomicInteger FOOD_NUMBER = new AtomicInteger(0); // 小面的数量

    private static class Customer implements Runnable {

        private final int TIME_OUT; // 超时时间 毫秒。  0 不会超时

        public Customer(int timeOut) {
            TIME_OUT = timeOut;
        }

        @Override
        public void run() {
            synchronized (LOCK) {
                System.out.println("【" + Thread.currentThread().getName() + "】: 来到前台排队取餐");
                long b = System.currentTimeMillis();
                try {
                    LOCK.wait(TIME_OUT);
                } catch (InterruptedException e) {
                    e.printStackTrace(); // 线程中断（Thread.interrupted()） 会抛出这个异常。 本次不测试此特性
                }
                if (FOOD_NUMBER.getAndAdd(-1) <= 0) {
                    FOOD_NUMBER.addAndGet(1);
                    System.out.println("【" + Thread.currentThread().getName() + "】: 还没有我的餐么？太遗憾了 我已经等了 " + (System.currentTimeMillis() - b) / 1000  + "秒 我不要了再见！");
                    return;
                }
                System.out.println("【" + Thread.currentThread().getName() + "】:  虽然等了 " + (System.currentTimeMillis() - b) / 1000  + "秒, 但是 好香的饭菜谢谢！ 我要去吃饭了！");
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(3*0.1 == 0.3);
        System.out.println(3*0.2 == 0.6);
        System.out.println(3*0.1);
        System.out.println(3*0.2);
        System.out.println(2*0.4);
        // 四个客人来到 小店取餐
        new Thread(new Customer(1000), "浩克").start();
        new Thread(new Customer(5000), "雷神").start();
        new Thread(new Customer(5000), "神奇女侠").start();
        new Thread(new Customer(5000), "钢铁侠").start();

        // 后厨开始做饭
        // 但是要1.5秒才能做好
        // 急躁的客人,运气不好的客人 吃不到饭咯
        Thread.sleep(1500);

        int number = 2;
        synchronized (LOCK) {
            for (int i = 0; i < number; i++) {
                FOOD_NUMBER.addAndGet(1);
                // 验证特性
                // notify 方法，只能负责唤醒线程。但不能指定线程唤醒顺序，与线程进入等待的时序无关
                LOCK.notify();
            }
        }
    }
}
