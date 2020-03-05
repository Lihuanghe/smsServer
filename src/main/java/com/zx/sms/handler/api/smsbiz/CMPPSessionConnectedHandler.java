package com.zx.sms.handler.api.smsbiz;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.marre.sms.SmsDcs;
import org.marre.sms.SmsMessage;
import org.marre.sms.SmsTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zx.sms.BaseMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppQueryRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppQueryResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.common.util.CachedMillisecondClock;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.config.ConfigFileUtil;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.ServerEndpoint;
import com.zx.sms.handler.api.SessionConnectedHandler;

import io.netty.channel.ChannelHandlerContext;
@Component
public class CMPPSessionConnectedHandler extends SessionConnectedHandler {
	private static final Logger logger = LoggerFactory.getLogger(CMPPSessionConnectedHandler.class);

	@Autowired
	ConfigFileUtil config ;
	
	@Override
	protected BaseMessage createTestReq(String content) {
		final EndpointEntity finalentity = getEndpointEntity();
		SmsMessage msgcontent = new SmsTextMessage(config.getTestSMS(),new SmsDcs(config.getDcs()));
		logger.info("send msg : {}",config.getTestSMS());
		if (finalentity instanceof ServerEndpoint) {
			CmppDeliverRequestMessage msg = new CmppDeliverRequestMessage();
			msg.setDestId("10086");
			msg.setMsgContent(msgcontent);
			msg.setMsgId(new MsgId());
			
			msg.setServiceid("10086");
			msg.setSrcterminalId(config.getTelephone());
			msg.setSrcterminalType((short) 1);
			
			return msg;
		} else {
			CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
			msg.setDestterminalId(config.getTelephone());
			msg.setMsgContent(msgcontent);
			msg.setRegisteredDelivery((short)1);
			return msg;
		}
	}
	
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {

		if (msg instanceof CmppDeliverRequestMessage) {
			CmppDeliverRequestMessage e = (CmppDeliverRequestMessage) msg;
			
			if(e.getFragments()!=null) {
				//长短信会带有片断
				for(CmppDeliverRequestMessage frag:e.getFragments()) {
					CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(frag.getHeader().getSequenceId());
					responseMessage.setResult(0);
					responseMessage.setMsgId(frag.getMsgId());
					ctx.channel().write(responseMessage);
				}
			}
			
			CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(e.getHeader().getSequenceId());
			responseMessage.setResult(0);
			responseMessage.setMsgId(e.getMsgId());
			ctx.channel().writeAndFlush(responseMessage);

		} else if (msg instanceof CmppDeliverResponseMessage) {
			CmppDeliverResponseMessage e = (CmppDeliverResponseMessage) msg;

		} else if (msg instanceof CmppSubmitRequestMessage) {
			//接收到 CmppSubmitRequestMessage 消息
			CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;
			
			final List<CmppDeliverRequestMessage> reportlist = new ArrayList<CmppDeliverRequestMessage>();
			
			if(e.getFragments()!=null) {
				//长短信会可能带有片断，每个片断都要回复一个response
				for(CmppSubmitRequestMessage frag:e.getFragments()) {
					CmppSubmitResponseMessage responseMessage = new CmppSubmitResponseMessage(frag.getHeader().getSequenceId());
					responseMessage.setResult(0);
					ctx.channel().write(responseMessage);
					
					CmppDeliverRequestMessage deliver = new CmppDeliverRequestMessage();
					deliver.setDestId(e.getSrcId());
					deliver.setSrcterminalId(e.getDestterminalId()[0]);
					CmppReportRequestMessage report = new CmppReportRequestMessage();
					report.setDestterminalId(deliver.getSrcterminalId());
					report.setMsgId(responseMessage.getMsgId());
					String t = DateFormatUtils.format(CachedMillisecondClock.INS.now(), "yyMMddHHmm");
					report.setSubmitTime(t);
					report.setDoneTime(t);
					report.setStat("DELIVRD");
					report.setSmscSequence(0);
					deliver.setReportRequestMessage(report);
					reportlist.add(deliver);
				}
			}
			
			final CmppSubmitResponseMessage resp = new CmppSubmitResponseMessage(e.getHeader().getSequenceId());
			resp.setResult(0);
			
			ctx.channel().writeAndFlush(resp);
			
			//回复状态报告
			if(e.getRegisteredDelivery()==1) {
				
				final CmppDeliverRequestMessage deliver = new CmppDeliverRequestMessage();
				deliver.setDestId(e.getSrcId());
				deliver.setSrcterminalId(e.getDestterminalId()[0]);
				CmppReportRequestMessage report = new CmppReportRequestMessage();
				report.setDestterminalId(deliver.getSrcterminalId());
				report.setMsgId(resp.getMsgId());
				String t = DateFormatUtils.format(CachedMillisecondClock.INS.now(), "yyMMddHHmm");
				report.setSubmitTime(t);
				report.setDoneTime(t);
				report.setStat("DELIVRD");
				report.setSmscSequence(0);
				deliver.setReportRequestMessage(report);
				reportlist.add(deliver);
				
				ctx.executor().submit(new Runnable() {
					public void run() {
						for(CmppDeliverRequestMessage t : reportlist)
							ctx.channel().writeAndFlush(t);
					}
				});
			}
			
		} else if (msg instanceof CmppSubmitResponseMessage) {
			CmppSubmitResponseMessage e = (CmppSubmitResponseMessage) msg;
		} else if (msg instanceof CmppQueryRequestMessage) {
			CmppQueryRequestMessage e = (CmppQueryRequestMessage) msg;
			CmppQueryResponseMessage res = new CmppQueryResponseMessage(e.getHeader().getSequenceId());
			ctx.channel().writeAndFlush(res);
		} else {
			ctx.fireChannelRead(msg);
		}
	}
}
