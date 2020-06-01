package com.pressure.core.httputil.impl;

import com.pressure.core.bean.ResultInfo;
import com.pressure.core.httputil.ResultInfoMonitor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

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

        int secondSize = secondSummary.size();
        if (nameSummary.size() > 0) {
            this.nameSummary.forEach((k, v) -> {
                str.append("【").append(k).append("】\n").append(v.toString(secondSize)).append("\n");
            });
        }

        if (secondSize > 0 && nameSummary.size() > 1) {
            Collection<RequestLog> values = secondSummary.values();
            RequestLog log = RequestLog.merge(values.toArray(new RequestLog[0]));
            str.append("【汇总】\n").append(log.toString(values.size()));
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

        /**
         * 记录响应时长，以及对应的次数
         */
        private ConcurrentSkipListMap<Integer, Integer> timeData = new ConcurrentSkipListMap<>();

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
                rl.timeData.forEach((k, v) -> {
                    // 合并
                    Integer integer = log.timeData.get(k);
                    if (integer == null) {
                        integer = 0;
                    }
                    integer += v;

                    log.timeData.put(k, integer);
                });
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
            } else {
                Integer integer = timeData.get(time);
                if (integer == null) {
                    integer = 0;
                }
                timeData.put(time, integer + 1);
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
            int successNum = this.requestCount - this.errorCount;
            String s = "";
            if (mergeCount > 1) {
                s += "成功吞吐量(qps)=" + new BigDecimal(successNum).divide(new BigDecimal(mergeCount),2, RoundingMode.HALF_DOWN).toString();
                s += ",总吞吐量(qps)=" + new BigDecimal(requestCount).divide(new BigDecimal(mergeCount),2, RoundingMode.HALF_DOWN).toString();
            }

            Temp[] temps = new Temp[]{
                    new Temp("90%响应", successNum * 90 / 100),
                    new Temp("95%响应", successNum * 95/ 100),
                    new Temp("99%响应", successNum * 99 / 100),
            };

            int number = 0;
            Set<Map.Entry<Integer, Integer>> entries = this.timeData.entrySet();
            for (Map.Entry<Integer, Integer> entry : entries) {
                number += entry.getValue();
                for (Temp temp : temps) {
                    if (temp.value == 0 && number >= temp.number) {
                        temp.value = entry.getKey();
                    }
                }
            }

            StringBuilder str = new StringBuilder();
            for (Temp temp : temps) {
                str.append(", ").append(temp.title).append("=").append(temp.value);
            }

            s += str.toString() +
                    ", 最慢响应=" + maxTime +
                    ", 最快响应=" + minTime +
                    ", 平均响应=" + timeTotal / this.requestCount +
                    ", 总请求次数=" + this.requestCount +
                    ", 请求成功次数=" + successNum +
                    ", 请求异常次数=" + errorCount +
                    ", 异常率=" +  new BigDecimal(errorCount).divide(new BigDecimal(this.requestCount),4, BigDecimal.ROUND_HALF_UP).toString() +
                    "";
            return s;
        }
    }

    /**
     * 用于计算 n% 响应时间的临时存储结构
     */
    private static class Temp {

        final String title;
        final int number;
        int value;

        Temp(String title, int number) {
            this.title = title;
            this.number = number;
        }
    }
}
