package com.cht.iot.mqtt.protocol;

import java.nio.ByteBuffer;

public class UnsubackPacket extends Packet {
	protected int packetIdentifier;
	
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
	protected ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(2);
		
		bytes.putShort((short) packetIdentifier);
		
		bytes.flip();
		
		return bytes;
	}
}
