# Eureka

## 一.使用spring cloud搭建服务注册中心

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

现在只是实现一个单节点的服务注册中心，一旦发生故障则整个服务就会瘫痪，所以实践中要搭建高可用服务注册中心

## 二.使用spring cloud搭建高可用服务注册中心

在eureka中通过集群实现高可用，Eureka Server的高可用实际上是将自己作为一个服务注册到其他的服务中心上，这样就会形成一组互相注册的服务注册中心，进而实现服务清单的互相同步，达到高可用的效果

### 增加配置文件

在上面的基础上添加两个配置文件，和`application.properties`文件放在同一个目录下面，分别是`application-peer1.properties`和`application-peer2.properties`

```properties
server.port=1111
eureka.instance.hostname=peer1
eureka.client.fetch-registry=false
eureka.client.register-with-eureka=false
eureka.client.service-url.defaultZone=http://peer2:1112/eureka/
```

```properties
server.port=1112
eureka.instance.hostname=peer1
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.client.service-url.defaultZone=http://peer1:1111/eureka/
```

+   在peer1的配置文件中，让它的`server-url`指向peer2；在peer2的配置文件中，让它的`server-url`指向peer1

+   需要在`/etc/hosts`文件中添加`127.0.0.1  peer1`和`127.0.0.1  peer2`这样才可以访问
+   由于peer1和peer2互相指向对方，实际上我们构建了一个双节点的服务注册中心集群

不从idea运行项目，直接打开终端输入`mvn package -Dmaven.test.skip=true`不打包测试也不编译，然后在终端通过java命令指定不同的配置文件

```
java -jar eureka-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=peer1
java -jar eureka-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=peer2
```

分别访问`http://localhost:1111`和`http://localhost:1112`可以发现在peer1节点的DS Replics我们已经看到peer2节点了，在peer2节点的DS Replics我们已经也可以看到peer1节点，如此我们的服务注册中心集群就搭建好了

修改serviceprovider中的配置文件

```
spring.application.name=hello-service
eureka.client.service-url.defaultZone=http://peer1:1111/eureka,http://peer2:1112/eureka/
```

可以发现注册到两个中心

## 三.spring cloud中服务的发现与消费

### 如何实现

服务的发现和消费是两个行为，由不同的对象来完成，服务的发现是由`Eureka`客户端来完成，而服务的消费有`Ribbon`完成。`Ribbon`是一个基于HTTP和TCP的客户端负载均衡器，当我们将`Ribbon`和`Eureka`一起使用时，`Ribbon`会从`Eureka`注册中心去获取服务端列表，然后进行轮询访问以达到负载均衡的作用，服务端是否在线这些问题则交由`Eureka`去维护。下面是实现的过程

### 开启注册中心

首先开启一个服务注册中心，和上面一样，只需要一个单节点就可以，显示当前1111端口被占用`sudo lsof -i:1111`然后`sudo kill -9 pid`杀死服务，接着重新启动即可

### 注册服务

使用命令启动两个实例，然后打开`http://localhost:1111`可以看到`**UP** (2) - [10.98.172.138:hello-service:8080](http://10.98.172.138:8080/info) , [10.98.172.138:hello-service:8081](http://10.98.172.138:8081/info)`两个服务已经注册成功

```shell
java -jar serviceprovider-0.0.1-SNAPSHOT.jar --server.port=8080
java -jar serviceprovider-0.0.1-SNAPSHOT.jar --server.port=8081
```

### 消费服务——创建客户端

首先创建一个spring boot项目，然后添加`Ribbon`和`Eureka`的依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-eureka</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-ribbon</artifactId>
</dependency>
```

### 配置启动入口类

#### 表明客户端身份

首先在启动类上添加`@EnableDiscoveryClient`表明是一个Eureka的客户端，从而可以发现服务

#### 提供`RestTemplate`的Bean

`RestTemplate`可以帮助我们发起一个GET或者POST请求，在提供Bean的同时添加`@LoadBanlaced`注解，表明客户端启用负载均衡

```java
//表示该应用是一个Eureka客户端应用，从而使它具有发现服务的能力
@EnableDiscoveryClient
@SpringBootApplication
public class RibbonconsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RibbonconsumerApplication.class, args);
    }

    //表明开启客户端负载均衡，以及表明是一个Bean
    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### 创建Controller

