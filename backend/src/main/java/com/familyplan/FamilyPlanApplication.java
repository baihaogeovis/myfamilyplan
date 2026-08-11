package com.familyplan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.familyplan.mapper")
public class FamilyPlanApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FamilyPlanApplication.class, args);
        System.out.println("====================================");
        System.out.println("家庭成长系统启动成功!");
        System.out.println("API文档: http://localhost:8080/api/doc.html");
        System.out.println("====================================");
    }
}
