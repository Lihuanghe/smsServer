package com.zx.sms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class  SpringContextUtil implements ApplicationContextAware {
	private static final Logger logger = LoggerFactory.getLogger(SpringContextUtil.class);
	
	static ApplicationContext ctx ;
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		ctx = arg0;//NOSONAR
	}
	public static  Object getBean(String s) {
		return ctx.getBean(s);
	}

	public static <T> T getBean(String s, Class<T> class1)
	{
		return ctx.getBean(s,class1);
	}

	public static <T> T getBean(Class<T> class1)
	{
		return ctx.getBean(class1);
	}
}
