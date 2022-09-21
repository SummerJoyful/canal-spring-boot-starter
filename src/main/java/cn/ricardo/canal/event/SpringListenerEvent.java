package cn.ricardo.canal.event;

import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Spring容器初始化后的事件
 *
 * @author wcp
 * @since 2022/9/10
 */
public class SpringListenerEvent extends ContextRefreshedEvent implements ApplicationContextAware, ApplicationRunner {

    private ApplicationContext applicationContext;

    public SpringListenerEvent(ApplicationContext source) {
        super(source);
    }

    @Override
    public void run(ApplicationArguments args) {
        // 发布自身
        applicationContext.publishEvent(this);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
