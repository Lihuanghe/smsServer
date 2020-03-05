package com.zx.sms;

import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.zx.sms.config.ConfigFileUtil;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.cmpp.CMPPClientEndpointEntity;

@Component
public class ServerStartListener implements ApplicationListener<ContextRefreshedEvent> {
	private static final Logger logger = LoggerFactory.getLogger(ServerStartListener.class);

	@Autowired
	ConfigFileUtil config ;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() == null) {
			ConfigFileUtil.loadconfiguration("configuration.xml");
			final EndpointManager manager = EndpointManager.INS;
			List serverlist = config.loadServerEndpointEntity();// 服务终端实体类集合
			manager.addAllEndpointEntity(serverlist);
			
			logger.info("load server complete.");
			try {
				manager.openAll();
			} catch (Exception e) {
				logger.error("load Server error.",e);
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			List<CMPPClientEndpointEntity> clientlist = config.loadClientEndpointEntity();// 服务终端实体类集合
			
			for(CMPPClientEndpointEntity entity : clientlist) {
				try {
					manager.openEndpoint(entity);
				} catch (Exception e) {
					logger.error("load client error.",e);
				}
			}
			logger.info("load client complete.");
		}
	}

}
