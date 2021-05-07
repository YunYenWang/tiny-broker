package com.cht.iot.mqtt;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

public class MqttSlave {
	final IoSession session;
	
	final String uid; // session id	
	final String connection;
	final long birthday;
	
	Account account;
	String clientId;
	
	Set<String> topics = new HashSet<>(); // subscription topics
	
	public MqttSlave(IoSession session) {
		this.session = session;
		
		uid = String.format("slave-%d", session.getId());
		connection = SessionUtils.toString(session);
		birthday = System.currentTimeMillis();
	}
	
	public IoSession getSession() {
		return session;
	}
	
	public String getConnection() {
		return connection;
	}
	
	public long getBirthday() {
		return birthday;
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
	
	public Set<String> getTopics() {
		return topics;
	}
	
	// ======
	
	public void close() {
		session.closeOnFlush();
	}
	
	public void write(byte[] bytes) {
		session.write(ByteBuffer.wrap(bytes));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MqttSlave) {
			MqttSlave s = (MqttSlave) obj;			
			return uid.equals(s.uid);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {		
		return uid.hashCode();
	}
	
	@Override
	public String toString() {			
		return String.format("account: %s, clientId: %s, from: %s",
				((account != null) && (account.getUsername() != null))? account.getUsername() : "",
				(clientId != null)? clientId : "",
				connection);
	}	
}
