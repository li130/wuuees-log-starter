package com.wuuees.log.controller;

import com.wuuees.log.service.LogMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
@Slf4j
public class WebSocketController {

    @Autowired
    private LogMonitorService logMonitorService;
    
    // 用于防止重复调用stopMonitoring的标志
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    /**
     * 处理开始监控的请求
     * @param message 包含文件名的消息
     */
    @MessageMapping("/start-monitoring")
    @SendTo("/topic/log-monitor")
    public Map<String, Object> startMonitoring(@Payload Map<String, String> message) {
        String fileName = message.get("fileName");
        if (fileName != null && !fileName.isEmpty()) {
            // 重置stopping标志
            stopping.set(false);
            logMonitorService.startMonitoring(fileName);
            return Map.of(
                    "type", "monitoring_started",
                    "message", "开始监控文件: " + fileName,
                    "fileName", fileName
            );
        } else {
            return Map.of(
                    "type", "error",
                    "message", "文件名不能为空"
            );
        }
    }

    /**
     * 处理停止监控的请求
     */
    @MessageMapping("/stop-monitoring")
    @SendTo("/topic/log-monitor")
    public Map<String, Object> stopMonitoring() {
        // 使用AtomicBoolean防止重复调用
        if (stopping.compareAndSet(false, true)) {
            logMonitorService.stopMonitoring();
            return Map.of(
                    "type", "monitoring_stopped",
                    "message", "已停止监控"
            );
        } else {
            // 如果已经在停止过程中，返回空消息或特殊消息
            return Map.of(
                    "type", "monitoring_already_stopped",
                    "message", "监控已处于停止状态"
            );
        }
    }
}