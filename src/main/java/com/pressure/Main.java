package com.pressure;

import com.pressure.core.bean.RequestInfo;
import com.pressure.core.httputil.RequestInterface;
import com.pressure.core.httputil.impl.LogResultInfoMonitor;
import com.pressure.core.httputil.impl.RequestImpl;
import com.pressure.core.httputil.impl.SummaryMonitor;

import java.io.File;

/**
 * 测试
 *
 * @author YL
 */
public class Main {

    public static void main(String[] args) {

        /**
         * 获取请求处理器
         * 总请求次数等于循环次数 * RequestInfo 脚本长度
         * @param poolSize 线程池并发线程数量
         * @param requestSize 循环请求次数
         */
        RequestInterface requestInterface = RequestImpl.build(
                500,
                5000)
                .setContentTimeOut(3000) // 链接超时时间 毫秒
                .setSocketTimeOut(5000)  // 响应超时时间 毫秒
                .setMonitors(
                        /**
                         * 设置监听（收集统计压测记录）
                         * 目前自带两个款监听器
                         * @see com.pressure.core.httputil.impl.LogResultInfoMonitor 日志记录
                         * @see com.pressure.core.httputil.impl.SummaryMonitor 汇总打印
                         *
                         * 想要实现自定义的监听器可实现:
                         * @see com.pressure.core.httputil.ResultInfoMonitor
                         * 在此处配置给处理器
                         */
                        new LogResultInfoMonitor(new File("C:\\Users\\yangchao\\Desktop\\test\\log.log")),
                        new SummaryMonitor()
                );

        // 配置请求脚本
        requestInterface.send(
                new RequestInfo(
                        "列表",
                        "https://xkx.xxx.com/api/video/pay/start?start=1&count=15&keyWord=",
                        RequestInfo.RequestMethod.GET
                ),
                new RequestInfo(
                        "详情(包含动态参数)",
                        "https://xkx.xxx.com/api/video/playDetail?videoId=${data.data.$2.id}&token=",
                        RequestInfo.RequestMethod.GET
                ),
                new RequestInfo(
                        "子列表(包含动态参数)",
                        "https://xkx.xxx.com/api/video/getVideList?recordId=${data.videoId}&type=video",
                        RequestInfo.RequestMethod.GET
                ),
                new RequestInfo(
                        "子列表详情(包含动态参数)",
                        "https://xkx.xxx.com/api/video/playDetail?videoId=${data.$1.id}&token=",
                        RequestInfo.RequestMethod.GET
                )
        );
    }
}
