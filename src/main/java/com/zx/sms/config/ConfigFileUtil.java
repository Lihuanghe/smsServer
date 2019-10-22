package com.zx.sms.config;

import java.nio.charset.Charset;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zx.sms.common.GlobalConstance;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.EndpointEntity.ChannelType;
import com.zx.sms.connect.manager.cmpp.CMPPEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;
import com.zx.sms.handler.api.BusinessHandlerInterface;
import com.zx.sms.config.SpringContextUtil;
/**
 * @author Lihuanghe(18852780@qq.com) 加载configuration文件
 */
@Component
public class ConfigFileUtil {
	private static DefaultConfigurationBuilder configbuilder = new DefaultConfigurationBuilder();
	private static boolean isLoad = false;
	private static CombinedConfiguration config=null;
	private static final Logger logger = LoggerFactory.getLogger(ConfigFileUtil.class);
	
	@Value("${cmppserver.port:7891}")
	private String port;
	
	@Value("${cmppserver.host:0.0.0.0}")
	private String host;
	
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
			// loadconfiguration("DBSQL.sql");
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
			if("CMPP".equals(server.getString("channelType"))){
				CMPPServerEndpointEntity tmpSever = new CMPPServerEndpointEntity();
				tmpSever.setId(server.getString("id"));
				tmpSever.setDesc(server.getString("desc"));
				tmpSever.setValid(server.getBoolean("isvalid", true));
				tmpSever.setHost(host);
				tmpSever.setPort(Integer.parseInt(port));
				tmpSever.setMaxChannels((short) 0x7fff);
				HierarchicalConfiguration endpoints = server.configurationAt("endpoints");
				List<HierarchicalConfiguration> sessions = endpoints.configurationsAt("endpoint");
				if (sessions.isEmpty())
					break;
				for (HierarchicalConfiguration session : sessions) {
					CMPPServerChildEndpointEntity tmp = new CMPPServerChildEndpointEntity();
					buildCMPPEndpointEntity(session, tmp);
					tmpSever.addchild(tmp);
				}
				result.add(tmpSever);
				return result;
			}
		}
		return null;
	}
	private static void buildCMPPEndpointEntity(HierarchicalConfiguration session, CMPPEndpointEntity tmp) {
		initLoad();
		tmp.setId(session.getString("id"));
		tmp.setDesc(session.getString("desc"));
		tmp.setChannelType(ChannelType.valueOf(ChannelType.class, session.getString("type", "DUPLEX")));
		tmp.setValid(session.getBoolean("isvalid", true));
		tmp.setGroupName(session.getString("group"));
		tmp.setHost(session.getString("host"));
		tmp.setPort(session.getInteger("port", 7891));
		tmp.setUserName(session.getString("user"));
		tmp.setPassword(session.getString("passwd"));
		tmp.setVersion(session.getShort("version", (short) 0x30));
		tmp.setIdleTimeSec(session.getShort("idleTime", (short) 30));
		tmp.setLiftTime(session.getLong("lifeTime", 259200L));
		tmp.setMaxRetryCnt(session.getShort("maxRetry", (short) 3));
		tmp.setRetryWaitTimeSec(session.getShort("retryWaitTime", (short) 60));
		tmp.setMaxChannels(session.getShort("maxChannels"));
//		tmp.setWindows(session.getShort("windows", (short) 3));
		tmp.setChartset(Charset.forName(session.getString("charset", GlobalConstance.defaultTransportCharset.name())));
		tmp.setReSendFailMsg(session.getBoolean("isReSendFailMsg", false));
		tmp.setMaxMsgQueue(session.getShort("maxMsgQueue", (short) 5));
		tmp.setWriteLimit(session.getInt("writeLimit", 0));
		tmp.setReadLimit(session.getInt("readLimit", 0));
		HierarchicalConfiguration handlerSet = session.configurationAt("businessHandlerSet");

		List<Object> handlers = handlerSet.getList("handler");

		List<BusinessHandlerInterface> bizHandlers = new ArrayList<BusinessHandlerInterface>();
		tmp.setBusinessHandlerSet(bizHandlers);

		if (handlers != null && !handlers.isEmpty()) {
			for (Object handler : handlers) {
				if (!tmp.isValid())
					continue;

				if (handler == null)
					continue;
				if (handler instanceof String && StringUtils.isBlank((String) handler)) {
					continue;
				}

				BusinessHandlerInterface obj = getBeanFromCtx((String) handler);
				if (obj != null) {
					bizHandlers.add(obj);
				} 
			}
		}

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
