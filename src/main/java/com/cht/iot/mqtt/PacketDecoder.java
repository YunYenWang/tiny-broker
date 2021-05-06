package com.cht.iot.mqtt;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.cht.iot.mqtt.protocol.Packet;
import com.cht.iot.mqtt.protocol.PacketBuilder;

public class PacketDecoder extends CumulativeProtocolDecoder {
	final PacketBuilder builder = new PacketBuilder();	

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		if (in.remaining() < 2) {
			return false; // not enough
		}
		
		int start = in.position(); // 1 type + maximum 4 bytes remaining length
		
		int type = in.get() & 0x0FF;		
		
		int length = 0;
		int b = 0;
		int m = 1;
		do {
			if (in.hasRemaining() == false) {
				in.position(start);
				return false; // not enough
			}
			
			b = in.get() & 0x0FF;
			length += ((b & 0x07F) * m); // 7 bits
			m *= 0x080;
			
		} while ((b & 0x080) != 0); // high bit is on
		// FIXME - maximum 4 bytes remaining length
		
		if (in.remaining() < length) {
			in.position(start);
			return false; // not enough
		}
		
		int s = in.position() + length; // type + length + body
		
		ByteBuffer bb = ByteBuffer.allocate(s);
		in.position(start);
		
		for (int i = 0;i < s;i++) {
			bb.put(in.get());
		}
		
		bb.flip();
		
		Packet pkt = builder.build(bb);
		
		out.write(pkt);

		return true; // find the next packet
	}

}
