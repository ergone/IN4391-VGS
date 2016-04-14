package org.tudelft.dcs.vgs.message;

import java.io.Serializable;
import java.util.List;

public class NodeInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int id;
	private String className;
	private List<String> gridSchedulers;
	private List<String> resourceManagers;
	private List<String> backupRresourceManagers;
	private List<String> processors;
	private boolean fails;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public List<String> getGridSchedulers() {
		return gridSchedulers;
	}

	public void setGridSchedulers(List<String> gridSchedulers) {
		this.gridSchedulers = gridSchedulers;
	}

	public List<String> getResourceManagers() {
		return resourceManagers;
	}

	public void setResourceManagers(List<String> resourceManagers) {
		this.resourceManagers = resourceManagers;
	}

	public List<String> getProcessors() {
		return processors;
	}

	public void setProcessors(List<String> processors) {
		this.processors = processors;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<String> getBackupRresourceManagers() {
		return backupRresourceManagers;
	}

	public void setBackupRresourceManagers(List<String> backupRresourceManagers) {
		this.backupRresourceManagers = backupRresourceManagers;
	}

	public boolean isFails() {
		return fails;
	}

	public void setFails(boolean fails) {
		this.fails = fails;
	}

}
