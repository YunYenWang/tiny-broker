package com.cht.iot.mqtt.protocol;

import java.nio.ByteBuffer;

public class UnsubackPacket extends Packet {
	int packetIdentifier;
	
	public UnsubackPacket() {
		super(Packet.Type.UNSUBACK);
	}
	
	public int getPacketIdentifier() {
		return packetIdentifier;
	}
	
	public void setPacketIdentifier(int packetIdentifier) {
		this.packetIdentifier = packetIdentifier;
	}
	
	// ======
	
	
	@Override
	ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(2);
		
		bytes.putShort((short) packetIdentifier);
		
		bytes.flip();
		
		return bytes;
	}
}
