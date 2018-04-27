package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class UnknownPacket extends Packet {
	protected byte[] payload;
	
	public UnknownPacket() {
		super(Packet.Type.RESERVED);
	}
	
	public byte[] getPayload() {
		return payload;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		byte h = bytes.get();
		
		this.type = (h & 0x0F0) >> 4;
		this.flags = h & 0x00F;		
		
		this.length = readRemainingLength(bytes);
		
		this.payload = new byte[bytes.remaining()];
		bytes.get(this.payload);
		
		return this;
	}
	
	@Override
	protected ByteBuffer body() {
		throw new UnsupportedOperationException("not yet implemented");
	}
}