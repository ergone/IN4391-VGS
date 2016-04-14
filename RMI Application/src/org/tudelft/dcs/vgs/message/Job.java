package org.tudelft.dcs.vgs.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Job implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;
	private long duration;
	private String client;
	private List<String> clusters;
	private boolean required;
	private boolean retransmition;
	private int executedOn;

	public Job(long id, long duration, String client) {
		setId(id);
		setClient(client);
		setDuration(duration);
		this.clusters = new ArrayList<String>();
	}

	@Override
	public String toString() {
		return String.format("[ ID=[%d], CLUSTERS=[%s], EXECUTED_ON=[%d] ]", id, clusters.toString(), executedOn);
	}

	public void setCluster(String rm) {
		clusters.add(rm);
	}

	public String getCluster() {
		return clusters.get(clusters.size() - 1);
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isRetransmition() {
		return retransmition;
	}

	public void setRetransmition(boolean retransmition) {
		this.retransmition = retransmition;
	}

	public int getExecutedOn() {
		return executedOn;
	}

	public void setExecutedOn(int executedOn) {
		this.executedOn = executedOn;
	}

}
