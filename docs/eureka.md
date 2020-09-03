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
# 
eureka.client.service-url.defaultZone=http://${eureka.instance.hostname}${server.port}/eureka/
```