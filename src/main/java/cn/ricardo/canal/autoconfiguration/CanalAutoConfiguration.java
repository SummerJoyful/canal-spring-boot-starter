package cn.ricardo.canal.autoconfiguration;

import cn.ricardo.canal.event.SpringListenerEvent;
import cn.ricardo.canal.handler.CanalMonitorCollectHandler;
import cn.ricardo.canal.handler.MessageHandler;
import cn.ricardo.canal.properties.CanalProperties;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.InetSocketAddress;

/**
 * @author wcp
 * @since 2022/9/9
 */
@Configuration
// 判断有无引入原生Canal包
@ConditionalOnClass(CanalConnector.class)
// 判断 @EnableCanalClient 注解有无启用
@ConditionalOnBean(CanalMonitorCollectHandler.class)
// 判断配置文件内有无 canal.host及port，有才创建此bean
@ConditionalOnProperty(prefix = "canal", name = {"host", "port"})
// 启用该配置文件
@EnableConfigurationProperties(CanalProperties.class)
@Import({SpringListenerEvent.class, MessageHandler.class})
public class CanalAutoConfiguration {

    private final CanalProperties canalProperties;

    public CanalAutoConfiguration(CanalProperties canalProperties) {
        this.canalProperties = canalProperties;
    }

    /**
     * 创建canal连接客户端
     *
     * @return canal连接客户端
     */
    @Bean("canalConnector")
    public CanalConnector canalConnector() {
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(canalProperties.getHost(), canalProperties.getPort()),
                canalProperties.getDestination(), canalProperties.getUsername(), canalProperties.getPassword());
        canalConnector.connect();
        canalConnector.subscribe(canalProperties.getFilter());
        canalConnector.rollback();
        return canalConnector;
    }

}
