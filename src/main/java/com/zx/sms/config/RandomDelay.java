package com.zx.sms.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RandomDelay {
	
	@Autowired
	private Environment env;
	
	private ReentrantLock lock = new ReentrantLock();

	private final static class Node {
		private final int weight;
		private int currWeight;
		private Integer[] rd;

		public Node(int weight, Integer[] d) {
			this.weight = weight;
			this.rd = d;
		}
	}

	private List<Node> nodes = new ArrayList<Node>();

	@PostConstruct
	private void init() {
		String responseDelay = env.getProperty("responseDelay", "1:-");
		String[] data = responseDelay.split(",");
		for (String d : data) {
			Integer[] rd = new Integer[2];
			String[] m = d.split(":");
			int weight = Integer.valueOf(m[0]);
			if (m[1].equals("-")) {
				rd[0] = -1;
				rd[1] = -1;
			} else {
				String[] r = m[1].split("-");
				int r1 = Integer.valueOf(r[0]);
				int r2 = Integer.valueOf(r[1]);
				rd[0] = r1 > r2 ? r2 : r1;
				rd[1] = r1 > r2 ? r1 : r2;

			}
			Node node = new Node(weight, rd);
			nodes.add(node);
		}
	}

	private Node select() {

		int totalWeight = 0;
		Node maxNode = null;
		int maxWeight = 0;
		for (Node n : nodes) {
			totalWeight += n.weight;
			n.currWeight += n.weight;

			if (maxNode == null || maxWeight < n.currWeight) {
				maxNode = n;
				maxWeight = n.currWeight;
			}
		}
		maxNode.currWeight -= totalWeight;
		return maxNode;
	}

	public int delay() {
		Node s;
		try {
			lock.lock();
			s = select();
		} finally {
			lock.unlock();
		}

		if (s.rd[0] < 0)
			return -1;
		else {
			return RandomUtils.nextInt(s.rd[0], s.rd[1]);
		}
	}
}
