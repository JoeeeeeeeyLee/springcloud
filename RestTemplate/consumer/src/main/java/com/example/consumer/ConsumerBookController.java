package com.example.consumer;

import com.example.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;

@RestController
public class ConsumerBookController {
    @Autowired
    RestTemplate restTemplate;

    //getForEntity方法的返回值是一个ResponseEntity<T>，它是Spring对HTTP请求响应的封装
    //包括了几个重要元素，如响应码、contentType、contentLength、响应消息等
    //第一个参数是调用的服务，这里是通过服务名进行调用，而不是服务第一，如果写成服务地址那就不能实现客户端负载均衡
    //第二个参数表明我想要返回的类型是String
    @RequestMapping("/getHello")
    public String getHello(){
        ResponseEntity<String> responseEntity=restTemplate.getForEntity("http://HELLO-SERVICE/hello",String.class);
        String body=responseEntity.getBody();
        HttpStatus statusCode = responseEntity.getStatusCode();
        int statusCodeValue = responseEntity.getStatusCodeValue();
        HttpHeaders headers = responseEntity.getHeaders();
        StringBuffer result=new StringBuffer();
        result.append("responseEntity.getBody():").append(body).append("<hr>")
                .append("responseEntity.getStatusCode():").append(statusCode).append("<hr>")
                .append("responseEntity.getStatusCodeValue():").append(statusCodeValue).append("<hr>")
                .append("responseEntity.getHeaders():").append(headers).append("<hr>");
        return result.toString();
    }

    /**
     * 有时候调用服务者提供的接口时，可能需要传递参数，有两种不同的方式
     * 1。可以用一个数字作占位符，最后是一个可变程度的参数，来一一替换前面的占位符
     * 2。也可以使用name={name}的形式，最后一个参数是一个map，map的key为前面占位符的名字，map的value为参数值
     */

    @RequestMapping("/sayhello")
    public String sayHello(){
        ResponseEntity<String> responseEntity=restTemplate
                .getForEntity("http://HELLO-SERVICE/sayhello?name={1}",String.class,"张三");
        return responseEntity.getBody();
    }

    @RequestMapping("/sayhello2")
    public String sayhello2(){
        HashMap<String,String> map=new HashMap<>();
        map.put("name","李四");
        ResponseEntity<String> responseEntity=restTemplate
                .getForEntity("http://HELLO-SERVICE/sayhello?name={name}",String.class,map);
        return responseEntity.getBody();
    }

    /**
     * 第一个调用地址也可以是一个uri而不一定非要是字符串，这时通过Spring提供的UriComponents来进行构建即可
     */
    @RequestMapping("/sayhello3")
    public String sayhello3(){
        UriComponents uriComponents= UriComponentsBuilder
                .fromUriString("http://HELLO-SERVICE/sayhello?name={name}").build().expand("王五").encode();
        URI uri=uriComponents.toUri();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
        return responseEntity.getBody();
    }

    /**
     * 服务提供者不仅可以返回String，也可以返回一个自定义类型的对象
     */
    @RequestMapping("/book1")
    public Book book1(){
        ResponseEntity<Book> responseEntity=restTemplate.getForEntity("http://HELLO-SERVICE/getbook1",Book.class);
        return responseEntity.getBody();
    }

    /**
     * getForObject是对getForEntity的进一步封装，当只关注返回的消息体的内容，对其他信息不关注时，可以用
     */
    @RequestMapping("/book2")
    public Book book2(){
        Book book=restTemplate.getForObject("http://HELLO-SERVICE/getbook1",Book.class);
        return book;
    }

    /**
     * 方法的第一个参数为要调用的服务的地址，第二个参数表示上传的参数，第三个参数表示返回的消息体的数据类型
     */
    @RequestMapping("/book3")
    public Book book3(){
        Book book=new Book();
        book.setName("xiyouji");
        ResponseEntity<Book> responseEntity = restTemplate.postForEntity("http://HELLO-SERVICE/getbook2", book, Book.class);
        return responseEntity.getBody();
    }

    //最后的99替换前面的占位符的值
    @RequestMapping("/put")
    public void put(){
        Book book=new Book();
        book.setName("xiyouji");
        restTemplate.put("http://HELLO-SERVICE/getbook3/{1}",book,99);
    }

    @RequestMapping("/delete")
    public void delete(){
        restTemplate.delete("http://HELLO-SERVICE/getbook4/{1}",100);
    }

}
