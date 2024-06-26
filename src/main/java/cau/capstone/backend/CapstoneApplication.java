package cau.capstone.backend;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;

import java.util.TimeZone;

@SpringBootApplication
public class CapstoneApplication {

    public static void main(String[] args) {


        SpringApplication.run(CapstoneApplication.class, args);
    }

    @PostConstruct
    void started(){
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }


}
