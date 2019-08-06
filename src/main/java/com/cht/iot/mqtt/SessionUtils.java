package com.cht.iot.mqtt;

import java.net.InetSocketAddress;

import org.apache.mina.core.session.IoSession;

public class SessionUtils {
		
	public static final boolean setup(IoSession session) {
		InetSocketAddress isa = (InetSocketAddress) session.getRemoteAddress();
		if (isa != null) {
			String from = String.format("%s:%d", isa.getAddress().getHostAddress(), isa.getPort());				
			session.setAttribute("from", from);
			
			return true;
		}
		
		return false;
	}
	
	public static final String toString(IoSession session) {
		String from = (String) session.getAttribute("from");
		if (from != null) {
			return from;
			
		} else {
			return session.toString();
		}		
	}
	
	public static final boolean isOkay(IoSession session) {
		return (session != null) && (session.getRemoteAddress() != null); // IoSession.isClosing() could be locked
	}
}
