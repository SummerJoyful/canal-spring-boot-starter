package cn.ricardo.canal.handler;

import cn.ricardo.canal.callback.CanalEntityCallBackHandler;
import cn.ricardo.canal.event.SpringListenerEvent;
import cn.ricardo.canal.properties.CanalProperties;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;

import java.util.List;
import java.util.Objects;

/**
 * @author wcp
 * @since 2022/9/9
 */
@DependsOn("canalConnector")
public class MessageHandler extends CanalMonitorCollectHandler implements CanalAction, ApplicationListener<SpringListenerEvent> {

    private final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    /**
     * 标记(表示当前是否需要继续监听Canal)，需要保持可见
     */
    private volatile boolean flag;

    /**
     * 工作线程 (使用单独线程，支持动态开关Canal监听线程 [因为线程需要无限循环监听Canal]，使用线程池很难动态控制)
     * 使用工作线程得保证只有一个线程调用本对象，不能用final标记。
     * 如果用线程池控制：通过flag标记，使任务取消无限循环，再通过submit()方法的Future返回对象检测任务结束与否，主要是无法获取工作线程，无法进行中断。
     */
    private Thread runner;

    /**
     * Canal连接客户端
     */
    private final CanalConnector canalConnector;

    /**
     * Canal配置对象
     */
    private final CanalProperties canalProperties;

    public MessageHandler(CanalConnector canalConnector, CanalProperties canalProperties) {
        this.canalConnector = canalConnector;
        this.canalProperties = canalProperties;
    }

    @Override
    public void onApplicationEvent(SpringListenerEvent springListenerEvent) {
        log.info("项目启动成功，开始消费Canal数据...");
        // 事件触发
        start();
    }

    @Override
    public void start() {
        log.info("Start Canal Client...");
        // 开始监听
        this.flag = true;
        // 执行
        this.runner = new Thread(this::execute, "Canal-Thread");
        this.runner.start();
    }

    @Override
    public void stop() {
        log.info("Stop Canal Client...");
        // 监听结束
        this.flag = false;
        // 防止线程为空
        if (null != this.runner) {
            // 中断线程
            this.runner.interrupt();
        }
        // TODO 结束后，是否需要断开 connector
        canalConnector.disconnect();
    }

    @SuppressWarnings("BusyWait")
    private void execute() {
        try {
            while (flag) {
                // 尝试从master那边拉去数据batchSize条记录，有多少取多少
                Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());
                long batchId = message.getId();
                int size = message.getEntries().size();
                // 取到消息
                if (batchId != -1 && size != 0) {
                    // 消息处理
                    handle(message);
                } else {
                    try {
                        Thread.sleep(canalProperties.getSleepMillis());
                    } catch (InterruptedException e) {
                        log.error("睡眠被唤醒，中断线程! {}", ExceptionUtils.getMessage(e));
                        e.printStackTrace();
                    }
                }
                canalConnector.ack(batchId);
            }
        } catch (Exception e) {
            log.error("canal client异常：{}", ExceptionUtils.getMessage(e));
            e.printStackTrace();
        } finally {
            canalConnector.disconnect();
        }
    }

    private void handle(Message message) throws InvalidProtocolBufferException {
        List<CanalEntry.Entry> entries = message.getEntries();
        for (CanalEntry.Entry entry : entries) {
            // 限制行数据类型
            if (CanalEntry.EntryType.ROWDATA.equals(entry.getEntryType())) {
                // 拼接key (限制小写)
                String key = StringUtils.lowerCase(entry.getHeader().getSchemaName() + "." + entry.getHeader().getTableName());
                CanalEntityCallBackHandler handlerProxy = map.get(key);
                // 没有的话表示没有监控该表，直接跳过
                if (Objects.isNull(handlerProxy))
                    continue;

                // 解析为行数据
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

                // 交给包装类处理
                handlerProxy.handle(rowChange);
            }
        }
    }

}
