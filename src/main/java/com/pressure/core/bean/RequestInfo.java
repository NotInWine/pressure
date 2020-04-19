package com.pressure.core.bean;

import com.pressure.core.LoadConsumer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;

import java.util.Arrays;
import java.util.Map;

/**
 * 请求指令包
 * @author YL
 */
public class RequestInfo {

    /**
     * 名字用于除统计，不要重复
     */
    private final String name;

    /**
     * 请求地址
     * 动态参数使用此形式获取 ${data.data.$2.id} 上一个请求返回的json（或者toJson处理后的json）结构 如：
     * （上一个请求返回）：
     * {
     * 	code: 1,
     * 	msg: "success",
     * 	time: "1587270284",
     * 	data: {
     * 		data: [{
     * 			id: 520 获取此值（${data.data.$2.id}）,
     * 		    ....
     *          }]
     *     }
     * }
     * 完整uri: https://www.xxxxxx.com/api/video/playDetail?videoId=${data.data.$2.id}
     */
    private final String uri;

    private final RequestMethod method;

    /**
     * 请求头
     */
    private final Header[] headers;

    /**
     * 请求参数 仅post有效
     * 动态参数对value生效
     * {videoId:${data.data.$2.id}}
     */
    private final Map<String, String> postParams;

    /**
     * 处理返回结果时会调用此方法。
     * 未实现的话默认直接将返回结果转json，用于帮助下一个请求完成初始化参数
     */
    private final LoadConsumer<String, JsonNode> toJson;

    /**
     * 根据上下文获取请求参数
     * postParams 有值时loadParams 不生效
     */
    private final LoadConsumer<ResultInfo, Map<String, String>> loadParams;

    public RequestInfo(String name, String uri, RequestMethod method, Map<String, String> postParams, LoadConsumer<String, JsonNode> toJson, LoadConsumer<ResultInfo, Map<String, String>> loadParams, Header[] headers) {
        this.name = name;
        this.uri = uri;
        this.method = method;
        this.postParams = postParams;
        this.toJson = toJson;
        this.loadParams = loadParams;
        this.headers = headers;
    }

    public RequestInfo(String name, String uri, RequestMethod method) {
        this.name = name;
        this.uri = uri;
        this.method = method;
        this.postParams = null;
        this.toJson = null;
        this.headers = null;
        this.loadParams = null;
    }

    /**
     * 请求方法
     * @author YL
     */
    public enum RequestMethod {
        /**
         * get 请求
         */
        GET,

        /**
         * post 请求
         */
        POST;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public RequestMethod getMethod() {
        return method;
    }

    public Map<String, String> getPostParams() {
        return postParams;
    }

    public LoadConsumer<String, JsonNode> getToJson() {
        return toJson;
    }

    public LoadConsumer<ResultInfo, Map<String, String>> getLoadParams() {
        return loadParams;
    }

    @Override
    public String toString() {
        return "RequestInfo{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", method=" + method +
                ", headers=" + Arrays.toString(headers) +
                ", postParams=" + postParams +
                ", toJson=" + toJson +
                ", loadParams=" + loadParams +
                '}';
    }
}
