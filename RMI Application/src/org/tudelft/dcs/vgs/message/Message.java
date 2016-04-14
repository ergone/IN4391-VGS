package org.tudelft.dcs.vgs.message;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private int pid;
	private long timestamp;
	private EventType eventType;
	private Job job;

	public Message(int pid, EventType eventType) {
		this(pid, eventType, null);
	}

	public Message(int pid, EventType eventType, Job job) {
		setPid(pid);
		setEventType(eventType);
		setJob(job);
		setTimestamp(System.currentTimeMillis());
	}

	@Override
	public String toString() {
		return String.format("PID=%d, TS=%s, EVENT_TYPE=%s, JOB=%s", pid, timestamp, eventType.toString(), job != null ? job.toString() : "");
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
