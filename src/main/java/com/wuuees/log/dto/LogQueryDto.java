package com.wuuees.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class LogQueryDto {

    @Data
    public static class LogQueryRequest {

        @NotBlank(message = "文件名称不能为空")
        private String fileName;

        /**
         * 页面，从1开始
         */
        @Min(value = 1, message = "页码必须大于0")
        private int page = 1;

        /**
         * 每页行数
         */
        @Min(value = 1, message = "每页行数必须要大于1")
        @Max(value = 1000, message = "每页行数不能超过1000")
        private int pageSize = 1000;


        /**
         * 搜索关键字
         */
        private String keyword;

        /**
         * 日志过滤级别
         */
        private String level;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;


        /**
         * 结束时间
         */
        private LocalDateTime endTime;

        /**
         * 是否倒叙
         */
        private boolean reverse = true;
    }



    @Data
    public static class LogQueryResponse {

        /**
         * 日志内容列表
         */
        private List<String> lines;

        /**
         * 总行数
         */
        private long totalLines;

        /**
         * 当前页码
         */
        private int currentPage;

        /**
         * 总页数
         */
        private int totalPages;

        /**
         * 文件大小（字节）
         */
        private Long fileSize;

        /**
         * 最后修改时间
         */
        private LocalDateTime lastModified;

    }

}
