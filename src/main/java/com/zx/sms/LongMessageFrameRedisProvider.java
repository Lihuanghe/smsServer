package com.zx.sms;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.zx.sms.codec.LongMessageFrameCache;
import com.zx.sms.codec.LongMessageFrameProvider;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;

public class LongMessageFrameRedisProvider implements LongMessageFrameProvider {

	private ConcurrentMap<String, List<LongMessageFrame>> map =  new ConcurrentHashMap<String, List<LongMessageFrame>>();
	
	@Override
	public LongMessageFrameCache create() {
		return new LongMessageFrameRedisCache();
	}

	@Override
	public int order() {
		return 1;
	}

	
	private class LongMessageFrameRedisCache implements LongMessageFrameCache {

		@Override
		public List<LongMessageFrame> get(String key) {
			return map.get(key);
		}

		@Override
		public void remove(String key) {
			map.remove(key);
		}

		@Override
		public void set(String key, List<LongMessageFrame> list, LongMessageFrame currFrame) {
			map.put(key, list);
		}
		
	}
}
