package com.pressure.core.httputil;

import com.pressure.core.bean.RequestInfo;
import com.pressure.core.bean.ResultInfo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 发送请求的接口 抽象类提供一些基本方法
 *
 * @author YL
 * time: 2018-12-24
 */
public abstract class AbstractRequest implements RequestInterface {


    private Condition condition = new ReentrantLock().newCondition();

    private ResultInfoMonitor<Object>[] monitors = null;

    /**
     * false 压测执行结束
     */
    protected volatile boolean loop = true;

    public AbstractRequest setMonitors(ResultInfoMonitor... monitors) {
        this.monitors = monitors;
        return this;
    }

    protected void putLog(ResultInfo resultInfo) {
        for (ResultInfoMonitor<Object> monitor : monitors) {
            monitor.add(resultInfo);
        }
    }

    protected void asyncPrint(boolean endNotice) {
        Thread thread = new Thread(() -> {
            System.out.println("启动打印线程");
            while (loop) {
                print(endNotice);
            }
        });
        thread.start();
    }

    /**
     * 打印监听结果
     * @param endNotice
     */
    protected void print(boolean endNotice) {
        try {
            for (ResultInfoMonitor<Object> monitor : monitors) {
                monitor.printOut(endNotice);
            }
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected String getValue(String key, JsonNode jsonNode) {
        String[] keys = key.split("\\.");
        JsonNode reJson = jsonNode;
        for (int i2 = 0; i2 < keys.length; i2++) {
            String sk = keys[i2];
            if (sk.contains("$")) {
                Iterator<JsonNode> iterators = reJson.iterator();
                int i = 0;
                int l = Integer.parseInt(sk.replace("$", ""));
                while (iterators.hasNext() && i <= l) {
                    i++;
                    reJson = iterators.next();
                }
            } else {
                reJson = reJson.get(sk);
            }
            if (reJson == null) {
                return null;
            }
        }
        return reJson.asText();
    }

    protected Map<String, String> getPostParams(RequestInfo ri, ResultInfo prResult) {
        Map<String, String> postParams = ri.getPostParams();
        if (postParams == null && ri.getLoadParams() != null) {
            postParams = ri.getLoadParams().implement(prResult);
        } else if (postParams == null) {
            postParams = new HashMap<>();
        }
        if (prResult != null) {
            Map<String, String> finalParams = postParams;
            postParams.forEach((k, v) -> {
                if (v.contains(".")) {
                    // 本次请求依赖上一次请求的返回结果
                    finalParams.put(k, getValue(v, prResult.getResult()));
                }
            });
        }
        return postParams;
    }

    /**
     * 获取最终的get 链接
     *
     * @param url
     * @param prReInfo
     * @return
     */
    protected String getGetUrl(String url, JsonNode prReInfo) {
        int b;
        if ((b = url.indexOf("${")) == -1) {
            return url;
        }

        String key = url.substring(b + 2, url.indexOf("}"));
        String val = getValue(key, prReInfo);
        url = url.replace("${" + key + "}", val);

        if (url.contains("${")) {
            return getGetUrl(url, prReInfo);
        }

        return url;
    }
}
