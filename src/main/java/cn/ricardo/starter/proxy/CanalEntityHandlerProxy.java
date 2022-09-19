package cn.ricardo.starter.proxy;

import cn.ricardo.starter.enums.OperateEnum;
import cn.ricardo.starter.handler.CanalEntityHandler;
import com.alibaba.otter.canal.protocol.CanalEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 包装类，实现调用方法
 *
 * @author wcp
 * @since 2022/9/10
 */
public class CanalEntityHandlerProxy extends AbstractCanalEntityHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalEntityHandlerProxy.class);

    private final CanalEntityHandler<?> canalEntityHandler;

    private final boolean isDdl;

    public CanalEntityHandlerProxy(CanalEntityHandler<?> canalEntityHandler, boolean isDdl) {
        this.canalEntityHandler = canalEntityHandler;
        this.isDdl = isDdl;
    }

    public void insertOperation(List<CanalEntry.RowData> rowDataList) {
        generalOperation(rowDataList, OperateEnum.INSERT.getName());
    }


    public void deleteOperation(List<CanalEntry.RowData> rowDataList) {
        generalOperation(rowDataList, OperateEnum.DELETE.getName());
    }

    @Override
    public void ddlOperation(CanalEntry.RowChange rowChange) {
        if (rowChange.getIsDdl() && isDdl) {
            String ddlName = OperateEnum.DDL.getName();
            // 正则过滤掉 注释
            Pattern pattern = Pattern.compile("(?ms)('(?:''|[^'])*')|--.*?$|/\\*.*?\\*/|#.*?$|");
            String result = pattern.matcher(rowChange.getSql()).replaceAll("$1");
            try {
                MethodUtils.invokeMethod(canalEntityHandler, ddlName, result);
            } catch (Exception e) {
                log.error("[{}] 实体类注入出现错误: {}", ddlName, ExceptionUtils.getMessage(e));
                e.printStackTrace();
            }
        }
    }

    /**
     * 通用操作
     *
     * @param rowDataList 行数据 (每一行包含该行所有列)
     */
    public void generalOperation(List<CanalEntry.RowData> rowDataList, String operateName) {
        for (CanalEntry.RowData rowData : rowDataList) {
            // 改前改后数据列
            List<CanalEntry.Column> columnList = OperateEnum.INSERT.getName().equals(operateName)
                    ? rowData.getAfterColumnsList() : rowData.getBeforeColumnsList();
            // 获取实体类Class
            Class<?> entityClass = GeneralUtil.getGenericsOnInterface(canalEntityHandler.getClass());

            try {
                Object entity = fillEntity(entityClass, columnList);
                // 将实体类注入到对应方法中 (必须通过反射执行该方法，否则会出现类型问题)
                MethodUtils.invokeMethod(canalEntityHandler, operateName, entity);
            } catch (Exception e) {
                log.error("[{}] 实体类注入出现错误: {}", operateName, ExceptionUtils.getMessage(e));
                e.printStackTrace();
            }
        }
    }

    /**
     * 构建实例对象
     *
     * @param entityClass 实例对象Class
     * @param columnList  列对象
     */
    private static Object fillEntity(Class<?> entityClass, List<CanalEntry.Column> columnList) throws Exception {
        // 创建实例
        Object newInstance = entityClass.newInstance();
        // 获取所有字段
        List<Field> allFieldList = FieldUtils.getAllFieldsList(newInstance.getClass());
        // 获取满足条件字段(拥有 @Column注解的字段)
        Map<String, Field> fieldMap = GeneralUtil.getFieldMapByAnnotation(allFieldList);

        // 遍历列，注入属性值
        for (CanalEntry.Column column : columnList) {
            // 默认小写，获取该字段的 Field对象
            Field field = fieldMap.get(StringUtils.lowerCase(column.getName()));
            // 如果匹配不上具体字段则跳过
            if (Objects.isNull(field)) {
                continue;
            }
            // 类型转换
            Object value = GeneralUtil.convert(column.getValue(), column.getSqlType());
            // 属性注入
            FieldUtils.writeField(field, newInstance, value);
        }

        return newInstance;
    }



    public void updateOperation(List<CanalEntry.RowData> rowDataList) {
        for (CanalEntry.RowData rowData : rowDataList) {
            // 更改后的数据列
            List<CanalEntry.Column> newColumnList = rowData.getAfterColumnsList();
            List<CanalEntry.Column> oldColumnList = rowData.getBeforeColumnsList();
            // 获取类上的泛型类型 (其实就是实体类)
            Class<?> entityClass = GeneralUtil.getGenericsOnInterface(canalEntityHandler.getClass());
            // 通过Class创建对象，注入数据 (TODO 只能通过反射注入，直接调用方法会导致类型问题!!!)
            // 循环 newColumnList 和 oldColumnsList 组合数据，然后在循环中执行方法，先需要创建出实体对象
            executeUpdate(entityClass, newColumnList, oldColumnList);
        }
    }

    public void executeUpdate(Class<?> entityClass, List<CanalEntry.Column> newColumnList, List<CanalEntry.Column> oldColumnList) {
        // 最后需要执行的方法名称
        String updateName = OperateEnum.UPDATE.getName();

        try {
            Object newInstance = entityClass.newInstance();
            Object oldInstance = entityClass.newInstance();

            // 获取实体类所有字段，准备注入值
            List<Field> newEntityFieldList = FieldUtils.getAllFieldsList(newInstance.getClass());
            List<Field> oldEntityFieldList = FieldUtils.getAllFieldsList(oldInstance.getClass());

            // 拥有@Column注解的属性字段
            Map<String, Field> newFieldMap = GeneralUtil.getFieldMapByAnnotation(newEntityFieldList);
            Map<String, Field> oldFieldMap = GeneralUtil.getFieldMapByAnnotation(oldEntityFieldList);

            // 该循环做实体类属性的注入操作
            for (CanalEntry.Column newColumn : newColumnList) {
                // ---- 新实体类逻辑
                String newColumnName = StringUtils.lowerCase(newColumn.getName());
                // 获取新列的对应字段
                Field newField = newFieldMap.get(newColumnName);
                // 如果匹配不上具体字段则跳过
                if (Objects.isNull(newField)) {
                    continue;
                }
                // 类型转换
                Object newValue = GeneralUtil.convert(newColumn.getValue(), newColumn.getSqlType());
                // 属性注入
                FieldUtils.writeField(newField, newInstance, newValue);
                // -------------------------------------------------------
                // ---- 旧实体类逻辑
                CanalEntry.Column oldColumn;
                List<CanalEntry.Column> matchColumnList = GeneralUtil.streamFilter(oldColumnList, el -> StringUtils.equals(newColumnName, el.getName()));
                if (!matchColumnList.isEmpty()) {
                    oldColumn = matchColumnList.get(0);
                    Field oldField = oldFieldMap.get(StringUtils.lowerCase(oldColumn.getName()));
                    Object oldValue = GeneralUtil.convert(oldColumn.getValue(), oldColumn.getSqlType());
                    // 属性注入
                    FieldUtils.writeField(oldField, oldInstance, oldValue);
                }
            }

            // 实体类属性注入完毕，执行方法，将实体类注入到方法参数中
            MethodUtils.invokeMethod(canalEntityHandler, updateName, oldInstance, newInstance);
        } catch (Exception e) {
            log.error("[update] 实体类注入出现错误: {}", ExceptionUtils.getMessage(e));
            e.printStackTrace();
        }
    }

}
