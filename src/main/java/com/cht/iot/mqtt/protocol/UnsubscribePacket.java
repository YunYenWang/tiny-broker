package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class UnsubscribePacket extends Packet {
	protected int packetIdentifier;
	protected List<String> topicFilters;
	
	public UnsubscribePacket() {
		super(Packet.Type.UNSUBSCRIBE);
		setFlags(0x02);
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}

	public List<String> getTopicFilters() {
		return topicFilters;
	}
	
	public void setTopicFilters(List<String> topicFilters) {
		this.topicFilters = topicFilters;
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		packetIdentifier = bytes.getShort();
		
		topicFilters = new ArrayList<String>();
		
		while (bytes.hasRemaining()) {
			String topicFilter = Packet.readString(bytes);
			topicFilters.add(topicFilter);
		}		
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		throw new UnsupportedOperationException("not yet implemented");
	}
}
