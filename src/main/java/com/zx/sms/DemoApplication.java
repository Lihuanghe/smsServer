package com.zx.sms;

import java.util.concurrent.locks.LockSupport;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@Configurable
@SpringBootApplication
@ComponentScan(basePackages= {"com.zx.sms"})
public class DemoApplication {
	public static void main(String[] args) throws Exception {
		try {
			SpringApplication.run(DemoApplication.class, args);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		LockSupport.park();
	}
}
