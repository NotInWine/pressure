# pressure
一款简单的，基于java的，http 服务器**压力测试**工具
### 起因

- jmeter动态脚本编写比较复杂
- 希望压测程序可以在服务端执行，从内网环境发起请求
- 有足够的扩展空间,方便对复杂流程进行压测

### 依赖
- JDK 8 
- Maven

## 快速开始
### 构建
1. 执行git命令
    ```git
    git clone https://github.com/NotInWine/pressure.git
    ```
2. 导入你的idea
3. 使用 pom.xml 构建项目
    ```
    步骤略过
    ```

### [示例](./src/main/java/com/pressure/Main.java)
```java
package com.pressure;

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
                        new LogResultInfoMonitor(new File("D:\\big_folder\\log.txt")),
                        new SummaryMonitor()
                );

        // 配置请求脚本
        requestInterface.send(
                new RequestInfo(
                        "列表",
                        "https://www.xxxxxx.com/api/video/pay/start?start=1&count=15&keyWord=",
                        RequestInfo.RequestMethod.GET
                ),
                new RequestInfo(
                        "详情(包含动态参数)",
                        "https://www.xxxxxx.com/api/video/playDetail?videoId=${data.data.$2.id}&token=",
                        RequestInfo.RequestMethod.GET
                ),
                new RequestInfo(
                        "子列表(包含动态参数)",
                        "https://www.xxxxxx.com/api/video/getVideList?recordId=${data.videoId}&type=video",
                        RequestInfo.RequestMethod.GET
                ),
                new RequestInfo(
                        "子列表详情(包含动态参数)",
                        "https://www.xxxxxx.com/api/video/playDetail?videoId=${data.$1.id}&token=",
                        RequestInfo.RequestMethod.GET
                )
        );
    }
}
```

### 输出示例
- LogResultInfoMonitor 日志
```text
batchId=4752, name=子列表(包含动态参数), state=SUCCESS, httpLog=HttpLog{url='https://xkx.xxx.com/api/video/getVideList?recordId=245&type=video', httpState=200, params=null
, requestHeaders=null
, responseHeaders=[Server: nginx, Date: Sun, 19 Apr 2020 04:59:12 GMT, Content-Type: application/json; charset=utf-8, Transfer-Encoding: chunked, Connection: keep-alive, X-Powered-By: PHP/5.5.38, Access-Control-Allow-Origin: *]
, responseBody='{"code":1,"msg":"success","time":"1587272352","data":[{]}'}
, time=4260
```
- SummaryMonitor 汇总
```text
【子列表】
成功吞吐量(qps)=35.18,总吞吐量(qps)=38.55, 90%响应=12134, 95%响应=14489, 99%响应=18518, 最慢响应=24910, 最快响应=739, 平均响应=6653, 总请求次数=1272, 请求成功次数=1161, 请求异常次数=111, 异常率=0.0873
【列表】
成功吞吐量(qps)=73.64,总吞吐量(qps)=80.24, 90%响应=20970, 95%响应=24402, 99%响应=30721, 最慢响应=36134, 最快响应=752, 平均响应=12610, 总请求次数=2648, 请求成功次数=2430, 请求异常次数=218, 异常率=0.0823
【详情】
成功吞吐量(qps)=52.79,总吞吐量(qps)=59.24, 90%响应=12673, 95%响应=15239, 99%响应=22150, 最慢响应=27739, 最快响应=88, 平均响应=6699, 总请求次数=1955, 请求成功次数=1742, 请求异常次数=213, 异常率=0.1090
【子列表详情(包含动态参数)】
成功吞吐量(qps)=19.55,总吞吐量(qps)=21.03, 90%响应=10958, 95%响应=13584, 99%响应=17857, 最慢响应=20536, 最快响应=758, 平均响应=6349, 总请求次数=694, 请求成功次数=645, 请求异常次数=49, 异常率=0.0706
【汇总】
成功吞吐量(qps)=181.15,总吞吐量(qps)=199.06, 90%响应=17277, 95%响应=20627, 99%响应=27582, 最慢响应=36134, 最快响应=88, 平均响应=9036, 总请求次数=6569, 请求成功次数=5978, 请求异常次数=591, 异常率=0.0900
```
### 打包执行
需要打包到服务器执行可编辑[示例](./src/main/java/com/pressure/Main.java)，再使用mvn install, 打成可执行jar，到（服务端|PC）java -jar 执行


# 核心概念
- [控制器 RequestInterface](./src/main/java/com/pressure/core/httputil/RequestInterface.java)  
    负责压测请求核心逻辑，负责http连接配置，请求发送，触发统计（调用监听器）
- [请求命令包 RequestInfo](./src/main/java/com/pressure/core/bean/RequestInfo.java)  
    封装请求指令，作为参数传递给控制器执行。  
- [监听器 ResultInfoMonitor](./src/main/java/com/pressure/core/httputil/ResultInfoMonitor.java)  
    处理压测结果目前支持两款监听器，接收[响应日志](./src/main/java/com/pressure/core/bean/ResultInfo.java)
    - 日志
    - 汇总监听  
    作为扩展性最强的部分，可利用监听器自定义各种形式的统计输出

## 执行流程
[时序图](https://www.processon.com/view/link/5e9c0eb2f346fb4bdd771fd0)