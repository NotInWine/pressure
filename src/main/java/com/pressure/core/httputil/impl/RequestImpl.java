package com.pressure.core.httputil.impl;

import com.pressure.core.Cache;
import com.pressure.core.bean.HttpLog;
import com.pressure.core.bean.RequestInfo;
import com.pressure.core.bean.ResultInfo;
import com.pressure.core.httputil.AbstractRequest;
import com.pressure.core.httputil.RequestInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.util.SimpleHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 发送请求的接口
 *
 * @author YL
 */
public class RequestImpl extends AbstractRequest implements RequestInterface {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 线程池并行数
     */
    private final int poolSize;

    /**
     * 请求循环数
     */
    private final int requestSize;

    /**
     * 链接超时时间
     */
    private int contentTimeOut = 1000;

    /**
     * 响应超时时间
     */
    private int socketTimeOut = 3000;

    /**
     * 总请求次数等于循环次数 * RequestInfo 脚本长度
     * @param poolSize 线程池并发线程数量
     * @param requestSize 循环请求次数
     * @return
     */
    public static RequestImpl build(int poolSize, int requestSize) {
        return new RequestImpl(poolSize, requestSize);
    }

    public RequestImpl setContentTimeOut(int contentTimeOut) {
        this.contentTimeOut = contentTimeOut;
        return this;
    }

    public RequestImpl setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
        return this;
    }

    private RequestImpl(int poolSize, int requestSize) {
        this.poolSize = poolSize;
        this.requestSize = requestSize;
    }

    private boolean close = false;

    /**
     * 发送测试脚本
     *
     * @param infos 指令集
     */
    @Override
    public synchronized void send(RequestInfo... infos) {
        if (close) {
            throw new RuntimeException("本方法不可重复调用, 新的测试请重新初始化");
        }
        close = true;

        System.out.println("BEGIN");

        BlockingQueue<ResultInfo> blockingQueue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor steadyThreadIdiotPool = createTask(infos, blockingQueue);


        // 这个监听会阻塞直到运行结束
        monitorAndBlock(steadyThreadIdiotPool, blockingQueue);

        // 没有打印干净的打印出来
        super.print(true);
    }

    /**
     * 监听
     * 这个监听会阻塞直到运行结束
     * @param steadyThreadIdiotPool
     * @param blockingQueue
     */
    protected void monitorAndBlock(ThreadPoolExecutor steadyThreadIdiotPool, BlockingQueue<ResultInfo> blockingQueue) {
        steadyThreadIdiotPool.shutdown();
        long b = System.currentTimeMillis();
        do {
            //等待所有任务完成
            try {
                ResultInfo take;
                while ((take = blockingQueue.poll()) != null){
                    putLog(take);
                }
                loop = !(steadyThreadIdiotPool.awaitTermination(5, TimeUnit.SECONDS) && blockingQueue.size() == 0);  //阻塞，直到线程池里所有任务结束
                System.out.println("线程池状态：" + steadyThreadIdiotPool + " queue " + blockingQueue.size() + ", loop " + loop);
                super.print(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (loop);
        System.out.println("TIME " + (System.currentTimeMillis() - b));
    }

    private ThreadPoolExecutor createTask(RequestInfo[] infos, BlockingQueue<ResultInfo> blockingQueue) {
        ThreadPoolExecutor steadyThreadIdiotPool = new ThreadPoolExecutor(poolSize, poolSize, 2000, TimeUnit.MILLISECONDS,
                new LinkedTransferQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < requestSize; i++) {
            int finalI = i;
            steadyThreadIdiotPool.execute(() -> {
                SimpleHttpClient sc = new SimpleHttpClient(contentTimeOut, socketTimeOut);
                ResultInfo resultInfo = null;
                for (RequestInfo ri : infos) {
                    // 循环处理请求
                    long begin = System.currentTimeMillis();
                    // 发送请求
                    HttpLog log = null;
                    try {
                        log = send(ri, sc, resultInfo);
                        resultInfo = new ResultInfo(
                                ri,
                                log,
                                System.currentTimeMillis() - begin,
                                begin,
                                finalI,
                                ResultInfo.State.SUCCESS);
                    } catch (Exception e) {
                        logger.error("", e);
                        resultInfo = new ResultInfo(
                                ri,
                                log,
                                System.currentTimeMillis() - begin,
                                begin,
                                finalI,
                                ResultInfo.State.ERROR,
                                e);
                    }

                    if (log != null) {
                        resultInfo.setResult(resultToJsonNode(ri, resultInfo.getHttpLog().getResponseBody()));
                    }

                    // 请求结果加入队列用于统计
                    blockingQueue.add(resultInfo);
                    if (resultInfo.getState() == ResultInfo.State.ERROR) {
                        break;
                    }
                }
            });
        }

        return steadyThreadIdiotPool;
    }

    private JsonNode resultToJsonNode(RequestInfo ri, String reStr) {
        if (reStr == null) {
            return null;
        }
        JsonNode jsonNode = null;
        try {
            if (ri.getToJson() != null) {
                jsonNode = ri.getToJson().implement(reStr);
            } else {
                jsonNode = Cache.JSON_UTIL.readTree(reStr);
            }
        } catch (IOException e) {
            logger.error("类型转换异常 {} {}", reStr, ri, e);
            e.printStackTrace();
        }
        return jsonNode;
    }

    /**
     * 发送请求
     *
     * @param ri
     * @param sc
     * @param prResult
     * @return
     * @throws IOException
     */
    private HttpLog send(RequestInfo ri, SimpleHttpClient sc, ResultInfo prResult) throws IOException {
        String url = ri.getUri();
        if (prResult != null) {
            url = getGetUrl(ri.getUri(), prResult.getResult());
        }
        switch (ri.getMethod()) {
            case GET:
                sc.get(url, ri.getHeaders());
                return new HttpLog(url, null, ri.getHeaders(), sc.getResponseInfo());
            case POST:
                Map<String, String> postParams = getPostParams(ri, prResult);
                sc.post(url, postParams, ri.getHeaders());
                return new HttpLog(ri.getUri(), postParams, ri.getHeaders(), sc.getResponseInfo());
            default:
                throw new RuntimeException("没有实现的请求方式 " + ri.getMethod());
        }
    }

    public static void main(String[] args) {
        System.out.println(".".contains("."));
        System.out.println(".".contains("\\."));
    }
}