创建一个Controller类，向其中注入`RestTemplate`，在其中提供一个`/ribbon-consumer`接口，在这个接口中，通过注入的`RestTemplate`实现对HELLO-SERVICE服务提供的`/hello`接口的调用，在调用过程中我们访问的是HELLO-SERVICE而不是某一个具体的地址，可以写成全部大些，会自动找到，别的形式不可以

```java
@RestController
public class ConsumerController {
    @Autowired
    RestTemplate restTemplate;
    @RequestMapping(value = "/ribbon-consumer",method = RequestMethod.GET)
    public String helloController(){
        return restTemplate.getForEntity("http://HELLO-SERVICE/hello",String.class).getBody();
    }
}
```

### 配置服务注册中心的位置

```xml
spring.application.name=ribbon-consumer
server.port=9000
eureka.client.service-url.defaultZone=http://peer1:1111/eureka
```

再次访问`http://localhost:1111`，可以看到客户端了，然后访问`http://localhost:9000/ribbon-consumer`，可以看到hello-service服务中提供的hello接口返回的`Hello Eureka`，在启动的两个服务提供者的终端可以看到当前是哪一个服务提供者响应了客户端。

## 四.Eureka中的核心概念

将从服务注册中心、服务提供者以及服务消费者进行介绍

### 1. 服务提供者

Eureka服务治理体系支持跨平台，无所谓语言，只要注册了，别的应用就可以调用

#### 服务注册

服务提供者在启动的时候会发送REST请求将自己注册在Eureka Server上，同时还携带了一些资深服务的元数据信息，Eureka Server接收到该请求之后，将原数据信息存储在一个双层Map集合中，第一层的key是服务名，第二层的key是具体服务的实例名，同时需要注意配置文件中的`eureka.client.register-with-eureka=true`默认为true，会自动注册

#### 服务同步

比如有两个服务注册中心，地址分别是`http://localhost:1111`和`http://localhost:1112`，还有两个服务提供者地址分别是`http://localhost:8080`和`http://localhost:8081`，将8080服务注册到1111中心，将8081服务注册到1112中心，此时消费者只向1111这个注册中心去查找服务提供者，服务消费者是可以查找到两个服务提供者提供的服务。虽然两个服务提供者的信息分别被两个服务注册中心维护，但是由于服务注册中心纸巾啊也互相注册为服务，当服务提供者发送请求到一个服务注册中心时，它会将该请求转发给集群中相连的其他注册中心，从而实现注册中心之间的服务同步，通过服务同步，两个服务提供者的服务信息我们就可以通过任意一台注册中心来获取到。

```
java -jar eureka-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=peer1  
java -jar eureka-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=peer2
```

启动两个注册中心，和两个服务提供者给provider提供两个配置文件，启动两个服务提供者

application-p1.properties:

```properties
spring.application.name=hello-service
server.port=8080
eureka.client.service-url.defaultZone=http://peer1:1111/eureka
```

application-p2.properties:

```properties
spring.application.name=hello-service
server.port=8081
eureka.client.service-url.defaultZone=http://peer2:1112/eureka
```

```
java -jar serviceprovider-0.0.1-SNAPSHOT.jar --spring.profiles.active=p1
java -jar serviceprovider-0.0.1-SNAPSHOT.jar --spring.profiles.active=p2
```

分别注册成功，查看ribbonconsumer，可以看到虽然我们只向1111的注册中心获取服务列表，但是在实际运行过程中8080和8081都有给我提供服务

#### 服务续约

在注册完服务之后，服务提供者会维护一个心跳来不停的告诉Eureka Server：“我还在运行”，以防止Eureka Server将该服务实例从服务列表中剔除，这个动作称之为服务续约，和服务续约相关的属性有两个，如下：

```properties
eureka.instance.lease-expiration-duration-in-seconds=90
eureka.instance.lease-renewal-interval-in-seconds=30
```

第一个是服务失效时间默认90s，第二个是服务续约的间隔时间默认30s

### 2. 服务消费者

#### 获取服务

当我们启动服务消费者的时候，它会发送一个REST请求给服务注册中心来获取服务注册中心上面的服务提供者列表，而Eureka Server上则会维护一份只读的服务清单来返回给客户端，这个服务清单并不是实时数据，而是一份缓存数据，默认30秒更新一次，如果想要修改清单更新的时间间隔，可以通过`eureka.client.registry-fetch-interval-seconds=30`来修改，单位为秒(注意这个修改是在eureka-server上来修改)。另一方面，我们的服务消费端要确保具有获取服务提供者的能力，此时要确保`eureka.client.fetch-registry=true`这个配置默认为true。

