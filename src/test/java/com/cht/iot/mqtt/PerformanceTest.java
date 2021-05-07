package com.cht.iot.mqtt;

import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceTest {	

	String serverURI = "tcp://localhost:1883";

	int clients = 100;
	int messages = 1000;
	
	byte[] message = new byte[1024];	
	
	CountDownLatch latch;
	
	@Test
	void test() throws Exception {
		try (MyBrokerImpl broker = new MyBrokerImpl()) {
			broker.setPort(1883);
			broker.setAuthenticationTimeout(5); // seconds
			broker.setIdleTimeout(90); // client's idle time-out in seconds
			broker.setListener(new MyBroker.Listener() {
	
				@Override
				public boolean challenge(MqttSlave slave, String username, String password) {
					return true; // accept the anonymous
				}
	
				@Override
				public void onSlaveArrived(MqttSlave slave) {					
				}
	
				@Override
				public void onSlaveExited(MqttSlave slave) {
				}
	
				@Override
				public void onSubscribe(MqttSlave slave, String topic) {
				}
	
				@Override
				public void onMessage(MqttSlave slave, String topic, byte[] payload) {
				}
				
			});
			broker.start();
			
			String topic = "test";
			
			for (int i = 0;i < clients;i++) {
				MqttClient client = newMqttClient();
				client.subscribe(topic);
			}
			
			int count = clients * messages;
			
			latch = new CountDownLatch(count);
			
			long ctm = System.currentTimeMillis();
			
			MqttClient client = newMqttClient();
			for (int i = 0;i < messages;i++) {
				client.publish(topic, new MqttMessage(message));
			}
			
			latch.await();
			
			long elapse = System.currentTimeMillis() - ctm;
			
			log.info("elapse: {} ms, rate: {} messages / second", elapse, count * 1000 / elapse);			
		}
	}
	
	MqttClient newMqttClient() throws MqttException {
		String clientId = MqttClient.generateClientId();
		MqttClientPersistence persistence = new MemoryPersistence();
		
		MqttClient client = new MqttClient(serverURI, clientId, persistence);
		client.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				latch.countDown();
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
			}
			
		});
		
		MqttConnectOptions opt = new MqttConnectOptions();		
		opt.setUserName("");
		opt.setPassword(new char[0]);
		opt.setConnectionTimeout(60);
		opt.setKeepAliveInterval(60);
		opt.setCleanSession(true);
		
		client.connect(opt);
		
		return client;
	}
}
