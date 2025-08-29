package com.wuuees.log.service;

import com.wuuees.log.config.LogConfigProperties;
import com.wuuees.log.util.LogParser;
import com.wuuees.log.dto.LogLineInfo;
import com.wuuees.log.dto.LogQueryDto;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LogService {

    @Autowired
    private LogConfigProperties logConfigProperties;

    @Autowired
    private LogParser logParser;

    /**
     * 获取日志文件列表
     */
    public List<Map<String, Object>> getLogFiles() {
        File logDir = new File(logConfigProperties.getLogPath());
        if (!logDir.exists() || !logDir.isDirectory()) {
            return Collections.emptyList();
        }

        return Arrays.stream(logDir.listFiles())
                .filter(this::isValidLogFile)
                .map(this::fileToMap)
                .sorted((a, b)
                        -> ((Long) b.get("lastModified")).compareTo((Long) a.get("lastModified")))
                .collect(Collectors.toList());
    }

    /**
     * 查询日志内容
     */
    public LogQueryDto.LogQueryResponse queryLogs(LogQueryDto.LogQueryRequest req) {
        File logFile = getLogFile(req.getFileName());
        validateFile(logFile);

        try {
            List<String> allLines = FileUtils.readLines(logFile, StandardCharsets.UTF_8);

            // 过滤日志行
            List<String> filteredLines = filterLines(allLines, req);

            // 倒序处理
            if (req.isReverse()) {
                Collections.reverse(filteredLines);
            }

            // 分页处理
            int totalLines = filteredLines.size();
            int totalPage = (int) Math.ceil((double) totalLines / req.getPageSize());
            int startIndex = (req.getPage() - 1) * req.getPageSize();
            int endIndex = Math.min(startIndex + req.getPageSize(), totalLines);

            List<String> pageLines = filteredLines.subList(startIndex, endIndex);

            LogQueryDto.LogQueryResponse respDto = new LogQueryDto.LogQueryResponse();
            respDto.setLines(pageLines);
            respDto.setTotalLines(totalLines);
            respDto.setCurrentPage(req.getPage());
            respDto.setTotalPages(totalPage);
            respDto.setFileSize(logFile.length());
            respDto.setLastModified(
                    LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(logFile.lastModified()),
                            ZoneId.systemDefault()));
            return respDto;
        } catch (Exception ex) {
            log.error("读取日志文件失败:{}", logFile.getAbsolutePath(), ex);
            throw new RuntimeException("读取日志文件失败", ex);
        }
    }


    /**
     * 下载日志文件
     */
    public void downloadLog(String fileName, LogQueryDto.LogQueryRequest req, HttpServletResponse resp) {
        File logFile = getLogFile(req.getFileName());
        validateFile(logFile);

        try {
            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            if (hasFilter(req)) {
                // 下载过滤后的内容
                List<String> allLines = FileUtils.readLines(logFile, StandardCharsets.UTF_8);
                List<String> filteredLines = filterLines(allLines, req);

                try (PrintWriter writer = resp.getWriter()) {
                    for (String line : filteredLines) {
                        writer.println(line);
                    }
                }
            } else {
                // 下载源文件
                resp.setContentLengthLong(logFile.length());
                try (FileInputStream inputStream = new FileInputStream(logFile);
                     ServletOutputStream outputStream = resp.getOutputStream()) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }

        } catch (Exception ex) {
            log.error("下载日志文件失败:{}", logFile.getAbsolutePath(), ex);
            throw new RuntimeException("下载日志文件失败", ex);
        }

    }

    private List<String> filterLines(List<String> lines, LogQueryDto.LogQueryRequest req) {
        if (!hasFilter(req)) {
            return lines;
        }

        return lines.stream()
                .map(logParser::parseLine)
                .filter(lineInfo -> lineInfo.matchesFilter(req))
                .map(LogLineInfo::getOriginalLine)
                .collect(Collectors.toList());
    }


    /**
     * 校验查询条件
     */
    private boolean hasFilter(LogQueryDto.LogQueryRequest req) {
        return StringUtils.isNoneBlank(req.getKeyword()) ||
                StringUtils.isNoneBlank(req.getLevel()) ||
                req.getStartTime() != null ||
                req.getEndTime() != null;
    }

    /**
     * 校验文件合法性
     */
    private void validateFile(File file) {
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        if (!file.isFile()) {
            throw new RuntimeException("不是有效的文件");
        }
        if (!isValidLogFile(file)) {
            throw new RuntimeException("不支持的文件类型");
        }

        long fileSizeMB = file.length() / (1024 * 1024);
        if (fileSizeMB > logConfigProperties.getMaxFileSize()) {
            throw new RuntimeException(String.format("文件过大，超过限制 %dMB", logConfigProperties.getMaxFileSize()));
        }
    }


    /**
     * 获取日志文件
     */
    private File getLogFile(String fileName) {
        // 安全检查：防止路径遍历攻击
        if (logConfigProperties.isEnableSecurity()) {
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new RuntimeException("非法文件名");
            }
        }
        return new File(logConfigProperties.getLogPath(), fileName);
    }

    /**
     * 判断文件后缀
     */
    private boolean isValidLogFile(File file) {
        String fileName = file.getName().toLowerCase();
        return logConfigProperties.getAllowedExtensions().stream().anyMatch(fileName::endsWith);
    }

    /**
     * 将文件转为map
     */
    private Map<String, Object> fileToMap(File file) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", file.getName());
        map.put("size", file.length());
        map.put("lastModified", file.lastModified());
        map.put("readable", file.canRead());
        return map;
    }
}
