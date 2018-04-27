package com.cht.iot.mqtt.protocol;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Buffer {
	protected byte[] bytes;
	protected int head = 0;
	protected int tail = 0;
	
	public Buffer(int initialCapacity) {
		this.bytes = new byte[initialCapacity + 1];
	}
	
	public void clear() {
		this.head = this.tail = 0;
	}
	
	public void write(byte b) {
		int i = (this.tail + 1) % this.bytes.length;
		if (i == this.head) { // full
			byte[] nb = new byte[bytes.length * 2];
			int s;
			if (this.tail >= this.head) {
				s = this.tail - this.head;
				System.arraycopy(this.bytes, this.head, nb, 0, this.tail - this.head);
								
			} else {
				s = this.bytes.length - this.head;
				System.arraycopy(this.bytes, this.head, nb, 0, s);
				if (this.tail > 0) {
					System.arraycopy(this.bytes, 0, nb, s, this.tail);
				}
				
				s += this.tail;
			}	
			
			this.bytes = nb;
			this.head = 0;
			this.tail = s;
			
			i = this.tail + 1;
		}
		
		this.bytes[this.tail] = b;
		this.tail = i;
	}
	
	public void write(ByteBuffer bb) {
		int s = bb.remaining();
		for (int i = 0; i < s; i++) {
			this.write(bb.get());
		}
	}
	
	public byte get(int offset) {
		int i = (this.head + offset) % this.bytes.length;
		return this.bytes[i];
	}
	
	public byte read() {
		if (this.head == this.tail) {
			throw new BufferUnderflowException();
			
		} else {
			byte b = this.bytes[this.head];
			this.head = (this.head + 1) % this.bytes.length;
			
			return b;
		}
	}
	
	public void read(byte[] b) {
		for (int i = 0; i < b.length; i++) {
			b[i] = this.read();
		}
	}
	
	public void read(ByteBuffer bb) {
		int s = bb.capacity();
		for (int i = 0; i < s; i++) {
			bb.put(this.read());
		}
	}
	
	public int size() {
		if (this.tail >= this.head) {
			return this.tail - this.head;
			
		} else {
			return this.tail + (this.bytes.length - this.head);				
		}
	}
	
	@Override
	public String toString() {
		return String.format("capacity: %,d, head: %,d, tail: %,d", this.bytes.length, this.head, this.tail);
	}
}
