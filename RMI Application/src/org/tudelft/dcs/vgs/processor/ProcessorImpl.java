package org.tudelft.dcs.vgs.processor;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import org.tudelft.dcs.vgs.rm.ResourceManager;
import org.tudelft.dcs.vgs.message.Job;

public class ProcessorImpl extends UnicastRemoteObject implements Processor {

	private static final long serialVersionUID = 1L;

	private final int id;
	private boolean idle;

	public ProcessorImpl(int id) throws RemoteException {
		this.id = id;
	}

	@Override
	public synchronized void execute(final Job job, final List<String> rmList) throws RemoteException {
		if (idle) {
			throw new RemoteException();
		} else if (job != null) {
			idle = true;
			new Thread() {
				public void run() {
					for (String rm : rmList) {
						try {
							((ResourceManager) Naming.lookup(rm)).receiveProcessorStateUpdate(id, true);
						} catch (Exception e) {
							continue;
						}
					}
					try {
						Thread.sleep(job.getDuration());
					} catch (InterruptedException e) {
					}
					job.setExecutedOn(id);
					for (String rm : rmList) {
						try {
							if (((ResourceManager) Naming.lookup(rm)).receiveJobCompletion(id, job)) {
								break;
							}
						} catch (Exception e) {
							continue;
						}
					}
					for (String rm : rmList) {
						try {
							((ResourceManager) Naming.lookup(rm)).receiveProcessorStateUpdate(id, false);
						} catch (Exception e) {
							continue;
						}
					}
					idle = false;
				}
			}.start();
		}
	}

}
