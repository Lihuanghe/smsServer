package com.zx.sms.handler.api.smsbiz;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

@Sharable
@Component
public class CmppEchoDeliverHandler extends AbstractBusinessHandler {

	@Override
	public String name() {
		return "CmppEchoDeliverHandler";
	}
	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		 if (msg instanceof CmppSubmitRequestMessage) {
				//接收到 CmppSubmitRequestMessage 消息
				CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;
				
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
					
					ctx.channel().writeAndFlush(msgdeli);
				}
		 }
		 ctx.fireChannelRead(msg);
	}
}
