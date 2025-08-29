package com.wuuees.log.util;


import com.wuuees.log.dto.LogLineInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@Slf4j
public class LogParser {

    private static final Pattern LOG_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+(.*)");


    /**
     * 解析日志行，提取时间和级别
     */
    public LogLineInfo parseLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String level = matcher.group(2);
            String content = matcher.group(3);

            LocalDateTime dateTime = parseTimestamp(timestamp);
            return new LogLineInfo(dateTime, level, content, line);
        }

        // 不匹配标准格式，返回原始行
        return new LogLineInfo(null, null, null, null);
    }


    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            return LocalDateTime.parse(timestamp, formatter);
        } catch (Exception ex) {
            return null;
        }
    }
}
