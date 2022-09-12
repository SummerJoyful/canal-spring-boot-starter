package cn.ricardo.starter.handler;

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
    void insert(E entity);

    /**
     * 更新操作
     *
     * @param oldEntity 更新前对象
     * @param newEntity 更新后对象
     */
    void update(E oldEntity, E newEntity);

    /**
     * 删除操作
     *
     * @param entity 删除前对象
     */
    void delete(E entity);

}
