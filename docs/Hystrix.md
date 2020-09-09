# Hystrix

如果一个模块出现故障会导致依赖他的模块也发生故障从而发生故障蔓延，进而导致整个服务的瘫痪，比如登陆模块依赖于数据库模块，如果数据库模块发生故障，那么当登陆模块去调用数据库模块时可能得不到响应，这个调用的线程被挂起，如果处于高并发的环境下，就会导致登陆模块也崩溃，当一个系统划分的模块越多，这种故障发生的频率就会越高，Spring Cloud对这个问题的解决方案就是Hystrix

### 服务消费者中加入断路器

引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-hystrix</artifactId>
</dependency>
```

### 修改服务消费者启动类

添加```@EnableCirxuitBreaker```注解，开启断路功能

```JAVA
//也可以使用@SpringCloudApplication注解代替这三个注解
@EnableCircuitBreaker
@EnableDiscoveryClient
@SpringBootApplication
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }

}
```

### 修改Controller

创建一个service类

```java
@Service
public class HelloService {
    @Autowired
    private RestTemplate restTemplate;

    //指定请求失败时回调的方法名
    @HystrixCommand(fallbackMethod = "error")
    public String hello(){
        ResponseEntity<String> responseEntity=restTemplate.getForEntity("http://HELLO-SERVICE/hello",String.class);
        return responseEntity.getBody();
    }
	//请求失败时回调的方法
    public String error(){
        return "this is a error";
    }
}
```

在消费者Controller中添加

```java
@Autowired
private HelloService helloService;

@RequestMapping(value = "/ribbon-consumer",method = RequestMethod.GET)
public String helloController(){
    return helloService.hello();
}
```

启动服务注册中心，启动两个服务，端口号分别是8080和8081，启动服务消费者9000，启动成功后，关闭其中一个服务提供者，访问```http://localhost:9000/ribbon-consumer```，当我们刷新时会发现间断出现```this is a error```，因为默认策略是轮询