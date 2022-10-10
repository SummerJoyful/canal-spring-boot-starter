package cn.ricardo.canal.callback;

import com.alibaba.otter.canal.protocol.CanalEntry;

import java.util.List;
import java.util.Objects;

/**
 * @author wcp
 * @since 2022/9/10
 */
abstract class AbstractCallbackHandler {

    public final void parseAndExecute(CanalEntry.RowChange rowChange) {
        // 获取更新类型
        CanalEntry.EventType eventType = rowChange.getEventType();
        Operation operation = Operation.match(eventType);
        if (Objects.nonNull(operation)) operation.doHandle(this, rowChange);
    }

    public abstract void insertOperation(List<CanalEntry.RowData> rowDataList);

    public abstract void updateOperation(List<CanalEntry.RowData> rowDataList);

    public abstract void deleteOperation(List<CanalEntry.RowData> rowDataList);

    public abstract void ddlOperation(CanalEntry.RowChange rowChange);

    enum Operation {
        INSERT(CanalEntry.EventType.INSERT) {
            @Override
            void doHandle(AbstractCallbackHandler handler, CanalEntry.RowChange rowChange) {
                handler.insertOperation(rowChange.getRowDatasList());
            }
        },
        UPDATE(CanalEntry.EventType.UPDATE) {
            @Override
            void doHandle(AbstractCallbackHandler handler, CanalEntry.RowChange rowChange) {
                handler.updateOperation(rowChange.getRowDatasList());
            }
        },
        DELETE(CanalEntry.EventType.DELETE) {
            @Override
            void doHandle(AbstractCallbackHandler handler, CanalEntry.RowChange rowChange) {
                handler.deleteOperation(rowChange.getRowDatasList());
            }
        },
        DDL(CanalEntry.EventType.ALTER) {
            @Override
            void doHandle(AbstractCallbackHandler handler, CanalEntry.RowChange rowChange) {
                handler.ddlOperation(rowChange);
            }
        };

        private final CanalEntry.EventType eventType;

        Operation(CanalEntry.EventType eventType) {
            this.eventType = eventType;
        }

        abstract void doHandle(AbstractCallbackHandler handler, CanalEntry.RowChange rowChange);

        static Operation match(CanalEntry.EventType eventType) {
            if (Objects.isNull(eventType))
                return null;
            for (Operation value : Operation.values()) {
                if (eventType.equals(value.eventType)) {
                    return value;
                }
            }
            return null;
        }
    }

}
