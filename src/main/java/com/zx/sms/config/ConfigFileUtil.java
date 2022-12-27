package com.zx.sms.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

import com.zx.sms.common.GlobalConstance;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.ServerEndpoint;
import com.zx.sms.connect.manager.EndpointEntity.SupportLongMessage;
import com.zx.sms.connect.manager.cmpp.CMPPEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipServerChildEndpointEntity;
import com.zx.sms.connect.manager.sgip.SgipServerEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPServerChildEndpointEntity;
import com.zx.sms.connect.manager.smgp.SMGPServerEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPServerEndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import com.zx.sms.handler.api.BusinessHandlerInterface;
import com.zx.sms.handler.api.smsbiz.CmppEchoDeliverHandler;

import io.netty.channel.ChannelHandlerContext;
/**
 */
@Component
public class ConfigFileUtil {
	private static DefaultConfigurationBuilder configbuilder = new DefaultConfigurationBuilder();
	private static boolean isLoad = false;
	private static CombinedConfiguration config=null;
	private static final Logger logger = LoggerFactory.getLogger(ConfigFileUtil.class);
	
	
	public synchronized static void loadconfiguration(String filepath) {
		// 多线程并发时，需要判断一次是否被其它线程load过了
		if (isLoad)
			return;
		configbuilder.setFileName(filepath);
		try {
			config = configbuilder.getConfiguration(true);
			isLoad = true;
		} catch (ConfigurationException e) {
			logger.error("load config {} failed.", filepath, e);
		}
	}
	
	private static void initLoad() {
		if (!isLoad) {
			loadconfiguration("configuration.xml");
		}
	}

	// 从服务加载xml配置文件
	public List<EndpointEntity> loadServerEndpointEntity() {
		initLoad();
		XMLConfiguration clientconfig = (XMLConfiguration) config.getConfiguration("serverEndPoint");
		List<HierarchicalConfiguration> servers = clientconfig.configurationsAt("server");
		if (servers.isEmpty())
			return null;
		List<EndpointEntity> result = new ArrayList<EndpointEntity>();
		for (HierarchicalConfiguration server : servers) {
			EndpointEntity tmpSever = null ;
			String serverType = server.getString("channelType");
			if("CMPP".equals(serverType)){
				tmpSever = new CMPPServerEndpointEntity();
			}else if("SMPP".equals(serverType)) {
				tmpSever = new SMPPServerEndpointEntity();
			}else if("SMGP".equals(serverType)) {
				tmpSever = new SMGPServerEndpointEntity();
			}else if("SGIP".equals(serverType)) {
				tmpSever = new SgipServerEndpointEntity();
			}
				tmpSever.setId(server.getString("id"));
				tmpSever.setDesc(server.getString("desc"));
				tmpSever.setValid(true);
				tmpSever.setHost(server.getString("host", "0.0.0.0"));
				tmpSever.setPort(server.getInt("port", 7890));
				HierarchicalConfiguration endpoints = server.configurationAt("endpoints");
				List<HierarchicalConfiguration> sessions = endpoints.configurationsAt("endpoint");
				if (sessions.isEmpty())
					break;
				for (HierarchicalConfiguration session : sessions) {
					EndpointEntity tmp = null;
					if("CMPP".equals(serverType)){
						 tmp = new CMPPServerChildEndpointEntity();
						buildCMPPEndpointEntity(session, (CMPPEndpointEntity)tmp);
					}else if("SMPP".equals(serverType)) {
						 tmp = new SMPPServerChildEndpointEntity();
						buildSMPPEndpointEntity(session, (SMPPEndpointEntity)tmp);
					}else if("SMGP".equals(serverType)) {
						 tmp = new SMGPServerChildEndpointEntity();
						buildSMGPEndpointEntity(session, (SMGPEndpointEntity)tmp);
					}else if("SGIP".equals(serverType)) {
						 tmp = new SgipServerChildEndpointEntity();
						buildSgipEndpointEntity(session,(SgipEndpointEntity) tmp);
					}
					tmp.setSupportLongmsg(SupportLongMessage.BOTH);
					tmp.setIdleTimeSec((short)10);
					((ServerEndpoint)tmpSever).addchild(tmp);
				}
				result.add(tmpSever);
		}
		return result;
	}
	private static void buildSgipEndpointEntity(HierarchicalConfiguration session, SgipEndpointEntity tmp) {
		initLoad();
		tmp.setId(session.getString("id"));
		tmp.setValid(session.getBoolean("isvalid", true));
		tmp.setLoginName(session.getString("user"));
		tmp.setLoginPassowrd(session.getString("passwd"));
		tmp.setNodeId(session.getLong("nodeId", 259200L));
		tmp.setMaxChannels(session.getShort("maxChannels"));
	
		addBusinessHandlerSet(session,tmp);
	}
	
