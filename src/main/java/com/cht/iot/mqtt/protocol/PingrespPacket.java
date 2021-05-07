package com.cht.iot.mqtt.protocol;

import java.nio.ByteBuffer;

public class PingrespPacket extends Packet {
	
	public PingrespPacket() {
		super(Packet.Type.PINGRESP);
	}
	
	// ======	
	
	@Override
	ByteBuffer body() {
		ByteBuffer bytes = ByteBuffer.allocate(0);
		
		return bytes;
	}	
}
