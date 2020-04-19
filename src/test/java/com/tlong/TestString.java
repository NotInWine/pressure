package com.tlong;



public class TestString {

    public static void main(String[] args) {
        String s = "\uD83C\uDF3A";
        System.out.println(s.codePointAt(0));
        s.chars().forEach(i -> System.out.print((char) i + " "));
        s.chars().forEach(i -> System.out.print("\\u" + Integer.toHexString(i)));
        System.out.println();
        s.chars().forEach(i -> System.out.print(i + " "));
        System.out.println();
        s.codePoints().forEach(i -> System.out.print((char) i  + " "));
        s.codePoints().forEach(i -> System.out.print("\\u" + Integer.toHexString(i)));
        System.out.println();
        s.codePoints().forEach(i -> System.out.print(i + " "));

        System.out.println(s);
        System.out.println((char) 1111);
    }
}
