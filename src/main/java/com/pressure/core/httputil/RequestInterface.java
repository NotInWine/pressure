package com.pressure.core.httputil;

import com.pressure.core.bean.RequestInfo;


/**
 * 发送请求的接口
 * 处理器
 * @author YL
 */
public interface RequestInterface {

    /**
     * 发送测试脚本
     * @param infos      指令集 按顺序执行
     */
    void send(RequestInfo... infos);
}
