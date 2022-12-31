package com.zx.sms.handler.api.smsbiz;

import org.springframework.stereotype.Component;

import com.zx.sms.codec.sgip12.msg.SgipDeliverRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipDeliverResponseMessage;
import com.zx.sms.codec.sgip12.msg.SgipReportRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitRequestMessage;
import com.zx.sms.codec.sgip12.msg.SgipSubmitResponseMessage;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
@Component
public class SGIPMessageReceiveHandler extends MessageReceiveHandler {

	@Override
	protected ChannelFuture reponse(final ChannelHandlerContext ctx, Object msg) {
		
		if(msg instanceof SgipDeliverRequestMessage){
			SgipDeliverRequestMessage deli = (SgipDeliverRequestMessage)msg;
			SgipDeliverResponseMessage resp = new SgipDeliverResponseMessage();
			resp.setSequenceNo(deli.getHeader().getSequenceId());
			resp.getHeader().setNodeId(deli.getHeader().getNodeId());
			resp.setResult((short)0);
			resp.setTimestamp(deli.getTimestamp());
			
			return ctx.writeAndFlush(resp);
		}else if(msg instanceof SgipSubmitRequestMessage) {
			
			SgipSubmitRequestMessage submit = (SgipSubmitRequestMessage)msg;
			SgipSubmitResponseMessage resp = new SgipSubmitResponseMessage();
			resp.setSequenceNo(submit.getHeader().getSequenceId());
			resp.getHeader().setNodeId(submit.getHeader().getNodeId());
			resp.setTimestamp(submit.getTimestamp());
			resp.setResult((short)0);
			
			boolean sendreport = 1 == submit.getReportflag();
			
			ChannelFuture future =  ctx.writeAndFlush(resp);
			if(sendreport) {
				SgipReportRequestMessage report = new SgipReportRequestMessage();
				report.setSequenceId(resp.getSequenceNumber());
				ctx.writeAndFlush(report);
			}

			return future;
		}
		return null;
	}

}
