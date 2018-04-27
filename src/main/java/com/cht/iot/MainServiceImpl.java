package com.cht.iot;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cht.iot.mqtt.MyBrokerImpl;

@Service
public class MainServiceImpl {
	static final Logger LOG = LoggerFactory.getLogger(MainServiceImpl.class);
		
	MyBrokerImpl broker;
	
	public MainServiceImpl() {
	}
	
	// ======
	
	@PostConstruct
	void start() throws Exception {
		broker = new MyBrokerImpl();		
		broker.start();
	}
	
	@PreDestroy
	void stop() throws Exception {
		broker.stop();
	}
}
