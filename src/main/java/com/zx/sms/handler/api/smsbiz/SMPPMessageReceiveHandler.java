package com.zx.sms.handler.api.smsbiz;

import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Component;

import com.zx.sms.codec.smpp.msg.DeliverSm;
import com.zx.sms.codec.smpp.msg.DeliverSmReceipt;
import com.zx.sms.codec.smpp.msg.DeliverSmResp;
import com.zx.sms.codec.smpp.msg.SubmitSm;
import com.zx.sms.codec.smpp.msg.SubmitSmResp;
import com.zx.sms.common.util.ChannelUtil;
import com.zx.sms.common.util.MsgId;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
@Component
public class SMPPMessageReceiveHandler extends MessageReceiveHandler {

	@Override
	protected ChannelFuture reponse(final ChannelHandlerContext ctx, Object msg)  {
		
		if(msg instanceof DeliverSmReceipt) {
			DeliverSmResp res = ((DeliverSm) msg).createResponse();
			return ctx.writeAndFlush(res);
			
		}else if (msg instanceof DeliverSm ) {
			DeliverSmResp res = ((DeliverSm) msg).createResponse();
			return ctx.writeAndFlush(res);
		} else if (msg instanceof SubmitSm) {
			SubmitSmResp res = ((SubmitSm) msg).createResponse();
			res.setMessageId((new MsgId()).toString());
			ChannelFuture future = ctx.writeAndFlush(res);

			if (((SubmitSm) msg).getRegisteredDelivery() == 1) {
				DeliverSmReceipt report = new DeliverSmReceipt();
				report.setId(res.getMessageId());
				report.setSourceAddress(((SubmitSm) msg).getDestAddress());
				report.setDestAddress(((SubmitSm) msg).getSourceAddress());
				report.setStat("DELIVRD");
				report.setText("");
				report.setErr("");
				report.setSub("");
				report.setDlvrd("");
				report.setSubmit_date(DateFormatUtils.format(new Date(), "yyMMddHHmm"));
				report.setDone_date(DateFormatUtils.format(new Date(), "yyMMddHHmm"));
				try {
					ChannelUtil.syncWriteLongMsgToEntity(getEndpointEntity(), report);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return future;
		}
		return null;
	}

}
