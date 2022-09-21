package cn.ricardo.canal.enums;

/**
 * @author wcp
 * @since 2022/9/11
 */
public enum OperateEnum {

    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete"),
    DDL("ddl");

    private final String name;

    OperateEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
