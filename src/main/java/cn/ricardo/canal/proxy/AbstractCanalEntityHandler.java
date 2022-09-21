package cn.ricardo.canal.proxy;

import com.alibaba.otter.canal.protocol.CanalEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author wcp
 * @since 2022/9/10
 */
abstract class AbstractCanalEntityHandler {

    private final Map<CanalEntry.EventType, Consumer<List<CanalEntry.RowData>>> consumerMap = new HashMap<>();

    {
        consumerMap.put(CanalEntry.EventType.INSERT, this::insertOperation);
        consumerMap.put(CanalEntry.EventType.UPDATE, this::updateOperation);
        consumerMap.put(CanalEntry.EventType.DELETE, this::deleteOperation);
    }

    public final void parseAndExecute(CanalEntry.RowChange rowChange) {
        // 获取更新类型
        CanalEntry.EventType eventType = rowChange.getEventType();
        // 获取本次更改行数
        List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
        // 执行
        Consumer<List<CanalEntry.RowData>> consumer = consumerMap.get(eventType);
        if (null != consumer) {
            consumer.accept(rowDataList);
        } else if (CanalEntry.EventType.ALTER.equals(eventType)) {
            ddlOperation(rowChange);
        }
    }

    public abstract void insertOperation(List<CanalEntry.RowData> rowDataList);

    public abstract void updateOperation(List<CanalEntry.RowData> rowDataList);

    public abstract void deleteOperation(List<CanalEntry.RowData> rowDataList);

    public abstract void ddlOperation(CanalEntry.RowChange rowChange);
}
