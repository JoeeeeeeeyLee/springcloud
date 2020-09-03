## 使用eureka搭建服务注册中心

### 创建服务注册中心

创建一个spring boot工程命名为eureka-server，在`pom.xml`中添加eureka依赖，使用了`<dependencyManagement>`标签，方便库的版本的管理，使用的eureka版本是Dalson.SR3，同时添加服务中心依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>Dalston.SR3</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-eureka-server</artifactId>
</dependency>
```

接着配置文件

```properties
# 服务注册中心的端口号
server.port=1111
# 服务注册中心的主机名
eureka.instance.hostname=localhost
# 创建的是一个服务注册中心，不是普通的应用，默认情况下，这个应用会向服务注册中心注册自己，设置为false禁止这种默认行为
eureka.client.register-with-eureka=false
# 设置为false代表不去检索其他的服务，注册中心是维护服务实例的，不需要检索其他服务
eureka.client.fetch-registry=false
# 设置eureka服务器所在的位置，查询服务和注册服务全部都要依赖这个地址
eureka.client.service-url.defaultZone=http://${eureka.instance.hostname}${server.port}/eureka/
```

启动注册服务中心，运行`ServiceproviderApplication`

```java
// 在Application类中添加@EnableEurekaServer注解即可
@EnableEurekaServer
@SpringBootApplication
public class ServiceproviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceproviderApplication.class, args);
    }
}
```

启动之后打开Chrome访问`http://localhost:1111`即可看到注册中心

### 注册服务提供者

创建一个web工程，每次创建都要从Spring Initialzer来创建，比直接maven创建好

同样添加erueka服务提供者的依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-eureka</artifactId>
    <version>1.4.3.RELEASE</version>
</dependency>
```

添加一个Controller，在其中添建一个访问入口

```java
@RestController
public class HelloController {
    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    private DiscoveryClient client;

    @RequestMapping(value = "/hello", method= RequestMethod.GET)
    public String index() {
        List<ServiceInstance> instances = client.getInstances("hello-service");
        for (int i = 0; i < instances.size(); i++) {
            logger.info("/hello,host:" + instances.get(i).getHost() + ",service-id:" + instances.get(i).getServiceId());
        }
        return "Hello Eureka";
    }
}
```

在Application文件中加入`@EnableDiscoveryClient`注解，激活eureka中的`DiscoveryClient`实现，因为我们在Controller中注入了`DiscovryClient`

```java
@EnableDiscoveryClient
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
```

在配置文件中加入如下配置

```properties
spring.application.name=hello-service
eureka.client.service-url.defaultZone=http://localhost:1111/eureka
```

运行`DemoApplication`，然后刷新`http://localhost:1111`页面，即可看到已经注册成功

现在只是实现一个单节点的服务注册中心，一旦发生故障则整个服务就会瘫痪，