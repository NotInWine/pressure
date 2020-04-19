package com.pressure.core.httputil;

import com.pressure.core.bean.ResultInfo;

/**
 * 压测结果监听
 * @param <OUT> 输出
 * @author YL
 */
public interface ResultInfoMonitor <OUT> {

    /**
     * 加入记录进行统计
     * @param resultInfo
     */
    void add(ResultInfo resultInfo);

    /**
     * 获得日志结果
     * @return
     */
    OUT out();

    /**
     * 用于打印
     * 系统会开启线程多次调用此方法，此方法何时调用out方法，以及以什么样的方式输出，由开发者自行定义
     * @param endNotice 最后一次通知了为 true
     * 默认的打印方法
     */
    default void printOut(boolean endNotice) {
        OUT out = this.out();
        if (out != null) {
            System.out.println(out);
        }
    }
}
