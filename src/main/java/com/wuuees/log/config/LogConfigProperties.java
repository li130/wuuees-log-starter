package com.wuuees.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "wuuees.log.viewer")
@Data
public class LogConfigProperties {

    /**
     * 日志文件根路径
     */
    private String logPath = "./logs";

    /**
     * 允许访问的日志文件扩展名
     */
    private List<String> allowedExtensions = Arrays.asList(".log", ".txt");


    /**
     * 单次查询的最大行数
     */
    private int maxLines = 1000;


    /**
     * 文件最大大小（MB）
     */
    private long maxFileSize = 100;


    /**
     * 是否启用安全检查
     */
    private boolean enableSecurity = true;
}