	private static void buildSMGPEndpointEntity(HierarchicalConfiguration session, SMGPEndpointEntity tmp) {
		initLoad();
		tmp.setId(session.getString("id"));
		tmp.setValid(session.getBoolean("isvalid", true));
		tmp.setClientID(session.getString("user"));
		tmp.setPassword(session.getString("passwd"));
		tmp.setClientVersion(session.getByte("version", (byte) 0x30));
		tmp.setMaxChannels(session.getShort("maxChannels"));
		addBusinessHandlerSet(session,tmp);

	}
	
	private static void buildSMPPEndpointEntity(HierarchicalConfiguration session, SMPPEndpointEntity tmp) {
		initLoad();
		tmp.setId(session.getString("id"));
		tmp.setValid(session.getBoolean("isvalid", true));
		tmp.setSystemId(session.getString("user"));
		tmp.setPassword(session.getString("passwd"));
		tmp.setMaxChannels(session.getShort("maxChannels"));
		addBusinessHandlerSet(session,tmp);

	}
	
	private static void addBusinessHandlerSet(HierarchicalConfiguration session,EndpointEntity tmp) {
		HierarchicalConfiguration handlerSet = session.configurationAt("businessHandlerSet");

		List<Object> handlers = handlerSet.getList("handler");

		List<BusinessHandlerInterface> bizHandlers = new ArrayList<BusinessHandlerInterface>();
		tmp.setBusinessHandlerSet(bizHandlers);

		bizHandlers.add(SpringContextUtil.getBean(CmppEchoDeliverHandler.class));
		
		if (handlers != null && !handlers.isEmpty()) {
			for (Object handler : handlers) {
				if (!tmp.isValid())
					continue;

				if (handler == null)
					continue;
				if (handler instanceof String && StringUtils.isBlank((String) handler)) {
					continue;
				}

				BusinessHandlerInterface handlerobj = getBeanFromCtx((String) handler);
				if (handlerobj != null) {
					bizHandlers.add(new AbstractBusinessHandler() {

					    @Override
					    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
					    	handlerobj.setEndpointEntity(getEndpointEntity());
					    	ctx.pipeline().addAfter(GlobalConstance.sessionHandler, handlerobj.name(), handlerobj);
					    	ctx.pipeline().remove(this);
					    }
						@Override
						public String name() {
							return "ResponseSenderHandler";
						}
					});
				} 
			}
		}
	}
	
	private static void buildCMPPEndpointEntity(HierarchicalConfiguration session, CMPPEndpointEntity tmp) {
		initLoad();
		tmp.setId(session.getString("id"));
		tmp.setValid(session.getBoolean("isvalid", true));
		tmp.setUserName(session.getString("user"));
		tmp.setPassword(session.getString("passwd"));
		tmp.setVersion(session.getShort("version", (short) 0x30));
		tmp.setMaxChannels(session.getShort("maxChannels"));
		tmp.setReadLimit(session.getInt("readLimit"));
		addBusinessHandlerSet(session,tmp);

	}
	
	private static BusinessHandlerInterface getBeanFromCtx(String beanName) {
		try {
			try {
				Object obj_h = SpringContextUtil.getBean(beanName);
				if (obj_h != null && obj_h instanceof BusinessHandlerInterface) {
					return (BusinessHandlerInterface) obj_h;
				}
			} catch (NoSuchBeanDefinitionException e) { //NOSONAR
				Class<BusinessHandlerInterface> clz = (Class<BusinessHandlerInterface>) Class.forName(beanName);
				if (BusinessHandlerInterface.class.isAssignableFrom(clz)) {
					return (BusinessHandlerInterface) SpringContextUtil.getBean(clz);
				}
			}

		} catch (Exception e) {//NOSONAR
			logger.error("Bean {} Does not exists。", beanName,e);
		}

		return null;
	}
	
}
