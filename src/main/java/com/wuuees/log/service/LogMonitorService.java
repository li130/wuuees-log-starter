package com.wuuees.log.service;

import com.wuuees.log.config.LogConfigProperties;
import com.wuuees.log.dto.LogLineInfo;
import com.wuuees.log.util.LogParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Slf4j
public class LogMonitorService implements InitializingBean, DisposableBean {

    @Autowired
    private LogConfigProperties logConfig;

    @Autowired
    private LogParser logParser;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    // 文件监控服务
    private WatchService watchService;

    // 线程池
    private ScheduledExecutorService executorService;

    // 存储每个文件的读取位置
    private final Map<String, Long> filePositions = new ConcurrentHashMap<>();

    // 当前监控的文件
    private volatile String currentMonitorFile;

    // 监控状态
    private volatile boolean monitoring = false;

    // 用于防止重复调用stopMonitoring的标志
    private final AtomicBoolean stopping = new AtomicBoolean(false);


    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            executorService = Executors.newScheduledThreadPool(2);

            // 注册日志目录监控
            Path logPath = Paths.get(logConfig.getLogPath());
            if (Files.exists(logPath)) {
                logPath.register(watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);

                // 启动文件监控线程
                executorService.submit(this::watchFiles);
                log.info("日志文件监控服务已启动，监控目录: {}", logPath);
            }
        } catch (Exception e) {
            log.error("初始化日志文件监控服务失败", e);
        }
    }

    @Override
    public void destroy() {
        monitoring = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception e) {
                log.error("关闭文件监控服务失败", e);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("日志监控服务已关闭");
    }

    /**
     * 文件监控线程
     */
    private void watchFiles() {
        while (!Thread.currentThread().isInterrupted() && watchService != null) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    if (monitoring && fileName.toString().equals(currentMonitorFile)) {
                        // 减少延迟，立即处理文件变更
                        processFileChange(currentMonitorFile);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException ex) {
                // 当watchService被关闭时，直接退出循环，防止 watchService.take()时出现空指针
                log.debug("WatchService已关闭，停止监控");
                break;
            } catch (Exception e) {
                log.error("文件监控异常", e);
            }
        }
    }

    /**
     * 处理文件的变化
     *
     * @param fileName 文件名
     */
    private void processFileChange(String fileName) {
        // 添加监控状态检查
        if (!monitoring) {
            return;
        }
        try {
            File file = new File(logConfig.getLogPath(), fileName);
            if (!file.exists()) {
                return;
            }

            long currentLength = file.length();
            Long lastPosition = filePositions.getOrDefault(fileName, 0L);

            // 如果日志文件被截断（日志轮转），重置位置
            if (currentLength < lastPosition) {
                lastPosition = 0L;
            }

            // 如果有新内容
            if (currentLength > lastPosition) {
                String newContent = readNewContent(file, lastPosition, currentLength);
                if (newContent != null && !newContent.trim().isEmpty()) {
                    // 解析新日志行
                    String[] lines = newContent.split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            // 发送日志
                            sendLogLine(fileName, line);
                        }
                    }
                }
                // 更新文件位置
                filePositions.put(fileName, currentLength);
            }
        } catch (Exception e) {
            log.error("处理文件变化失败: {}", fileName, e);
        }
    }


    /**
     * 读取文件新增内容
     *
     * @param file          文件
     * @param startPosition 开始位置
     * @param endPosition   结束位置
     * @return 新增内容
     */
    private String readNewContent(File file, long startPosition, long endPosition) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(startPosition);

            long length = endPosition - startPosition;

            if (length > 1024 * 1024) {
                // 限制单次读取大小为1MB
                length = 1024 * 1024;
            }

            byte[] buffer = new byte[(int) length];
            int bytesRead = raf.read(buffer);

            if (bytesRead > 0) {
                return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("读取文件新增内容失败:{}", file.getName(), e);
        }
        return null;
    }

    /**
     * 发送日志行到WebSocket客户端
     *
     * @param fileName 文件名
     * @param logLine  日志行
     */
    private void sendLogLine(String fileName, String logLine) {
        try {
            // 解析日志行
            LogLineInfo lineInfo = logParser.parseLine(logLine);

            // 构建消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "new_log_line");
            message.put("fileName", fileName);
            message.put("content", logLine);
            message.put("timestamp", lineInfo.getTimestamp() != null ?
                    lineInfo.getTimestamp().toString() : "");
            message.put("level", lineInfo.getLevel() != null ?
                    lineInfo.getLevel() : "");
            message.put("rawContent", lineInfo.getContent() != null ?
                    lineInfo.getContent() : logLine);

            // 发送到WebSocket客户端
            messagingTemplate.convertAndSend("/topic/log-monitor", message);
        } catch (Exception e) {
            log.error("发送日志行失败", e);
        }
    }


    /**
     * 开始监控指定文件
     * @param fileName 文件名
     */
    public void startMonitoring(String fileName) {
        this.currentMonitorFile = fileName;
        this.monitoring = true;
        // 初始化文件位置，从文件末尾开始监控
        File file = new File(logConfig.getLogPath(), fileName);
        if (file.exists()) {
            this.filePositions.put(fileName, file.length());
        }
        // 重置stopping标志
        this.stopping.set(false);
        log.info("开始实时监控文件: {}", fileName);
    }

    /**
     * 停止监控
     */
    public void stopMonitoring() {
        // 使用AtomicBoolean防止重复调用
        if (stopping.compareAndSet(false, true)) {
            this.monitoring = false;
            this.currentMonitorFile = null;
            log.info("停止实时文件监控");

            // 发送监控停止消息
            messagingTemplate.convertAndSend("/topic/log-monitor",
                    Map.of("type", "monitor_stopped"));
        }
    }
}