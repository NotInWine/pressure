package com.pressure.core.httputil.impl;

import com.pressure.core.bean.ResultInfo;
import com.pressure.core.httputil.ResultInfoMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * 日志监听器，将请求结果记录到
 * @see LogResultInfoMonitor#bufferedWriter 对应的文件中
 * @author YL
 **/
public class LogResultInfoMonitor implements ResultInfoMonitor<String> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<ResultInfo> logs = new LinkedList<>();
    private final BufferedWriter bufferedWriter;

    public LogResultInfoMonitor(File logFile) {
        if (!logFile.exists()) {
            try {
                boolean newFile = logFile.createNewFile();
                if (!newFile) {
                    logger.warn("文件创建失败 {}", logFile.toString());
                }
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        try {
            this.bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            logger.error("文件流获取失败", e);
            throw new RuntimeException("文件流获取失败", e);
        }
    }

    @Override
    public void add(ResultInfo resultInfo) {
        synchronized (logs) {
            logs.add(resultInfo);
        }
    }

    @Override
    public String out() {
        if (logs.size() == 0) {
            return null;
        }
        List<ResultInfo> copy;
        synchronized (logs) {
            copy = new ArrayList<>(logs);
            logs.clear();
        }
        copy.sort(Comparator.comparing(ResultInfo::getBeginTime));
        copy.forEach(i -> {
            String str = i.toString();
            try {
                bufferedWriter.write(str, 0, str.length());
                bufferedWriter.newLine();
            } catch (IOException e) {
                logger.error("", e);
            }
        });

        return null;
    }

    @Override
    public void printOut(boolean endNotice) {
        this.out();
        if (endNotice) {
            try {
                bufferedWriter.flush();
            } catch (IOException e) {
                logger.error("", e);
            }
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
    }
}
