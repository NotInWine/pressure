package com.util;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author YL
 */
public class SimpleHttpClient {

    private final static Logger log = LoggerFactory.getLogger(SimpleHttpClient.class);
    private HttpContext context;
    private volatile Header[] responseHeaders;
    private volatile Info info;
    /** 设置连接超时时间，单位毫秒。*/
    private final int CONNECT_TIMEOUT;
    /** 请求获取数据的超时时间，单位毫秒 */
    private final int SOCKET_TIMEOUT;
    private final static int REQUEST_RETRY_CNT = 1;
    private final static int MAX_TOTAL = 1000;
    private final static int MAX_PER_ROUTE = 1000;
    private final static PoolingHttpClientConnectionManager POOL;

    /** 请求重试处理 */
    private final static HttpRequestRetryHandler HTTP_REQUEST_RETRY_HANDLER = (exception, executionCount, context) -> {
        if (executionCount >= REQUEST_RETRY_CNT) {// 如果已经重试了5次，就放弃
            return false;
        }
        if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
            return true;
        }
        if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
            return false;
        }
        if (exception instanceof InterruptedIOException) {// 超时
            return false;
        }
        if (exception instanceof UnknownHostException) {// 目标服务器不可达
            return false;
        }
        if (exception instanceof SSLException) {// SSL握手异常
            return false;
        }

        HttpClientContext clientContext = HttpClientContext
                .adapt(context);
        HttpRequest request = clientContext.getRequest();
        // 如果请求是幂等的，就再次尝试
        return !(request instanceof HttpEntityEnclosingRequest);
    };

    static {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
                .getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory
                .getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("http", plainsf)
                .register("https", sslsf).build();
        POOL = new PoolingHttpClientConnectionManager(registry);
        // 将最大连接数增加
        POOL.setMaxTotal(MAX_TOTAL);
        // 将每个路由基础的连接增加
        POOL.setDefaultMaxPerRoute(MAX_PER_ROUTE);
        // 创建线程异步维护链接状态（清理超时链接）
        new HttpClientConnectionMonitorThread(POOL);
    }

    public SimpleHttpClient(int connectTimeout, int socketTimeout) {
        CONNECT_TIMEOUT = connectTimeout;
        SOCKET_TIMEOUT = socketTimeout;
        context = new BasicHttpContext();
    }

    public SimpleHttpClient() {
        CONNECT_TIMEOUT = 1000;
        SOCKET_TIMEOUT = 3 * 1000;
        context = new BasicHttpContext();
    }

    public static SimpleHttpClient get() {
        return new SimpleHttpClient();
    }

    private List<BasicNameValuePair> toNameValuePairList(Map<String, String> m) {
        List<BasicNameValuePair> nameValuePairList = new ArrayList<>();
        if (m == null) {
            return nameValuePairList;
        }
        List<String> keylist = MapUtil.keyList(m);
        for (String key : keylist) {
            nameValuePairList.add(new BasicNameValuePair(key, m.get(key)));
        }
        return nameValuePairList;
    }

    public String get(String url, Header... headers) throws IOException {
        HttpGet httpget = new HttpGet(url);
        setHeadGet(httpget, headers);
        return getString(httpget);
    }

    private void setHeadGet(HttpGet httpget, Header[] headers) {
        if (headers != null) {
            for (Header header : headers) {
                httpget.setHeader(header);
            }
        }
    }

    public String get(String url, Map<String, String> params) throws IOException {
        url = !url.contains("?") ?
                url + "?" + SignUtil.createLinkString(params)
                : url + "&" + SignUtil.createLinkString(params);
        return get(url);
    }

    public String post(String url) throws IOException {
        HttpPost httppost = new HttpPost(url);
        return getString(httppost, null);
    }

    public String post(String url, Header[] headers) throws IOException {
        HttpPost httppost = new HttpPost(url);
        setHead(httppost, headers);
        return getString(httppost, null);
    }

    public String post(String url, Map<String, String> params) throws IOException {
        HttpPost httppost = new HttpPost(url);
        List<BasicNameValuePair> nameValuePairList = toNameValuePairList(params);
        return getString(httppost, new UrlEncodedFormEntity(nameValuePairList, "UTF-8"));
    }

    /**
     * 提交 json数据
     *
     * @param url
     * @param json
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String postJson(String url, String json, Header... headers) throws ClientProtocolException, IOException {
        return post(url, json, "json", headers);
    }

    /**
     * 提交 xml 数据
     *
     * @param url
     * @param xml
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String postXml(String url, String xml, Header... headers) throws ClientProtocolException, IOException {
        return post(url, xml, "xml", headers);
    }

    private String post(String url, String str, String type, Header... headers) throws IOException {
        HttpPost httppost = new HttpPost(url);
        setHead(httppost, headers);
        StringEntity se = new StringEntity(str, Charset.forName("UTF-8"));
        se.setContentType("text/" + type);
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/" + type));
        return getString(httppost, se);
    }

    public String post(String url, Map<String, String> params, Header... headers)
            throws IOException {
        if (params == null) {
            return post(url, headers);
        }
        HttpPost httppost = new HttpPost(url);
        setHead(httppost, headers);
        List<BasicNameValuePair> nameValuePairList = toNameValuePairList(params);
        return getString(httppost, new UrlEncodedFormEntity(nameValuePairList, "UTF-8"));
    }

    private void setHead(HttpPost httppost, Header[] headers) {
        if (headers != null) {
            for (Header header : headers) {
                httppost.setHeader(header);
            }
        }
    }

    /**
     * Post 上传文件
     *
     * @param url
     * @param params
     * @param fileList
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String post(String url, Map<String, String> params, Map<String, File> fileList)
            throws ClientProtocolException, IOException {
        HttpPost httppost = new HttpPost(url);

        MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
        List<String> fileKey = MapUtil.keyList(fileList);
        for (String fk : fileKey) {
            FileBody file1 = new FileBody(fileList.get(fk));
            reqEntity.addPart(fk, file1);
        }

        List<String> paramsKey = MapUtil.keyList(params);
        for (String pk : paramsKey) {
            StringBody name = new StringBody(params.get(pk), ContentType.MULTIPART_FORM_DATA);
            reqEntity.addPart(pk, name);
        }

        return getString(httppost, reqEntity.build());
    }

    private String getString(HttpPost httppost, HttpEntity httpEntity) throws IOException {
        if (httpEntity != null) {
            httppost.setEntity(httpEntity);
        }
        return getString(httppost);
    }

    private String getString(HttpRequestBase http) throws IOException {
        CloseableHttpResponse response = getHttpClient().execute(http, context);
        responseHeaders = response.getAllHeaders();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            info = new Info(response.getStatusLine().getStatusCode(), responseHeaders);
        }
        String result = EntityUtils.toString(response.getEntity());

        info = new Info(result, response.getStatusLine().getStatusCode(), responseHeaders);

        response.close();
        return result;
    }

    private Info get(HttpRequestBase http) throws IOException {
        CloseableHttpResponse response = getHttpClient().execute(http, context);
        responseHeaders = response.getAllHeaders();
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            return new Info(statusCode, responseHeaders);
        }
        String result = EntityUtils.toString(response.getEntity());
        response.close();
        return new Info(result, statusCode, responseHeaders);
    }

    /**
     * 禁止重复调用
     * @return
     */
    public Info getResponseInfo() {
        return info;
    }

    /**
     * @author xiangqi
     * @date 2018-01-29 下午 2:10
     */
    private static class HttpClientConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connManager;
        private volatile boolean shutdown = false;

        HttpClientConnectionMonitorThread(HttpClientConnectionManager connManager) {
            super();
            this.setName("http-connection-monitor");
            this.setDaemon(true);
            this.connManager = connManager;
            this.start();
        }

        @Override
        public void run() {
            System.out.println("清理过期http链接 open：" + !shutdown);
            while (!shutdown) {
                synchronized (this) {
                    try {
                        // 等待
                        wait(10000);
                        // 关闭过期的链接
                        connManager.closeExpiredConnections();
                        // 选择关闭 空闲30秒的链接
                        connManager.closeIdleConnections(30, TimeUnit.SECONDS);
                    } catch (Throwable e) {
                        log.error("http 连接池清理任务抛出异常", e);
                    }
                }
            }
        }

        public boolean isShutdown() {
            return shutdown;
        }
    }


    /**
     * 获取HttpClient对象
     * @author YL
     */
    private CloseableHttpClient getHttpClient() {
        Builder bu = RequestConfig.custom();
        bu.setConnectTimeout(CONNECT_TIMEOUT);
        bu.setSocketTimeout(SOCKET_TIMEOUT);
        return HttpClients.custom()
                .setConnectionManager(POOL)
                .setDefaultRequestConfig(bu.build())
                .setRetryHandler(HTTP_REQUEST_RETRY_HANDLER).build();
    }

    public static void main(String[] args) throws IOException {
        String url = "https://www.xxxxxx.com/api/video/pay/start?start=1&count=15&keyWord=";

        SimpleHttpClient sc = new SimpleHttpClient();
        System.out.println(sc.get(url));
        System.out.println(sc.get("https://www.xxxxxx.com/api/video/playDetail?videoId=520&token="));
        System.out.println(sc.get("https://www.xxxxxx.com/api/video/getVideList?recordId=520&type=video"));
        System.out.println(sc.get("https://www.xxxxxx.com/api/video/playDetail?videoId=462&token="));
    }

    public static class Info {

        private final String body;
        private final int code;
        private final Header[] heads;

        public Info(int code, Header[] headers) {
            this.body = null;
            this.heads = initHeads(headers);
            this.code = code;
        }

        private Header[] initHeads(Header[] headers) {
           return headers;
        }

        public Info(String body, int code, Header[] headers) {
            this.body = body;
            this.code = code;
            this.heads = initHeads(headers);
        }

        public int getCode() {
            return code;
        }

        public String getBody() {
            return body;
        }

        public Header[] getHeads() {
            return heads;
        }

        @Override
        public String toString() {
            return "Info{" +
                    " code=" + code +
                    ", \n heads=" + heads +
                    '}';
        }
    }

}
