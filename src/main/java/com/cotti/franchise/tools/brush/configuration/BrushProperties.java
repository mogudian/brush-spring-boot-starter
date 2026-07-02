package com.cotti.franchise.tools.brush.configuration;

import com.cotti.franchise.tools.brush.model.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 配置项
 */
@ConfigurationProperties(prefix = "brush")
public class BrushProperties {

    /**
     * 查询最大数量 默认1000
     */
    private int queryLimit = 1000;

    private List<DataSource> dataSources;

    private Map<String, DataSource> dataSourceMap;

    private List<String> schemaNames;

    public int getQueryLimit() {
        return queryLimit;
    }

    public void setQueryLimit(int queryLimit) {
        this.queryLimit = queryLimit;
    }

    public List<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DataSource> dataSources) {
        this.dataSources = dataSources;
        if (dataSources != null) {
            this.dataSourceMap = dataSources.stream().collect(Collectors.toMap(DataSource::getName, Function.identity()));
        }
    }

    public Map<String, DataSource> getDataSourceMap() {
        return dataSourceMap;
    }

    public List<String> getSchemaNames() {
        return schemaNames;
    }

    public void setSchemaNames(List<String> schemaNames) {
        this.schemaNames = schemaNames;
    }
}
