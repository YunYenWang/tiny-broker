package com.cht.iot.mqtt;

import org.apache.mina.core.session.IoSession;

import com.cht.iot.mqtt.protocol.PacketBuilder;

public class Client {
	final String id; // session id
	final IoSession session;
	final String connection;
	Account account;
	String clientId;
	
	final PacketBuilder builder;
	
	int transaction = 1;
			
	public Client(IoSession session, int packetBufferInitialSize) {
		this.session = session;	
		
		connection = MyUtils.toString(session);
		id = String.format("mqtt-%d", session.getId());
		
		builder = new PacketBuilder(packetBufferInitialSize);
	}
	
	public String getId() {
		return id;
	}
	
	public IoSession getSession() {
		return session;
	}
	
	public String getConnection() {
		return connection;
	}
	
	public Account getAccount() {
		return account;
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public PacketBuilder getPacketBuilder() {
		return builder;
	}
	
	public int nextTransaction() {
		return transaction ++;
	}
	
	@Override
	public String toString() {
		return String.format("account: %s, clientId: %s, from: %s",
				(account != null)? account.getUsername() : "",
				(clientId != null)? clientId : "",
				connection);		
	}
}
