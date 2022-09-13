#### 描述

  简单实现的Canal-Spring-Boot-Starter，利用SpringBoot的事件、后置处理等实现，使用工作线程无限循环监听Canal-Server，大量用到反射，用于运行时，封装未知实体类，并将实体类注入到方法中。

  至于Canal-Server如何部署这样的问题，就不多赘述了。





#### 如何使用

1. 配置文件上加上对应的配置

```java
canal:
  host: 127.0.0.1
  port: 11111
```

2. 启动类加上@EnableCanalClient注解，即关即开

```java
@SpringBootApplication
@EnableCanalClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

3. 将监听操作具现到具体的库与表 (接口还拥有一个默认方法，可以监听该表的DDL)

```java
@Component
@CanalMonitor(databaseName = "zxg", tableName = "teacher", ddl = true)
public class TeacherHandler implements CanalEntityHandler<TeacherEntity> {

    @Override
    public void insert(TeacherEntity entity) {
        System.out.println("存入数据：" + entity);
    }

    @Override
    public void update(TeacherEntity oldEntity, TeacherEntity newEntity) {
        System.out.println("更新数据前：" + oldEntity);
        System.out.println("更新数据后：" + newEntity);
    }

    @Override
    public void delete(TeacherEntity entity) {
        System.out.println("被删除的数据：" + entity);
    }
    
    @Override
    public void ddl(String sql) {
        System.out.println("DDL操作：" + sql);
    }
}
```

4. 实体类必须加上@Id、@Column注解 (可以不用@Column(name = "")，默认注解即可，只要是驼峰 <-> 下划线即可匹配)

```java
@Id
private Integer teacherId;

@Column
private String teacherNo;
```





#### 实现原理

1. 项目组成

```
CanalProperties			    --> 将配置实例化为对象。
@EnableCanalClient 		    --> 用于启动整体能力的注解。
@CanalMonitor  	   		    --> 用于将监听操作具现到具体的数据库、数据表。
CanalAutoConfiguration  	--> 自动配置类，整体服务的开启者。
SpringListenerEvent		    --> Spring初始化事件。
CanalEntityHandler<?>		--> 监听操作入口，没有具体的接口控制行为的话，没办法反射找到对应操作。
CanalMonitorCollectHandler	--> 后置处理，将所有监听类注册到容器。
MessageHandler			    --> 消息处理器，继承CanalMonitorCollectHandler，拥有其收集的所有监听Bean。
CanalEntityHandlerProxy		--> 作为行数据的具体处理对象，是CanalEntityHandler接口实现类的封装对象，反射调用等等操作。
GeneralUtil			        --> 内部使用工具。
```







