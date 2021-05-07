package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PubrecPacket extends Packet {
	int packetIdentifier;
	
	public PubrecPacket() {
		super(Packet.Type.PUBREC);
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}
	
	// ======	
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		packetIdentifier = bytes.getShort();
		
		return this;
	}
	
	@Override
	ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(2);
		
		bytes.putShort((short) packetIdentifier);
		
		bytes.flip();
		
		return bytes;
	}	
}
