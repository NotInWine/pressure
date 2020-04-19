package com.tlong;


public class TestAbstracString {

    public static void main(String[] args) {
        int i = Integer.MAX_VALUE;
        System.out.println("int最大长度 二进制：" + Integer.toBinaryString(i));
        System.out.println("int最大长度 二进制位数：" + Integer.toBinaryString(i).length());
        while (true) {
            try {
                System.out.println(new char[i].length);
            } catch (OutOfMemoryError e) {
                i = i >> 1;
                // e.printStackTrace();
                // 异常继续
                continue;
            }
            break;
        }

        int scale = 100000000;
        System.out.println("精度提升：" + i + " " + scale);
        while (true) {
            try {
                if ((Integer.MAX_VALUE - scale) < i) {
                    scale = scale / 10;
                    System.out.println("调整刻度：" + i + " " + scale);
                    continue;
                } else {
                    i += scale;
                }
                char[] c = new char[i];
                c = null;
            } catch (OutOfMemoryError e) {
                if (scale == 1) {
                    System.out.println("数组最大长度 十进制：" + i);
                    System.out.println("数组最大长度 二进制：" + Integer.toBinaryString(i));
                    System.out.println("数组最大长度 二进制位数：" + Integer.toBinaryString(i).length());
                    break;
                }
                i -= scale;
                scale = scale / 10;
                System.out.println("调整刻度：" + i + " " + scale);
            }
        }

        try {
            char[] c = new char[i + 1];
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.out.println("再次校验通过");
            System.out.println("比 Integer.MAX_VALUE 小：" + (Integer.MAX_VALUE - i));
            return;
        }
        System.out.println("再次校未通过：" + i);
    }
}