#### 服务调用

服务消费者从服务注册中心拿到服务提供者列表之后，通过服务名就可以获取具体提供服务的实例名和该实例的元数据信息，客户端将根据这些信息来决定调用哪个实例，我们之前采用了Ribbon，Ribbon中默认采用轮询的方式去调用服务提供者，进而实现了客户端的负载均衡。

#### 服务下线

服务提供者在运行的过程中可能会发生关闭或者重启，当服务进行**正常关闭**时，它会触发一个服务下线的REST请求给Eureka Server，告诉服务注册中心我要下线了，服务注册中心收到请求的时候就会把该服务的状态设置为DOWN，表示服务已下线，并将该事件传播出去，这样就可以避免服务消费者调用了一个已经下线的服务提供者

### 3. 服务注册中心

#### 失效剔除

正常的服务下线发生流程有一个前提那就是服务**正常**关闭,但是在实际运行中服务有可能没有正常关闭，比如系统故障、网络故障等原因导致服务提供者非正常下线，那么这个时候对于已经下线的服务Eureka采用了定时清除：Eureka Server在启动的时候会创建一个定时任务，每隔60秒就去将当前服务提供者列表中超过90秒还没续约的服务剔除出去，通过这种方式来避免服务消费者调用了一个无效的服务。

#### 自我保护

这个警告实际上就是触发了Eureka Server的自我保护机制。Eureka Server在运行期间会去统计心跳失败比例在15分钟之内是否低于85%，如果低于85%，Eureka Server会将这些实例保护起来，让这些实例不会过期，但是在保护期内如果服务刚好这个服务提供者非正常下线了，此时服务消费者就会拿到一个无效的服务实例，此时会调用失败，对于这个问题需要服务消费者端要有一些容错机制，如重试，断路器等。我们在单机测试的时候很容易满足心跳失败比例在15分钟之内低于85%，这个时候就会触发Eureka的保护机制，一旦开启了保护机制，则服务注册中心维护的服务实例就不是那么准确了，此时我们可以使用`eureka.server.enable-self-preservation=false`来关闭保护机制，这样可以确保注册中心中不可用的实例被及时的剔除

## 五. 客户端负载均衡

### 服务端负载均衡

负载均衡是我们处理高并发、缓解网络压力和进行服务端扩容的重要手段之一，但是一般情况下我们所说的负载均衡通常都是指服务端负载均衡，服务端负载均衡又分为两种，一种是硬件负载均衡，还有一种是软件负载均衡。

硬件负载均衡主要通过在服务器节点之间安装专门用于负载均衡的设备，常见的如F5。

软件负载均衡则主要是在服务器上安装一些具有负载均衡功能的软件来完成请求分发进而实现负载均衡，常见的就是Nginx。

无论是硬件负载均衡还是软件负载均衡都会维护一个可用的服务端清单，然后通过心跳机制来删除故障的服务端节点以保证清单中都是可以正常访问的服务端节点，此时当客户端的请求到达负载均衡服务器时，负载均衡服务器按照某种配置好的规则从可用服务端清单中选出一台服务器去处理客户端的请求。这就是服务端负载均衡。

### 客户端负载均衡

>   “Ribbo是一个基于HTTP和TCP的客户端负载均衡器，当我们将Ribbon和Eureka一起使用时，Ribbon会从Eureka注册中心去获取服务端列表，然后进行轮询访问以到达负载均衡的作用，客户端负载均衡中也需要心跳机制去维护服务端清单的有效性，当然这个过程需要配合服务注册中心一起完成。”

从上面的描述我们可以看出，客户端负载均衡和服务端负载均衡最大的区别在于服务清单所存储的位置。在客户端负载均衡中，所有的客户端节点都有一份自己要访问的服务端清单，这些清单统统都是从Eureka服务注册中心获取的。在Spring Cloud中我们如果想要使用客户端负载均衡，方法很简单，开启`@LoadBalanced`注解即可，这样客户端在发起请求的时候会先自行选择一个服务端，向该服务端发起请求，从而实现负载均衡。

