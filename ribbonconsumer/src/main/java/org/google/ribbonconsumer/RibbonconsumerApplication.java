package org.google.ribbonconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

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
