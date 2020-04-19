package com.tlong;


import java.util.LinkedHashSet;
import java.util.Set;

/**
 * UTF-16是16bit最多编码65536，那大于65536如何编码？Unicode 标准制定组想出的办法是，从这65536个编码里，拿出2048个，规定他们是「Surrogates」，让他们两个为一组，来代表编号大于65536的那些字符。
 * 编号为 U+D800 至 U+DBFF 的规定为「High Surrogates」，共1024个。
 * 编号为 U+DC00 至 U+DFFF 的规定为「Low Surrogates」，也是1024个。
 */
public class TestChar {

    /**
     * 高位最小值 U+D800
     */
    public static final char MIN_HIGH_SURROGATE = Character.MIN_HIGH_SURROGATE;

    /**
     * 低位最小值 U+DC00
     */
    public static final char MIN_LOW_SURROGATE = Character.MIN_LOW_SURROGATE;

    /**
     * 65536
     * 最小代码补充位置 选取为两个字节 即长度为16的二进制的最大值。 超过这个值即为补充代码点详情请强查看：
     *
     * @see java.lang.Character#toCodePoint(char, char) 源码
     */
    private final static int MIN_SUPPLEMENTARY_CODE_POINT = Character.MIN_SUPPLEMENTARY_CODE_POINT;

    public static void main(String[] args) {
        // 🌺好看
        String s = "\uD83C\uDF3A好看";
        System.out.println("原始字符：\n" + s);
        System.out.println();
        // 处理char
        Set<Integer> chars = new LinkedHashSet<>();
        s.codePoints().forEach(i -> {
            if (MIN_SUPPLEMENTARY_CODE_POINT < i) {
                System.out.println("高位合并后的 二进制：\n" + Integer.toBinaryString(i));
                System.out.println();
                // 还原：去除补位
                i -= MIN_SUPPLEMENTARY_CODE_POINT;
                System.out.println("还原后的代码点：\n" + i);
                System.out.println();
                System.out.println("还原后的代码点的二进制：\n" + Integer.toBinaryString(i));
                System.out.println();

                /**
                 * 取出二进制，高十位 和低十位
                 * 为什么是十位不是十六位 因为utf-16 使用Surrogates标识字符时 占用了每个编码的二进制的高六位，用于区分Surrogates 和 常规编码
                 * 「High Surrogates」「Low Surrogates」的最小值分别是
                 * @see MIN_HIGH_SURROGATE
                 * @see MIN_LOW_SURROGATE
                 */
                int low = i & Integer.parseInt("1111111111", 2); // 取出低十位
                int high = i >> 10; // 取出高十位
                System.out.println("「High Surrogates」+「Low Surrogates」 的二进制：\n" + Integer.toBinaryString(high) + " + " + Integer.toBinaryString(low));
                System.out.println();

                // 补位打印
                low = MIN_LOW_SURROGATE + low;
                high = MIN_HIGH_SURROGATE + high;

                chars.add(high);
                chars.add(low);
            } else {
                chars.add(i);
            }
        });


        System.out.println("对应的 utf-16 字符 \\u + 十六进制代码点：");
        chars.forEach(i -> System.out.print("\\u" + Integer.toHexString(i)));
        System.out.println();

        System.out.println("解码后字符：");
        chars.forEach(i -> System.out.print((char) i.intValue()));
        System.out.println();
    }
}
