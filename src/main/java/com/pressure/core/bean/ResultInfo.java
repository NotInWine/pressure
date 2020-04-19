package com.pressure.core.bean;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 响应信息包装
 * @author YL
 */
public class ResultInfo {

    private final RequestInfo requestInfo;

    /**
     * 返回结果
     */
    private JsonNode result;

    private final HttpLog httpLog;

    /**
     * 耗时（毫秒）
     */
    private final long time;

    /**
     * 循环编号
     */
    private final int batchId;

    /**
     * 开始事件毫秒
     */
    private final long beginTime;

    private final State state;

    private final Throwable throwable;

    /**
     * 响应状态
     */
    public enum State {

        /**
         * 成功
         */
        SUCCESS,

        /**
         * 异常
         */
        ERROR;
    }

    public ResultInfo(RequestInfo requestInfo, HttpLog httpLog, long time, long beginTime, int batchId, State state) {
        this.requestInfo = requestInfo;
        this.httpLog = httpLog;
        this.time = time;
        this.beginTime = beginTime;
        this.batchId = batchId;
        this.state = state;
        this.throwable = null;
    }

    public ResultInfo(RequestInfo requestInfo, HttpLog httpLog, long time, long beginTime, int batchId, State state, Throwable throwable) {
        this.requestInfo = requestInfo;
        this.httpLog = httpLog;
        this.time = time;
        this.beginTime = beginTime;
        this.batchId = batchId;
        this.state = state;
        this.throwable = throwable;
    }

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    public HttpLog getHttpLog() {
        return httpLog;
    }

    public long getTime() {
        return time;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        String s = "" +
                "batchId=" + batchId +
                ", name=" + requestInfo.getName() +
                ", state=" + state +
                ", httpLog=" + httpLog +
                ", time=" + time;
        if (throwable != null) {
            s += ", throwable=" + throwable.getMessage();
        }
        return s;
    }
}
