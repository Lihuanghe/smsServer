package com.zx.sms.handler.api.smsbiz;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Component;

import com.zx.sms.codec.smgp.msg.SMGPDeliverMessage;
import com.zx.sms.codec.smgp.msg.SMGPDeliverRespMessage;
import com.zx.sms.codec.smgp.msg.SMGPReportData;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.codec.smgp.msg.SMGPSubmitRespMessage;
import com.zx.sms.common.util.CachedMillisecondClock;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
@Component
public class SMGPMessageReceiveHandler extends MessageReceiveHandler {

	@Override
	protected ChannelFuture reponse(final ChannelHandlerContext ctx, Object msg) {
		
		if(msg instanceof SMGPDeliverMessage){
			SMGPDeliverRespMessage resp = new SMGPDeliverRespMessage();
		    resp.setSequenceNo(((SMGPDeliverMessage)msg).getSequenceNo());
		    resp.setMsgId( ((SMGPDeliverMessage)msg).getMsgId());
		    resp.setStatus(0);
		  
			return ctx.writeAndFlush(resp);
		}else if(msg instanceof SMGPSubmitMessage) {
			SMGPSubmitMessage  smgpsubmit = (SMGPSubmitMessage)msg;
			SMGPSubmitRespMessage resp = new SMGPSubmitRespMessage();
			resp.setSequenceNo(smgpsubmit.getSequenceNo());
		    resp.setStatus(0);
		    
			if(smgpsubmit.isNeedReport()) {
				SMGPDeliverMessage deliReport = new SMGPDeliverMessage();
				deliReport.setSrcTermId(smgpsubmit.getDestTermIdArray()[0]);
				deliReport.setDestTermId(smgpsubmit.getSrcTermId());
				SMGPReportData reportData = new SMGPReportData();
				reportData.setMsgId(resp.getMsgId());
				String t = DateFormatUtils.format(CachedMillisecondClock.INS.now(), "yyMMddHHmm");
				reportData.setSubTime(t);
				reportData.setDoneTime(t);
				reportData.setStat("DELIVRD");
				deliReport.setReport(reportData);
				ctx.writeAndFlush(deliReport);
			}
			
		
			return ctx.writeAndFlush(resp);
		}
		return null;
	}

}
