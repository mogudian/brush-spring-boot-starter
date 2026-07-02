## 使用说明

- 1、引用项目

```xml
<dependency>
    <groupId>com.mogudiandian</groupId>
    <artifactId>brush-spring-boot-starter</artifactId>
    <version>[3.0.0,4.0.0)</version>
</dependency>
```

- 2、加入配置
```properties
brush.schema-names=库名
```
```yaml
brush:
  schema-names:
    - 库名1
    - 库名2
```

- 3、加入SSO过滤

```properties
# 国内
joymo.sso.ignore-pattern=/tree,/grid,/editgrid,/db,/stmt,/icons/**,/edit_area/**

# 海外
cotti.uac.password-mode.ignore-pattern=/tree,/grid,/editgrid,/db,/stmt,/icons/**,/edit_area/**
```

- 4、如果使用了 LoginInterceptor 需要改造
```java
// 国内
...
LoginUserBO userInfo = AuthUserHolder.getUserInfo();
// 需要加这个判断，避免某些不需要登录的资源被拦截抛NPE
if (userInfo != null) {
    ...
}
return true;
...


// 海外
...
User loginUser = AuthenticationHolder.getLoginUser();
// 需要加这个判断，避免某些不需要登录的资源被拦截抛NPE
if (loginUser != null) {
    ...
}
return true;
...
```

- 5、如果继承了 WebMvcConfigurationSupport 或 I18nWebMvcConfigurationSupport 需要改造
```java
@Configuration
public class Xxx extends I18nWebMvcConfigurationSupport {

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/",
            "classpath:/resources/",
            "classpath:/static/",
            "classpath:/public/"};
    
    // 需要实现这个方法实现静态文件访问
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
    }
}
```

- 6、如果使用了 Spring Security 需要改造
```java
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests()
                    .antMatchers("/tree", "/grid", "/editgrid", "/db", "/stmt", "/icons/**", "/edit_area/**")
                    .permitAll();
    }
}
```

-7、新建备份表用于自动备份数据
```sql
CREATE TABLE `t_backup_table`
(
    `id`            bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `database_name` varchar(64) DEFAULT NULL COMMENT '数据库名称',
    `table_name`    varchar(64) DEFAULT NULL COMMENT '表名称',
    `table_id`      varchar(64) DEFAULT NULL COMMENT '表ID',
    `table_data`    longtext COMMENT '表数据',
    `create_time`   datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time`   datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_table_id` (`table_id`),
    KEY `idx_table_name_id` (`table_name`, `table_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='备份表';
```