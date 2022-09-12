package cn.ricardo.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wcp
 * @since 2022/9/9
 */
@ConfigurationProperties(prefix = "canal")
public class CanalProperties {

    /**
     * canal-server地址
     */
    private String host;

    /**
     * canal-server端口
     */
    private Integer port;

    /**
     * 描述
     */
    private String destination = "example";

    /**
     * 账户，如果不设置，默认为""
     */
    private String username = "";

    /**
     * 密码，如果不设置，默认为""
     */
    private String password = "";

    /**
     * 过滤(格式 {database}.{table})
     */
    private String filter = ".*\\..*";

    /**
     * 获取数据条数
     */
    private Integer batchSize = 1000;

    /**
     * 获取不到数据时的休眠时间
     */
    private Long sleepMillis = 1000L;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Long getSleepMillis() {
        return sleepMillis;
    }

    public void setSleepMillis(Long sleepMillis) {
        this.sleepMillis = sleepMillis;
    }
}
