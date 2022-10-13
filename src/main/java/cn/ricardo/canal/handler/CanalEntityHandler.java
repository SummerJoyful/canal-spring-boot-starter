package cn.ricardo.canal.handler;

import cn.ricardo.canal.annotation.CanalMonitor;

/**
 * @author wcp
 * @since 2022/9/10
 */
public interface CanalEntityHandler<E> {

    /**
     * 存入操作
     *
     * @param entity 存入对象
     */
    void insert(E entity) throws Exception;

    /**
     * 更新操作
     *
     * @param oldEntity 更新前对象
     * @param newEntity 更新后对象
     */
    void update(E oldEntity, E newEntity) throws Exception;

    /**
     * 删除操作
     *
     * @param entity 删除前对象
     */
    void delete(E entity) throws Exception;

    /**
     * 可选实现，监听DDL操作 (需要在{@link CanalMonitor#ddl()}属性中定义true)
     *
     * @param sql sql语句
     */
    default void ddl(String sql) throws Exception {
        /* Implement this method if necessary. */
    }
}
