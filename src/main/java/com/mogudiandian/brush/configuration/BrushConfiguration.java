package com.mogudiandian.brush.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 配置
 */
@Configuration
@ServletComponentScan(basePackages = "com.mogudiandian.brush.servlet")
@EnableConfigurationProperties(BrushProperties.class)
public class BrushConfiguration {

    @Autowired
    private BrushProperties brushProperties;

}
