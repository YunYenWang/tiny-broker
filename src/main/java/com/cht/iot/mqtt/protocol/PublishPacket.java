package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PublishPacket extends Packet {	
	String topic;
	int packetIdentifier;
	ByteBuffer message;
	
	public PublishPacket() {
		super(Packet.Type.PUBLISH);
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}
	
	public ByteBuffer getMessage() {
		return message.slice();
	}
	
	public void setMessage(ByteBuffer message) {
		this.message = message;
	}
	
	public void setMessage(byte[] message) {
		this.message = ByteBuffer.wrap(message);
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		topic = Packet.readString(bytes);
		if (getQoS() > 0) {
			packetIdentifier = bytes.getShort();
		}
		
		message = bytes.slice();
		bytes.position(bytes.limit()); // eat them all
		
		return this;
	}
	
	@Override
	ByteBuffer body() throws IOException {
		byte[] tp = Packet.toStringBytes(topic);
		int qos = getQoS();
		
		ByteBuffer bytes = ByteBuffer.allocate(tp.length + ((qos > 0)? 2 : 0) + message.remaining());
		
		bytes.put(tp);
		if (qos > 0) {		
			bytes.putShort((short) packetIdentifier);
		}
		bytes.put(message);
		
		bytes.flip();
		
		return bytes;
	}
	
	// ======
	
	public static final class Topic {
		String topicFilter;
		int qos;
		
		public Topic() {		
		}
		
		public Topic(String topicFilter, int qos) {
			this.topicFilter = topicFilter;
			this.qos = qos;
		}
		
		public String getTopicFilter() {
			return topicFilter;
		}
		
		public void setTopicFilter(String topicFilter) {
			this.topicFilter = topicFilter;
		}
		
		public int getQos() {
			return qos;
		}
		
		public void setQos(int qos) {
			this.qos = qos;
		}
	}
}
