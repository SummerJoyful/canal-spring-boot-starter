package cn.ricardo.canal.handler;

import cn.ricardo.canal.annotation.CanalMonitor;
import cn.ricardo.canal.callback.CanalEntityCallBackHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计所有声明监视器注解的类
 *
 * @author wcp
 * @since 2022/9/9
 */
public class CanalMonitorCollectHandler implements BeanPostProcessor {

    protected final ConcurrentHashMap<String, CanalEntityCallBackHandler> map = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        CanalMonitor canalMonitor = AnnotationUtils.findAnnotation(bean.getClass(), CanalMonitor.class);
        // 加了监听注解并且实现接口的类
        if (Objects.nonNull(canalMonitor) && bean instanceof CanalEntityHandler)
            collectInfo(canalMonitor, bean);

        return bean;
    }

    /**
     * 收集信息(所有Spring管理的类中实现了CanalEntityHandler接口并且加上@CanalMonitor注解的类)
     *
     * @param canalMonitor 监听注解
     * @param bean         监听对象
     */
    private void collectInfo(CanalMonitor canalMonitor, Object bean) {
        String databaseName = canalMonitor.databaseName();
        String tableName = canalMonitor.tableName();
        if (StringUtils.isBlank(databaseName) || StringUtils.isBlank(tableName))
            return;
        // 加入容器 (控制小写)
        String key = StringUtils.lowerCase(databaseName + "." + tableName);
        map.put(key, new CanalEntityCallBackHandler((CanalEntityHandler<?>) bean, canalMonitor.ddl()));
    }

}
