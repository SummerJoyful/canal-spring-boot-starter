package cn.ricardo.canal.annotation;

import java.lang.annotation.*;

/**
 * 监听注解
 *
 * @author wcp
 * @since 2022/9/9
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CanalMonitor {

    /**
     * 数据库名称
     */
    String databaseName() default "";

    /**
     * 表名称
     */
    String tableName() default "";

    /**
     * 是否监听DDL操作
     */
    boolean ddl() default false;
}
