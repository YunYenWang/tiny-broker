package com.cht.iot.mqtt;

import java.nio.ByteBuffer;

public class Message {
	final int from;
	final String topic;
	final ByteBuffer payload;	
	
	public Message(int from, String topic, ByteBuffer payload) {
		this.from = from;
		this.topic = topic;
		this.payload = payload;
	}
	
	public int getFrom() {
		return from;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public ByteBuffer getPayload() {
		return payload.slice();
	}
}
