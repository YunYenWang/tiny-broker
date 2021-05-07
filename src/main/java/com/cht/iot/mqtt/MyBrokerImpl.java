package com.cht.iot.mqtt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.IoEventQueueThrottle;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.cht.iot.mqtt.protocol.ConnackPacket;
import com.cht.iot.mqtt.protocol.ConnectPacket;
import com.cht.iot.mqtt.protocol.DisconnectPacket;
import com.cht.iot.mqtt.protocol.IllegalProtocolException;
import com.cht.iot.mqtt.protocol.Packet;
import com.cht.iot.mqtt.protocol.PingreqPacket;
import com.cht.iot.mqtt.protocol.PingrespPacket;
import com.cht.iot.mqtt.protocol.PubackPacket;
import com.cht.iot.mqtt.protocol.PubcompPacket;
import com.cht.iot.mqtt.protocol.PublishPacket;
import com.cht.iot.mqtt.protocol.SubackPacket;
import com.cht.iot.mqtt.protocol.SubscribePacket;
import com.cht.iot.mqtt.protocol.UnsubackPacket;
import com.cht.iot.mqtt.protocol.UnsubscribePacket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyBrokerImpl implements MyBroker {
	int executorCorePoolSize = 100;
	int executorMaxPoolSize = 1000;
	int executorKeepAliveTime = 60;
	
	int port = 1883;
	IoAcceptor acceptor;
	int eventQueueThrottle = 8192;
	
	int authenticationTimeout = 5; // login timeout in seconds
	int idleTimeout = 90; // idle timeout in seconds
	
	Listener listener = new Listener() {		
		public boolean challenge(MqttSlave slave, String username, String password) { return true; }		
		public void onSlaveArrived(MqttSlave slave) {}
		public void onSlaveExited(MqttSlave slave) {}		
		public void onSubscribe(MqttSlave slave, String topic) {};		
		public void onMessage(MqttSlave slave, String topic, byte[] payload) {};
	};
	
	Map<String, TopicRoom> rooms = new ConcurrentHashMap<>(); // topic -> room
	
	OrderedThreadPoolExecutor executor;
	
	public MyBrokerImpl() {	
	}
	
	/**
	 * How many threads are ready.
	 * 
	 * @param executorCorePoolSize
	 */
	public void setExecutorCorePoolSize(int executorCorePoolSize) {
		this.executorCorePoolSize = executorCorePoolSize;
	}
	
	/**
	 * Maximum number of threads will be created.
	 * 
	 * @param executorMaxPoolSize
	 */
	public void setExecutorMaxPoolSize(int executorMaxPoolSize) {
		this.executorMaxPoolSize = executorMaxPoolSize;
	}
	
	/**
	 * Thread's idle timeout in seconds;
	 * 
	 * @param executorKeepAliveTime
	 */	
	public void setExecutorKeepAliveTime(int executorKeepAliveTime) {
		this.executorKeepAliveTime = executorKeepAliveTime;
	}
	
	/**
	 * Set the MQTT listening port.
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Set the flow control. Default is 8192.
	 * 
	 * @param eventQueueThrottle
	 */
	public void setEventQueueThrottle(int eventQueueThrottle) {
		this.eventQueueThrottle = eventQueueThrottle;
	}
	
	/**
	 * Set authentication timeout in seconds. default is 5 seconds
	 * 
	 * @param authenticationTimeout
	 */
	public void setAuthenticationTimeout(int authenticationTimeout) {
		this.authenticationTimeout = authenticationTimeout;
	}
	
	/**
	 * Set session idle timeout in seconds. default is 90 seconds
	 * 
	 * @param timeout
	 */
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}
	
	// ======
	
	@PostConstruct
	public void start() throws IOException, GeneralSecurityException {
		acceptor = new NioSocketAcceptor(); // Mina Server		

		executor = new OrderedThreadPoolExecutor(
				executorCorePoolSize,
				executorMaxPoolSize,
				executorKeepAliveTime, TimeUnit.SECONDS,
				Executors.defaultThreadFactory(), new IoEventQueueThrottle(eventQueueThrottle));
		
		acceptor.getFilterChain().addLast("executor", new ExecutorFilter(executor));
		
		ProtocolCodecFactory pcf = new CodecFactory();
		ProtocolCodecFilter filter = new ProtocolCodecFilter(pcf);
		acceptor.getFilterChain().addLast("mqtt", filter);
		
		IoHandler handler = new ServerHandler();		
		acceptor.setHandler(handler);
		
		acceptor.bind(new InetSocketAddress(port));
		
		log.info("Listens at {}", port);
	}
	
	@PreDestroy
	public void stop() {		
		log.info("Shutdown the broker");
		
		for (IoSession s : acceptor.getManagedSessions().values()) {
			s.closeNow();
		}
		
		acceptor.unbind();		
		acceptor.dispose();
		
		executor.shutdown();
	}
		
	// ======
	
	/**
	 * Assign a 'Slave' into the connected session.
	 * 
	 * @param session
	 * @return
	 */
	MqttSlave register(IoSession session) {
		MqttSlave slave = new MqttSlave(session);
		session.setAttribute("slave", slave);
		
		listener.onSlaveArrived(slave);
		
		return slave;
	}
	
	/**
	 * Get 'Slave' from session.
	 * 
	 * @param session
	 * @return
	 */
	
	MqttSlave getSlave(IoSession session) {
		return (MqttSlave) session.getAttribute("slave");
	}
	
	/**
	 * Fire the advisory message for session disconnection.
	 * 
	 * @param session
	 * @return
	 */
	void unregister(IoSession session) {
		MqttSlave slave = getSlave(session);
		if (slave == null) {
			log.error("Failed to get MqttSlave from - {}", SessionUtils.toString(session));			
			return;
		}
		
		for (String topic : slave.getTopics()) {
			TopicRoom room = rooms.get(topic);
			if (room == null) {
				continue;
			}
			
			room.removeSubscriber(slave);
			removeTopicRoomIfEmpty(room);
		}			
		
		listener.onSlaveExited(slave);
	}
	
	void removeTopicRoomIfEmpty(TopicRoom room) {
		if (room.isEmpty()) { // FIXME - another subscriber could be added after this statement
			String topic = room.getTopic();				
			rooms.remove(topic);					
			log.info("Remove the empty topic - {}", topic);
		}
	}
	
	// ======

	@Override
	public void publish(String topic, byte[] payload) {
		publish(topic, ByteBuffer.wrap(payload));
	}
	
	void publish(final String topic, final ByteBuffer message) {
		try {
			dispatch(topic, message); // publish the message to the local subscribers
	        
		} catch (Exception e) {
			log.error("Failed to publish message to topic - " + topic, e);
		}		
	}
	
	void write(MqttSlave slave, ByteBuffer buffer) {
		IoSession session = slave.getSession();
		if (SessionUtils.isOkay(session)) {
			session.write(buffer);
		}
	}
	
	void write(MqttSlave slave, Packet packet) throws IOException {
		write(slave, packet.toByteBuffer());
	}
	
	/**
	 * Dispatch the message to the local subscribers.
	 * 	
	 * @param topic
	 * @param message
	 * @throws IOException
	 */
	void dispatch(String topic, ByteBuffer message) throws IOException {
		TopicRoom room = rooms.get(topic);
		if (room != null) {
			PublishPacket pkt = new PublishPacket();
			pkt.setTopic(topic);
			pkt.setMessage(message);

			ByteBuffer bytes = pkt.toByteBuffer();
			
			for (MqttSlave slave : room.getSubscribers()) { // return the WHOLE NEW subscriber list
				ByteBuffer copy = bytes.slice();
				write(slave, copy); // HINT - don't call Packet.toByteBuffer() again
			}			
		}
	}
	
	// ======
	
	/**
	 * Register the new subscriber.
	 * 
	 * @param topic
	 * @param subscriber
	 * @throws IOException
	 */
	void subscribe(MqttSlave slave, String topic) throws IOException {
		TopicRoom room;
		
		synchronized (rooms) {
			room = rooms.get(topic);
			if (room == null) {
				room = new TopicRoom(topic);
				rooms.put(topic, room);
			}
		}	
		
		room.addSubscriber(slave); // FIXME - the 'room' could be removed from 'rooms' by other threads
		
		slave.getTopics().add(topic);
	}
	
	/**
	 * Un-register the subscriber.
	 * 
	 * @param subscriber
	 * @param slave
	 * @param topic
	 * @throws IOException
	 */	
	void unsubscribe(MqttSlave slave, String topic) throws IOException {
		TopicRoom room = rooms.get(topic);
		if (room != null) {			
			room.removeSubscriber(slave);
			removeTopicRoomIfEmpty(room);
			
			slave.getTopics().remove(topic);
		}
	}
	
	// ======
	
	// receive the packets from client side
	void handle(IoSession session, Packet packet) throws IOException, InterruptedException {
		MqttSlave slave = getSlave(session);
		if (slave == null) {
			log.error("Failed to get MqttSlave from - {}", SessionUtils.toString(session));			
			return;
		}		
		
		handle(slave, packet);
	}
	
	// handle the MQTT packets
	void handle(MqttSlave slave, Packet packet) throws IOException, InterruptedException {
		if (packet instanceof PublishPacket) { // publish (notice the issue of memory leak)
			PublishPacket req = (PublishPacket) packet;
			
			String topic = req.getTopic();
			ByteBuffer message = req.getMessage();				
			
			// send messages from clients to internal service
			byte[] bytes = new byte[message.remaining()];
			message.get(bytes);
			listener.onMessage(slave, topic, bytes); // HINT - I'll not receive the messages from myself
							
			// build the message again
			message = req.getMessage();
			publish(topic, message); // publish the message to local subscribers
				
			int qos = req.getQoS();
			if (qos > 0) {				
				PubackPacket res = new PubackPacket();
				res.setPacketIdentifier(req.getPacketIdentifier());
				
				write(slave, res);
			}				
			
		} else if (packet instanceof PubackPacket) { // TODO - QoS 2
			
		} else if (packet instanceof PubcompPacket) { // TODO - QoS 2								
			
		} else if (packet instanceof ConnectPacket) { // connect
			ConnectPacket req = (ConnectPacket) packet;
			
			Account account = new Account(req.getUsername(), req.getPassword());
			slave.setAccount(account);
			slave.setClientId(req.getClientId());
			
			if (listener.challenge(slave, req.getUsername(), req.getPassword()) == false) { // no accepted
				ConnackPacket res = new ConnackPacket();
				res.setSessionPresent(false);
				res.setReturnCode(ConnackPacket.ReturnCode.UNAUTHENTICATED);					
				write(slave, res);
				
				throw new PermissionException();
			}
			
			// assign the reasonable timeout now
			IoSession session = slave.getSession();
			session.getConfig().setReaderIdleTime(idleTimeout); 
			session.getConfig().setWriterIdleTime(idleTimeout);
			
			ConnackPacket res = new ConnackPacket();
			res.setSessionPresent(false);
			res.setReturnCode(ConnackPacket.ReturnCode.ACCEPTED);
			
			write(slave, res);
			
			listener.onSlaveArrived(slave); // HINT - could throw the PermissionException here
				
		} else if (packet instanceof SubscribePacket) { // subscribe
			SubscribePacket req = (SubscribePacket) packet;
			
			List<SubscribePacket.Topic> topics = req.getTopics();
			for (SubscribePacket.Topic topic : topics) {
				String tf = topic.getTopicFilter();
				
				listener.onSubscribe(slave, tf); // HINT - could throw the PermissionException here
				
				subscribe(slave, tf);
			}
				
			SubackPacket res = new SubackPacket();
			res.setPacketIdentifier(req.getPacketIdentifier());
			res.setReturnCode(SubackPacket.ReturnCode.QOS0);				
			write(slave, res);
			
		} else if (packet instanceof UnsubscribePacket) { // unsubscribe
			UnsubscribePacket req = (UnsubscribePacket) packet;
			
			for (String tf : req.getTopicFilters()) {
				unsubscribe(slave, tf);
			}
			
			UnsubackPacket res = new UnsubackPacket();
			res.setPacketIdentifier(req.getPacketIdentifier());
			write(slave, res);				
			
		} else if (packet instanceof PingreqPacket) { // ping
			PingrespPacket res = new PingrespPacket();
			
			write(slave, res);
			
		} else if (packet instanceof DisconnectPacket) {
			
		} else {
			throw new IllegalProtocolException(packet.getClass().getSimpleName() + " is not yet supported - " + packet.getType());
		}
	}
	
	// ====== Mina Server ======
	
	class ServerHandler extends IoHandlerAdapter {
		
		@Override
		public void sessionOpened(IoSession session) throws Exception {
			if (SessionUtils.setup(session) == false) {
				log.error("Don't accept the broken session");				
				session.closeNow();
				return;
			}
			
			SocketSessionConfig cfg = (SocketSessionConfig) session.getConfig();
			cfg.setReaderIdleTime(authenticationTimeout);
			cfg.setSoLinger(0); // avoid TIME_WAIT problem			
			
			MqttSlave slave = register(session); // every session must has 'from' and 'slave'

			log.info(String.format("Connected - %s, sessions: %,d, topics: %,d, active: %,d, free: %,d bytes",
					slave.getConnection(),
					acceptor.getManagedSessionCount(),
					rooms.size(),
					executor.getActiveCount(),
					Runtime.getRuntime().freeMemory()
					));
		}
		
		@Override
		public void messageReceived(IoSession session, Object message) throws Exception {
			Packet pkt = (Packet) message;
			try {			
				handle(session, pkt);		
				
			} catch (IllegalProtocolException e) {
				log.error("Unsupported MQTT packet is from - " + getSlave(session), e);
				
			} catch (PermissionException e) {
				log.error("Permission denied from - " + getSlave(session));
				
				session.closeNow();
				
			} catch (Exception e) {
				log.error("Failed to handle the MQTT packet from - " + getSlave(session), e);
			}					
		}
		
		@Override
		public void sessionIdle(final IoSession session, IdleStatus status) throws Exception {
			log.info("Idle - {}", getSlave(session));
			
			session.closeNow();
		}
		
		@Override
		public void exceptionCaught(final IoSession session, final Throwable cause) throws Exception {
			log.error("[{}] {} - {}", getSlave(session), cause.getClass().getSimpleName(), cause.getMessage(), cause);
			
			session.closeNow();
		}
		
		@Override
		public void sessionClosed(final IoSession session) throws Exception {
			log.info("Disconnected - {}", getSlave(session));	// session will be recycled by Mina Server, remove it from memory during 'dispatch()'
			
			unregister(session);
		}
	}
	
	static class Encoder implements ProtocolEncoder {
		
		@Override
		public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
			ByteBuffer bytes = (ByteBuffer) message; // ByteBuffer to IoBuffer
			IoBuffer ib = IoBuffer.wrap(bytes);			
			out.write(ib);			
		}
		
		@Override
		public void dispose(IoSession session) throws Exception {			
		}
	}
	
	static class CodecFactory implements ProtocolCodecFactory {
		final ProtocolEncoder encoder = new Encoder();
		final ProtocolDecoder decoder = new PacketDecoder();
		
		@Override
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			return encoder;
		}
		
		@Override
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			return decoder;
		}
	}	
}
