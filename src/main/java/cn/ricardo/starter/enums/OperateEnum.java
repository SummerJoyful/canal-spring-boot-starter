package cn.ricardo.starter.enums;

/**
 * @author wcp
 * @since 2022/9/11
 */
public enum OperateEnum {

    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete");

    private final String name;

    OperateEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
