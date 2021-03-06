package com.zx.sms.handler.api.smsbiz;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Component;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.common.util.CachedMillisecondClock;
import com.zx.sms.common.util.ChannelUtil;
import com.zx.sms.common.util.MsgId;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
@Component
public class CMPPMessageReceiveHandler extends MessageReceiveHandler {

	@Override
	protected ChannelFuture reponse(final ChannelHandlerContext ctx, Object msg) {
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
			return ctx.channel().writeAndFlush(responseMessage);

		}else if (msg instanceof CmppSubmitRequestMessage) {
			//接收到 CmppSubmitRequestMessage 消息
			CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;
			
			final List<CmppDeliverRequestMessage> reportlist = new ArrayList<CmppDeliverRequestMessage>();
			
			//echo指令回复上行短信 
			
			String command = e.getMsgContent();
			
			if(StringUtils.isNotBlank(command) && command.startsWith("echo")) {
				String deliCommand = command.substring(4);
				CmppDeliverRequestMessage msgdeli = new CmppDeliverRequestMessage();
				msgdeli.setDestId(e.getSrcId());
				msgdeli.setLinkid("0000");
				msgdeli.setMsgContent(deliCommand.trim());
				msgdeli.setMsgId(new MsgId());
				
				msgdeli.setServiceid("10086");
				msgdeli.setSrcterminalId(e.getDestterminalId()[0]);
//				msgdeli.setSrcterminalType((short) 1);
				
				
				ctx.channel().writeAndFlush(msgdeli);
			}
			
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
			
			ChannelFuture future = ctx.channel().writeAndFlush(resp);
			
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
			return  future ;
		}
		return null;
	}
	
	
	

}
