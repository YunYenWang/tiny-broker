package com.cht.iot.mqtt;

public class PermissionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PermissionException() {
		super("");
	}
	
	public PermissionException(String message) {
		super(message);
	}
}
