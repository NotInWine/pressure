package com.pressure.core;

/**
 * 函数接口
 * @param <I> 输入
 * @param <R> 输出
 * @author YL
 */
@FunctionalInterface
public interface LoadConsumer<I, R>  {

    /**
     * 接口方法
     * @param i
     * @return
     */
    R implement(I i);
}
