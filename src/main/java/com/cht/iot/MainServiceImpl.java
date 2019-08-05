package com.cht.iot;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.mina.core.service.IoAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cht.iot.mqtt.MqttSlave;
import com.cht.iot.mqtt.MyBroker;
import com.cht.iot.mqtt.MyBrokerImpl;

@Service
public class MainServiceImpl {
	static final Logger LOG = LoggerFactory.getLogger(MainServiceImpl.class);

	@Value("${broker.executor-core-pool-size:100}")
	int executorCorePoolSize;
	
	@Value("${broker.executor-max-pool-size:1000}")
	int executorMaxPoolSize;
	
	@Value("${broker.session-idle-timeout-in-seconds:90}")
	int timeout;
	
	MyBrokerImpl broker;
	
	@Autowired(required = false)
	MyBroker.Listener listener;
	
	public MainServiceImpl() {
	}
	
	// ======
	
	@PostConstruct
	void start() throws Exception {
		broker = new MyBrokerImpl();
		broker.setExecutorCorePoolSize(executorCorePoolSize);
		broker.setExecutorMaxPoolSize(executorMaxPoolSize);
		broker.setIdleTimeout(timeout);
		
		if (listener != null) {
			LOG.info("Set Listener - {}", listener);			
			broker.setListener(listener);
		}
		
		LOG.info("ExecutorCorePoolSize: {}, ExecutorMaxPoolSize: {}, IdleTimeout: {}s", executorCorePoolSize, executorMaxPoolSize, timeout);
		
		broker.start();
	}
	
	@PreDestroy
	void stop() throws Exception {
		broker.stop();
	}
}
