package com.example.wonit.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

// 라우팅 기능: 사용자의 요청(Request)을 주소줄과 매핑해서 서버에 전달하는 역할
@RestController
@RequestMapping("/hello")  // localhost:8080/hello
public class HelloController {

    // application.yml 에 들에었는 app.title 의 속성값을 가져와서 bean에 등록
    @Value("${app.title}")
    private String appTitle;

    @GetMapping
    public String sayHello() {
        return "Hello, Spring Boot!" + appTitle;
    }

//    @GetMapping("/{path}")  // localhost:8080/hello/경로변수(PathVarible)
//    public String sayHello(@PathVariable String path) {
//        return "Hello, Spring Boot!" + path;
//    }

//    @GetMapping // localhost:8080/hello?name=값
//    public String sayHello(@RequestParam(value="name") String name) {
//        return "Hello, Spring Boot!" + name;
//    }

    @GetMapping("/{path}") // localhost:8080/hello/path?name=값&footSize=250
    // 2번째 RequestParam으로 footSize를 받아서 Hello, Spring Boot!
    // 나는 이름, 신발사이즈는 250    // 없으면 안 들어옴2
    public String sayHello(@PathVariable String path,
                           @RequestParam(value="name", defaultValue = "안 들어옴") String name,
                           @RequestParam(value="footSize", defaultValue = "안 들어옴2") String foot) {
        return "Hello, Spring Boot! " + path + " "+ name + " 신발사이즈는 " + foot;
    }
}
