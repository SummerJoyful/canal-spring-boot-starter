package cn.ricardo.canal.handler;

/**
 * Canal线程监听开始、关闭
 *
 * @author wcp
 * @since 2022/9/9
 */
public interface CanalAction {

    /**
     * 准备动作
     */
    void start();

    /**
     * 完成动作
     */
    void stop();

}
