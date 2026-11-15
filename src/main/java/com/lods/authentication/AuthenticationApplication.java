package com.lods.authentication;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lods.authentication.infrastructure.persistence.mapper")
public class AuthenticationApplication {

    static void main(String[] args) {
        SpringApplication.run(AuthenticationApplication.class, args);
        System.out.println("========================= Authentication Init Done =========================");
    }

}
