package com.wuuees.log.controller;

import com.wuuees.log.dto.LogQueryDto;
import com.wuuees.log.service.LogService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogController {

    @Autowired
    private LogService logService;

    /**
     * 获取日志文件列表
     */
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> getLogFiles() {
        try {
            List<Map<String, Object>> files = logService.getLogFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("获取日志文件列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 查询日志内容
     */
    @PostMapping("/query")
    public ResponseEntity<LogQueryDto.LogQueryResponse> queryLogs(@Valid @RequestBody LogQueryDto.LogQueryRequest request) {
        try {
            LogQueryDto.LogQueryResponse response = logService.queryLogs(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("查询参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("查询日志失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载日志文件
     */
    @GetMapping("/download/{fileName}")
    public void downloadLog(
            @PathVariable String fileName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) {

        try {
            LogQueryDto.LogQueryRequest request = new LogQueryDto.LogQueryRequest();
            request.setFileName(fileName);
            request.setKeyword(keyword);
            request.setLevel(level);
            request.setStartTime(startTime);
            request.setEndTime(endTime);

            logService.downloadLog(fileName, request, response);

        } catch (IllegalArgumentException e) {
            log.warn("下载参数错误: {}", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } catch (Exception e) {
            log.error("下载日志失败", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    /**
     * 模拟日志产生
     */
    @GetMapping("/mock")
    public void mockLog() {
        for (int i = 0; i < 20; i++) {
            log.error("This is a mock error log line--->{},date--->{}", i, new Date());

            log.info("This is a mock info log line--->{},date--->{}", i, new Date());
        }
    }
}
