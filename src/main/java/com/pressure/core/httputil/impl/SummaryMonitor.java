package com.pressure.core.httputil.impl;

import com.pressure.core.bean.ResultInfo;
import com.pressure.core.httputil.ResultInfoMonitor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 汇总监控
 * 汇总结果将打印出来，汇总结果见
 * @see RequestLog#toString(int)
 * @author YL
 **/
public class SummaryMonitor implements ResultInfoMonitor<String> {

    /**
     * 按name统计
     */
    private Map<String, RequestLog> nameSummary = new HashMap<>();

    /**
     * 按秒统计
     */
    private Map<Long, RequestLog> secondSummary = new HashMap<>();

    @Override
    public void add(ResultInfo resultInfo) {
        RequestLog nameLog = getLogOrInit(resultInfo, resultInfo.getRequestInfo().getName(), nameSummary);
        RequestLog secondLog = getLogOrInit(resultInfo, (resultInfo.getBeginTime() + resultInfo.getTime()) / 1000, secondSummary);

        long time = resultInfo.getTime();
        boolean success = resultInfo.getState() == ResultInfo.State.SUCCESS;

        nameLog.add((int) time, success);
        secondLog.add((int) time, success);
    }

    /**
     * @return
     */
    @Override
    public String out() {
        StringBuilder str = new StringBuilder();

        if (nameSummary.size() > 0) {
            this.nameSummary.forEach((k, v) -> {
                str.append(k).append(":").append(v.toString(1)).append("\n");
            });
        }

        if (secondSummary.size() > 0) {
            Collection<RequestLog> values = secondSummary.values();
            RequestLog log = RequestLog.merge(values.toArray(new RequestLog[0]));
            str.append("汇总:").append(log.toString(values.size()));
        }

        if (str.length() == 0) {
            return null;
        }

        return str.append("\n").toString();
    }


    /**
     * 获取或者初始化log
     *
     * @param resultInfo
     * @param key
     * @param map
     * @param <T>
     * @return
     */
    private <T> RequestLog getLogOrInit(ResultInfo resultInfo, T key, Map<T, RequestLog> map) {
        RequestLog requestLog = map.get(key);
        if (requestLog == null) {
            // 以响应时间点的统计作为性能依据
            requestLog = initResultLog(resultInfo);
            map.put(key, requestLog);
        }
        return requestLog;
    }

    /**
     * 初始化log
     *
     * @param resultInfo
     * @return
     */
    private RequestLog initResultLog(ResultInfo resultInfo) {
        return new RequestLog((resultInfo.getBeginTime() + resultInfo.getTime()) / 1000);
    }


    /**
     * 请求信息记录
     * 用于统计不支持并发访问的
     *
     * @author YL
     **/
    protected static class RequestLog {

        /**
         * 秒数的时间戳，可以用于分隔统计
         */
        private final long second;

        /**
         * 最大响应时间
         */
        private int maxTime;

        private int minTime = Integer.MAX_VALUE;

        /**
         * 总响应时间
         */
        private int timeTotal;

        /**
         * 请求次数
         */
        private int requestCount;

        /**
         * 请求次数
         */
        private int errorCount;

        public RequestLog(long second) {
            this.second = second;
        }

        /**
         * 多个log合并成一个
         *
         * @param logs
         * @return
         */
        public static RequestLog merge(RequestLog... logs) {
            if (logs.length == 1) {
                return logs[0];
            }
            RequestLog log = new RequestLog(logs[0].getSecond());
            for (RequestLog rl : logs) {
                if (log.maxTime < rl.maxTime) {
                    log.maxTime = rl.maxTime;
                }
                if (log.minTime > rl.minTime) {
                    log.minTime = rl.minTime;
                }

                log.timeTotal += rl.timeTotal;
                log.requestCount += rl.requestCount;
                log.errorCount += rl.errorCount;
            }
            return log;
        }

        /**
         * 记一次请求
         *
         * @param time
         */
        public void add(int time, boolean success) {
            if (time > maxTime) {
                maxTime = time;
            } else if (time < minTime) {
                // TODO 极端情况下，会出现最小请求时间为初始值
                minTime = time;
            }

            if (!success) {
                errorCount++;
            }

            timeTotal += time;
            requestCount++;
        }

        public int getMaxTime() {
            return maxTime;
        }

        public int getMinTime() {
            return minTime;
        }

        public int getRequestCount() {
            return requestCount;
        }

        public long getSecond() {
            return second;
        }

        public int getTimeTotal() {
            return timeTotal;
        }

        public int getErrorCount() {
            return errorCount;
        }

        /**
         * @param mergeCount 合并数量(秒数)
         * @return
         */
        public String toString(int mergeCount) {
            String s = "{";
            if (mergeCount > 1) {
                s += "吞吐量(qps)=" + new BigDecimal(requestCount).divide(new BigDecimal(mergeCount),2, BigDecimal.ROUND_HALF_UP).toString();
            }
            s += ", 最慢响应=" + maxTime +
                    ", 最快响应=" + minTime +
                    ", 平均响应=" + timeTotal / requestCount +
                    ", 总请求次数=" + requestCount +
                    ", 请求异常次数=" + errorCount +
                    ", 异常率=" +  new BigDecimal(errorCount).divide(new BigDecimal(requestCount),4, BigDecimal.ROUND_HALF_UP).toString() +
                    '}';
            return s;
        }
    }
}
