package com.zx.sms;

import java.util.concurrent.locks.LockSupport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages= {"com.zx.sms"})
public class DemoApplication {
	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
		LockSupport.park();
	}
}
