package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DisconnectPacket extends Packet {
	
	public DisconnectPacket() {
		super(Packet.Type.DISCONNECT);
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		return this;
	}
	
	@Override
	ByteBuffer body() {
		throw new UnsupportedOperationException("not yet implemented");
	}
}
