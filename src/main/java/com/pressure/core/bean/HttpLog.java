package com.pressure.core.bean;

import com.util.SimpleHttpClient;
import org.apache.http.Header;

import java.util.Arrays;
import java.util.Map;

/**
 * 请求日志
 * @author YL
 **/
public class HttpLog {

    private final String url;

    private final int httpState;

    private final String responseBody;

    private final Map<String, String> params;

    private final Header[] requestHeaders;

    private final Header[] responseHeaders;

    public HttpLog(String url, Map<String, String> params, Header[] requestHeaders, SimpleHttpClient.Info info) {
        this.url = url;
        this.params = params;
        this.requestHeaders = requestHeaders;

        if (info != null) {
            this.httpState = info.getCode();
            this.responseHeaders = info.getHeads();
            this.responseBody = info.getBody();
        } else {
            this.httpState = 0;
            this.responseHeaders = null;
            this.responseBody = null;
        }
    }

    @Override
    public String toString() {
        return "HttpLog{" +
                "url='" + url + '\'' +
                ", httpState=" + httpState +
                ", params=" + params +
                "\n, requestHeaders=" + Arrays.toString(requestHeaders) +
                "\n, responseHeaders=" + Arrays.toString(responseHeaders) +
                "\n, responseBody='" + responseBody + '\'' +
                "}\n";
    }

    public String getUrl() {
        return url;
    }

    public int getHttpState() {
        return httpState;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Header[] getRequestHeaders() {
        return requestHeaders;
    }

    public Header[] getResponseHeaders() {
        return responseHeaders;
    }
}
