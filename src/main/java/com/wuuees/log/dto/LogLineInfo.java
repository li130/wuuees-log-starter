package com.wuuees.log.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
public class LogLineInfo {

    private LocalDateTime timestamp;

    private String level;

    private String content;

    private String originalLine;


    public boolean matchesFilter(LogQueryDto.LogQueryRequest req) {
        // 时间范围过滤
        if (timestamp != null) {
            if (req.getStartTime() != null && timestamp.isBefore(req.getStartTime())) {
                return false;
            }
            if (req.getEndTime() != null && timestamp.isAfter(req.getEndTime())) {
                return false;
            }
        }

        // 日志级别过滤
        if (StringUtils.isNoneBlank(req.getLevel()) && !StringUtils.equalsIgnoreCase(level, req.getLevel())) {
            return false;
        }

        // 关键字过滤
        if (StringUtils.isNoneBlank(req.getKeyword()) && !StringUtils.containsIgnoreCase(originalLine, req.getKeyword())) {
            return false;
        }
        return true;
    }
}
