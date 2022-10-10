package cn.ricardo.canal.callback;

import cn.ricardo.canal.handler.CanalEntityHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import javax.persistence.Column;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.text.ParseException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 内部使用工具，用来进行反射等通用操作
 *
 * @author wcp
 * @since 2022/9/11
 */
class GeneralUtil {
    private GeneralUtil() {
    }

    private static final String[] PARSE_PATTERNS = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"
    };

    /**
     * 获取接口实现类上的接口泛型类型
     *
     * @param clazz 接口实现类
     * @return 泛型Class
     **/
    static Class<?> getGenericsOnInterface(Class<?> clazz) {
        Class<?> result = null;
        // 获取类实现的所有接口，包括参数化类型和Class类型
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        if (0 == genericInterfaces.length) {
            throw new RuntimeException("请指定类 " + clazz + " 实现接口 CanalEntityHandler<?>");
        }

        for (Type type : genericInterfaces) {
            // 在这里做判断，如果接口拥有泛型，则为包装类型ParameterizedTypeImpl；如果没有泛型，则只是Class
            if (type instanceof Class) {
                continue;
            }
            ParameterizedType parameterizedType = (ParameterizedType) type;
            // 判断是否为CanalEntityHandler，不为该接口则直接跳过
            if (!CanalEntityHandler.class.equals(parameterizedType.getRawType())) {
                continue;
            }
            // 获取接口上的具体泛型类型
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (0 == actualTypeArguments.length) {
                throw new RuntimeException("请指定类 " + clazz + " 上的泛型具体类型");
            }
            result = (Class<?>) actualTypeArguments[0];
            break;
        }
        return result;
    }

    /**
     * 简单过滤
     */
    static <E> List<E> streamFilter(List<E> list, Predicate<? super E> predicate) {
        return (CollectionUtils.isEmpty(list)) ? Collections.emptyList() : list.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 根据注解寻找对应属性
     *
     * @param fieldList 字段列表
     * @return 符合条件的字段Map
     */
    static Map<String, Field> getFieldMapByAnnotation(List<Field> fieldList) {
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fieldList) {
            // 禁止访问检查
            ReflectionUtils.makeAccessible(field);
            // 获取本类中的该字段的注解
            Id id = field.getDeclaredAnnotation(Id.class);
            Column column = field.getDeclaredAnnotation(Column.class);
            // 表示没有加注解，不管他
            if (Objects.isNull(column) && Objects.isNull(id)) {
                continue;
            }
            // 未加name属性，必然是驼峰，转为下划线即可 (短路与，不计算后面，不会产生空指针)
            String key = Objects.nonNull(id) || StringUtils.isBlank(column.name())
                    ? camelToUnderline(field.getName())
                    : StringUtils.lowerCase(column.name());
            fieldMap.put(key, field);
        }
        return fieldMap;
    }

    /**
     * 简易版类型转换方法
     *
     * @param value   要转换的值
     * @param sqlType sql类型，基于java.sql.Types类
     * @return 转换后的参数
     */
    static Object convert(final String value, int sqlType) throws ParseException {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Object result;
        switch (sqlType) {
            case Types.BIGINT:
                result = Long.valueOf(value);
                break;
            case Types.BIT:
            case Types.BOOLEAN:
                // 数据库返回的是 1,0 或null
                result = "1".equals(value) ? Boolean.TRUE : Boolean.FALSE;
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                result = new BigDecimal(value);
                break;
            case Types.DOUBLE:
            case Types.FLOAT:
                result = Double.valueOf(value);
                break;
            case Types.REAL:
                result = Float.valueOf(value);
                break;
            case Types.INTEGER:
                result = Integer.valueOf(value);
                break;
            case Types.SMALLINT:
                result = Short.valueOf(value);
                break;
            case Types.TINYINT:
                result = Byte.valueOf(value);
                break;
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                result = DateUtils.parseDate(value.trim(), PARSE_PATTERNS);
                break;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                result = value.getBytes(StandardCharsets.UTF_8);
                break;
            default:
                result = value;
                break;
        }

        return result;
    }

    /**
     * 将驼峰转为下划线
     *
     * @param content 内容
     * @return 处理后的内容
     */
    static String camelToUnderline(String content) {
        // 正则规则 -> 匹配大写字母
        Pattern compile = Pattern.compile("[A-Z]");
        Matcher matcher = compile.matcher(content);
        StringBuffer sb = new StringBuffer();
        // 匹配到合适字符
        while (matcher.find()) {
            // group获取匹配的字符，转化为小写加_ ，接着追加到 StringBuffer
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        // 追加其余部分
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将下划线转为驼峰
     *
     * @param content 内容
     * @return 转换后的内容
     */
    static String underlineToCamel(String content) {
        // 转小写字符，转驼峰不关心大小写，只关心下划线
        content = content.toLowerCase();
        // 匹配下划线_小写字母
        Pattern compile = Pattern.compile("_[a-z]");
        Matcher matcher = compile.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            // 替换下划线，并将下划线后的字符转为大写并追加
            matcher.appendReplacement(sb, matcher.group(0).toUpperCase().replace("_", ""));
        }
        // 追加剩余内容
        matcher.appendTail(sb);
        return sb.toString();
    }


}
