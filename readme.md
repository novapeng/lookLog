# 说明
  这是一个在线查看日志的工具, 基于mongoDB存储日志.
   通过访问 http://{应用域名根目录}/lookLog 地址可以实时查看当前应用打出的日志
   
# 环境要求
   * spring-boot项目
   * JDK8

# 使用方式

1. 将源代码pull到本地执行mvn install命令, 在应用项目的pom.xml中添加如下依赖,(版本号与依赖的spring-boot版本号保持一致)

    ```javascript
    
        <dependency>
            <groupId>novayoung</groupId>
            <artifactId>spring-boot-looklog</artifactId>
            <version>1.4.1.RELEASE</version>
        </dependency>
        
    ```


2. 添加配置, 在application.properties中加入以下配置项.

    ```javascript

    \# 是否开启日志查看功能, 值为 true/false
    
    lookLog.enable=
    
    \# 日志缓存的秒数, 默认是一天, 如果为-1表示永不过期
    
    lookLog.cachedSecond=
    
    \# 日志临时存储的MongoDB表名
    
    lookLog.mongoDbCollectionName=
    
    \# MongoDB Uri
    spring.data.mongodb.uri=

    
    ```
 
3. 在logback.xml文件中增加如下代码.
    
   ```javascript
   
    <include resource="looklog.xml"/>
   
   
   ```
4. 在main函数的启动类中, 增加对包"novayoung.log"的扫描, 同时将spring上下文对象注入到LookLogAppender中, 代码如下

    ```javascript
    
        @SpringBootApplication(scanBasePackages = {"当前应用需要扫描的包", "novayoung.log"})   //此处增加对"novayoung.log"包的扫描
        public class TestApplication {
        
            public static void main(String[] args) {
                ApplicationContext applicationContext = SpringApplication.run(TestApplication.class, args);
                LookLogAppender.setApplicationContext(applicationContext);   //此处增加spring上下文的注入
            }
        }
    
    ```
    
5. 打开浏览器, 访问 http://{应用域名根目录}/lookLog

# 关于traceId
  traceId即请求跟踪的ID, 如果在并发的情况下出现异常, 我们需要对出现异常的那次调用进行日志分析, 这个时候我们就可以根据traceId来进行过滤, 找出该次调用的所有日志进行分析.
  
  looklog的traceId分两种
  
  * http请求
    
    默认已经实现了traceId,不需要增加额外的代码
    
  * 跑批
    
    需要加注解实现, 代码如下
    
    ```javascript
    
        public class SimpleJob {
        
            @TraceIdJobFilter.TraceId       //此处增加注解即可
            public void doJob() {
                // do something...
            }
        
        }
    
    ```