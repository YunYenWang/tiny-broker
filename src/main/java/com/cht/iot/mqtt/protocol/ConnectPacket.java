package com.cht.iot.mqtt.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ConnectPacket extends Packet {
	String protocolName; // 'MQTT'
	int protocolLevel; // 4
	int connectionFlags;
	int keepAlive; // in second
	
	// payload
	String clientId;
	String willTopic;
	String willMessage;
	
	String username;
	String password;
	
	public ConnectPacket() {
		super(Packet.Type.CONNECT);
	}

	public String getProtocolName() {
		return protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
	}

	public int getProtocolLevel() {
		return protocolLevel;
	}

	public void setProtocolLevel(int protocolLevel) {
		this.protocolLevel = protocolLevel;
	}

	public int getConnectionFlags() {
		return connectionFlags;
	}

	public void setConnectionFlags(int connectionFlags) {
		this.connectionFlags = connectionFlags;
	}
	
	public int getKeepAlive() {
		return keepAlive;
	}
	
	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public String getWillTopic() {
		return willTopic;
	}
	
	public String getWillMessage() {
		return willMessage;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public boolean hasUsername() {
		return (connectionFlags & 0x0080) != 0;
	}
	
	public boolean hasPassword() {
		return (connectionFlags & 0x0040) != 0;
	}
	
	public boolean hasWillRetain() {
		return (connectionFlags & 0x0020) != 0;
	}
	
	public int getWillQos() {
		return (connectionFlags & 0x0018) >> 3;
	}
	
	public boolean hasCleanSession() {
		return (connectionFlags & 0x0002) != 0;
	}
	
	// ======
	
	@Override
	public Packet from(ByteBuffer bytes) throws IOException {
		super.from(bytes);
		
		protocolName = Packet.readString(bytes);
		
		protocolLevel = bytes.get() & 0x00FF;
		
		connectionFlags = bytes.get() & 0x00FF;
		
		keepAlive = bytes.getShort();
		
		clientId = Packet.readString(bytes);
		
		if (hasWillRetain()) {
			willTopic = Packet.readString(bytes);
			willMessage = Packet.readString(bytes);
		}
		
		if (hasUsername()) {
			username = Packet.readString(bytes);
		}
		
		if (hasPassword()) {
			password = Packet.readString(bytes);
		}
		
		return this;
	}
	
	@Override
	ByteBuffer body() throws IOException {
		ByteBuffer bytes = ByteBuffer.allocate(64);
		
		bytes.put(Packet.toStringBytes(protocolName));
		bytes.put((byte) protocolLevel);
		bytes.put((byte) connectionFlags);
		bytes.putShort((short) keepAlive);
		bytes.put(Packet.toStringBytes(clientId));
		
		if (hasWillRetain()) {
			bytes.put(Packet.toStringBytes(willTopic));
			bytes.put(Packet.toStringBytes(willMessage));
		}
		
		if (hasUsername()) {
			bytes.put(Packet.toStringBytes(username));
		}
		
		if (hasPassword()) {
			bytes.put(Packet.toStringBytes(password));
		}
		
		bytes.flip();
		
		return bytes;
	}
}
