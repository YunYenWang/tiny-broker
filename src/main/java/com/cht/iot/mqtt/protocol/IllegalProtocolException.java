package com.cht.iot.mqtt.protocol;

import java.io.IOException;

public class IllegalProtocolException extends IOException {
	private static final long serialVersionUID = 1L;

	public IllegalProtocolException() {	
	}
	
	public IllegalProtocolException(String message) {
		super(message);
	}
}
