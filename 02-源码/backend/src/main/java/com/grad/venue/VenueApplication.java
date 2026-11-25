package com.grad.venue;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 校内公共场地借用与审批管理系统启动类 */
@SpringBootApplication
@MapperScan("com.grad.venue.mapper")
public class VenueApplication {
    public static void main(String[] args) {
        SpringApplication.run(VenueApplication.class, args);
    }
}
