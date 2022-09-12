package cn.ricardo.starter.annotation;

import cn.ricardo.starter.handler.CanalMonitorCollectHandler;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 热插拔，即关即开
 *
 * @author wcp
 * @since 2022/9/12
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CanalMonitorCollectHandler.class)
public @interface EnableCanalClient {

}
