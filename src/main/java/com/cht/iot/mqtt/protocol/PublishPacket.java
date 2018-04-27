package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PublishPacket extends Packet {	
	protected String topic;
	protected int packetIdentifier;
	protected ByteBuffer message;
	
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
		
		this.topic = Packet.readString(bytes);
		if (this.getQoS() > 0) {
			this.packetIdentifier = bytes.getShort();
		}
		
		this.message = bytes.slice();
		bytes.position(bytes.limit()); // eat them all
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() throws IOException {
		byte[] tp = Packet.toStringBytes(this.topic);
		int qos = this.getQoS();
		
		ByteBuffer bytes = ByteBuffer.allocate(tp.length + ((qos > 0)? 2 : 0) + this.message.remaining());
		
		bytes.put(tp);
		if (qos > 0) {		
			bytes.putShort((short) this.packetIdentifier);
		}
		bytes.put(this.message);
		
		bytes.flip();
		
		return bytes;
	}
	
	@Override
	protected ByteBuffer[] bodies() throws IOException {
		byte[] tp = Packet.toStringBytes(this.topic);
		int qos = this.getQoS();
		
		ByteBuffer head = ByteBuffer.allocate(tp.length + ((qos > 0)? 2 : 0));
		
		head.put(tp);
		if (qos > 0) {		
			head.putShort((short) this.packetIdentifier);
		}
		
		head.flip();
		
		return new ByteBuffer[] { head, this.getMessage() };
	}
	
	// ======
	
	public static final class Topic {
		protected String topicFilter;
		protected int qos;
		
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
