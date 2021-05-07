package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketBuilder {	
	
	public Packet build(ByteBuffer bytes) throws IOException {
		Packet.Type type = Packet.TYPES[(bytes.get(0) & 0x0F0) >> 4];
		
		if (type == Packet.Type.PUBLISH) {
			return new PublishPacket().from(bytes);

		} else if (type == Packet.Type.PUBACK) {
			return new PubackPacket().from(bytes);
			
		} else if (type == Packet.Type.PUBREC) {
			return new PubrecPacket().from(bytes);
			
		} else if (type == Packet.Type.PUBCOMP) {
			return new PubcompPacket().from(bytes);			
			
		} else if (type == Packet.Type.CONNECT) {
			return new ConnectPacket().from(bytes);
			
		} else if (type == Packet.Type.CONNACK) {
			return new ConnackPacket().from(bytes);
			
		} else if (type == Packet.Type.SUBSCRIBE) {
			return new SubscribePacket().from(bytes);
			
		} else if (type == Packet.Type.SUBACK) {
			return new SubackPacket().from(bytes);
			
		} else if (type == Packet.Type.UNSUBSCRIBE) {
			return new UnsubscribePacket().from(bytes);
			
		} else if (type == Packet.Type.PINGREQ) {
			return new PingreqPacket().from(bytes);
			
		} else if (type == Packet.Type.DISCONNECT) {
			return new DisconnectPacket().from(bytes);
			
		} else {		
			return new UnknownPacket().from(bytes);
		}
	}	
}
