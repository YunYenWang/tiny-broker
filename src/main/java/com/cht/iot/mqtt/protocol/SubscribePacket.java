package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SubscribePacket extends Packet {
	protected int packetIdentifier;
	protected List<Topic> topics;
	
	public SubscribePacket() {
		super(Packet.Type.SUBSCRIBE);
		setFlags(0x02);
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}
	
	public List<Topic> getTopics() {
		return topics;
	}
	
	public void setTopics(List<Topic> topics) {
		this.topics = topics;
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		packetIdentifier = bytes.getShort();
		
		topics = new ArrayList<Topic>();
		
		while (bytes.hasRemaining()) {
			String topicFilter = Packet.readString(bytes);
			int qos = bytes.get();
			
			Topic topic = new Topic(topicFilter, qos);
			topics.add(topic);
		}		
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(128); // FIXME - enough ?
		
		bytes.putShort((short) packetIdentifier);
		for (Topic topic : topics) {
			bytes.put(Packet.toStringBytes(topic.getTopicFilter()));
			bytes.put((byte) topic.getQos());
		}
		
		bytes.flip();
		
		return bytes;
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
