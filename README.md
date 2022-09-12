1. 首先需要部署好 Canal-server，不多赘述。
2. 引入canal-spring-boot-starter，要想达到监听目的的话，需要实现 CanalEntityHandler<?> 接口，并且实现类需要在类上加上 @CanalMonitor注解，在注解中设定需要**监听的数据库 databaseName，及数据表 tableName**。
3. 实现CanalEntityHandler<?> 接口，?泛型是实体类，实体类中需要在对应字段上加上JPA注解：@Id、@Column 等。
4. 主要在@Column注解上配置具体字段名，当然如果双方是驼峰 <-> 下划线的话，可以不需要写具体字段名，加@Column注解即可。

