package com.cht.iot.mqtt;

import java.io.IOException;

public interface MyBroker {

	/**
	 * 
	 * @param topic
	 * @param payload
	 * @throws IOException
	 */
	void publish(String topic, byte[] payload) throws IOException;
	
	interface Listener {
		
		/**
		 * 
		 * @param slave
		 * @param username
		 * @param password
		 * @return
		 */
		boolean challenge(MqttSlave slave, String username, String password);
		
		/**
		 * 
		 * @param slave
		 */
		void onSlaveArrived(MqttSlave slave);

		/**
		 * 
		 * @param slave
		 */
		void onSlaveExited(MqttSlave slave);
		
		/**
		 * 
		 * @param slave
		 * @param topic
		 */
		void onSubscribe(MqttSlave slave, String topic);
		
		/**
		 * 
		 * @param slave
		 * @param topic
		 * @param payload
		 */
		void onMessage(MqttSlave slave, String topic, byte[] payload);
	}
}
