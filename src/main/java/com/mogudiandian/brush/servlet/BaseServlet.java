package com.mogudiandian.brush.servlet;

import com.alibaba.fastjson.JSON;
import com.mogudiandian.brush.configuration.BrushProperties;
import com.mogudiandian.brush.dialect.Dialect;
import com.mogudiandian.brush.dialect.MySQLDialect;
import com.mogudiandian.brush.model.DataSource;
import com.mogudiandian.brush.model.Result;
import com.mogudiandian.brush.model.Table;
import com.mogudiandian.brush.util.DBUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = -1L;

    protected Dialect dialect = new MySQLDialect();

    protected String encoding = "UTF-8";

    protected boolean useCache = false;

    protected String tpl;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String tplResourcePath = getTplResourcePath();

        if (tplResourcePath != null) {
            StringBuilder buffer = new StringBuilder();

            try (InputStream is = this.getClass()
                                      .getClassLoader()
                                      .getResourceAsStream(tplResourcePath);
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                for (String temp = null; (temp = br.readLine()) != null; ) {
                    buffer.append(temp);
                    buffer.append("\r\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            tpl = buffer.toString();
        }
    }

    public abstract String getTplResourcePath();

    @Override
    public void destroy() {
        super.destroy();
    }

    protected void outputJson(HttpServletResponse response, Result result) {
        if (!result.success) {
            response.setStatus(600);
        }
        response.setContentType("text/plain");
        try {
            PrintWriter out = response.getWriter();
            String json = JSON.toJSONStringWithDateFormat(result, "yyyy-MM-dd HH:mm:ss");
            json = json.replaceAll("<", "&lt").replaceAll(">", "&gt;");
            out.print(json);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void outputJson(HttpServletResponse response, Object obj, boolean success) {
        if (!success) {
            response.setStatus(600);
        }
        response.setContentType("text/plain");
        try {
            PrintWriter out = response.getWriter();
            out.print(JSON.toJSONStringWithDateFormat(obj, "yyyy-MM-dd HH:mm:ss"));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected List<String> getDBs(HttpServletRequest request) {
        ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        BrushProperties properties = applicationContext.getBean(BrushProperties.class);
        if (CollectionUtils.isNotEmpty(properties.getDataSources())) {
            return properties.getDataSources().stream().map(DataSource::getName).collect(Collectors.toList());
        }
        return properties.getSchemaNames();
    }

    protected Connection getConnection(String db, HttpServletRequest request) {
        ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        BrushProperties properties = applicationContext.getBean(BrushProperties.class);
        if (CollectionUtils.isNotEmpty(properties.getDataSources())) {
            DataSource dataSource = properties.getDataSourceMap().get(db);
            return dialect.newConnection(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource.getPassword());
        }
        javax.sql.DataSource dataSourceBean = applicationContext.getBean(javax.sql.DataSource.class);
        try {
            return dataSourceBean.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getSchema(String db, HttpServletRequest request) {
        ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        BrushProperties properties = applicationContext.getBean(BrushProperties.class);
        if (CollectionUtils.isNotEmpty(properties.getDataSources())) {
            DataSource dataSource = properties.getDataSourceMap().get(db);
            String url = dataSource.getJdbcUrl();
            int index = url.indexOf('?');
            if (index > 0) {
                url = url.substring(0, index);
            }

            return url.substring(url.lastIndexOf('/') + 1);
        }
        return db;
    }

    protected Table getTable(String db, String tableName, HttpServletRequest request) {
        Connection conn = getConnection(db, request);
        Table table = DBUtils.getTable(dialect, conn, getSchema(db, request), tableName, useCache);
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return table;
    }

    protected String convertType(String dbType) {
        String xtype = "textfield";

        String upper = dbType.toUpperCase();

        Set<String> numberSet = new HashSet<>();
        numberSet.add("TINYINT");
        numberSet.add("INT");
        numberSet.add("BIGINT");
        numberSet.add("INTEGER");
        numberSet.add("LONG");
        numberSet.add("FLOAT");
        numberSet.add("DOUBLE");
        numberSet.add("DECIMAL");
        numberSet.add("NUMERIC");

        if (numberSet.contains(upper)) {
            xtype = "numberfield";
        } else if ("BIT".equals(upper) || "BOOL".equals(upper) || "BOOLEAN".equals(upper)) {
            xtype = "radiogroup";
        } else if ("DATE".equals(upper)) {
            xtype = "datefield";
        } else if ("DATETIME".equals(upper) || "TIME".equals(upper) || "TIMESTAMP".equals(upper)) {
            xtype = "datetimefield";
        } else if ("ENUM".equals(upper)) {
            xtype = "combo";
        }
        return xtype;
    }

}
