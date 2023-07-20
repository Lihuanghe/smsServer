package com.zx.sms.handler.api.smsbiz;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipDeliverRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitRequestMessage;
import com.zx.sms.codec.smgp.msg.SMGPDeliverMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.codec.smpp.msg.DeliverSm;
import com.zx.sms.codec.smpp.msg.SubmitSm;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

@Sharable
@Component
public class EchoDeliverHandler extends AbstractBusinessHandler {

	@Override
	public String name() {
		return "CmppEchoDeliverHandler";
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof CmppSubmitRequestMessage) {
			// 接收到 CmppSubmitRequestMessage 消息
			CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;

			// echo指令回复上行短信

			String command = e.getMsgContent();

			if (StringUtils.isNotBlank(command) && command.startsWith("echo")) {
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
		} else if (msg instanceof SubmitSm) {
			SubmitSm smppSubmit = (SubmitSm) msg;
			String command = smppSubmit.getMsgContent();
			if (StringUtils.isNotBlank(command) && command.startsWith("echo")) {
				String deliCommand = command.substring(4);
				DeliverSm deliver = new DeliverSm();
				deliver.setSourceAddress(smppSubmit.getDestAddress());
				deliver.setDestAddress(smppSubmit.getSourceAddress());
				deliver.setSmsMsg(deliCommand);
				ctx.channel().writeAndFlush(deliver);
			}

		} else if (msg instanceof SMGPSubmitMessage) {
			SMGPSubmitMessage smgpSubmit = (SMGPSubmitMessage) msg;
			String command = smgpSubmit.getMsgContent();
			if (StringUtils.isNotBlank(command) && command.startsWith("echo")) {
				String deliCommand = command.substring(4);
				SMGPDeliverMessage pdu = new SMGPDeliverMessage();
				pdu.setDestTermId(smgpSubmit.getSrcTermId());
				pdu.setMsgContent(deliCommand);
				pdu.setSrcTermId(smgpSubmit.getDestTermIdArray()[0]);
				ctx.channel().writeAndFlush(pdu);
			}
		} else if (msg instanceof SgipSubmitRequestMessage) {
			SgipSubmitRequestMessage sgipSubmit = (SgipSubmitRequestMessage) msg;
			String command = sgipSubmit.getMsgContent();
			if (StringUtils.isNotBlank(command) && command.startsWith("echo")) {
				String deliCommand = command.substring(4);
				SgipDeliverRequestMessage sgipmsg = new SgipDeliverRequestMessage();
				sgipmsg.setUsernumber(sgipSubmit.getUsernumber()[0]);
				sgipmsg.setSpnumber(sgipSubmit.getSpnumber());
				sgipmsg.setMsgContent(deliCommand);
				ctx.channel().writeAndFlush(sgipmsg);
			}
		}
		ctx.fireChannelRead(msg);
	}
}
