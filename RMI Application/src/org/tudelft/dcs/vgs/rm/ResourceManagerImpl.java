package org.tudelft.dcs.vgs.rm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.tudelft.dcs.vgs.client.Client;
import org.tudelft.dcs.vgs.gs.GridScheduler;
import org.tudelft.dcs.vgs.message.EventType;
import org.tudelft.dcs.vgs.message.Job;
import org.tudelft.dcs.vgs.message.Message;
import org.tudelft.dcs.vgs.processor.Processor;

public class ResourceManagerImpl extends UnicastRemoteObject implements ResourceManager {

	private static final long serialVersionUID = 1L;

	private Registry registry;
	private final int id;
	private final String jobQueueFile;
	private final String url;
	private final int port;
	private final List<String> backups;
	private final List<String> gridSchedulers;
	private final Map<Integer, Processor> processors;

	private Queue<Job> jobQueue;
	private Map<Integer, Boolean> idleProcessors;

	public ResourceManagerImpl(Registry registry, int id, String url, int port, List<String> gridSchedulers,
			List<String> processors, List<String> backups, boolean fails)
					throws NotBoundException, FileNotFoundException, IOException, ClassNotFoundException {
		this.registry = registry;
		this.id = id;
		this.jobQueueFile = "jobQueue" + id + ".ser";
		this.url = url;
		this.port = port;
		this.gridSchedulers = gridSchedulers;
		this.backups = backups;
		this.processors = new HashMap<Integer, Processor>();
		for (int pid = 0; pid < processors.size(); pid++) {
			this.processors.put(pid, (Processor) Naming.lookup(processors.get(pid)));
		}
		this.idleProcessors = new ConcurrentHashMap<Integer, Boolean>();
		for (int pid : this.processors.keySet()) {
			this.idleProcessors.put(pid, false);
		}
		this.jobQueue = new ConcurrentLinkedQueue<Job>();
		new LoadBalancer().start();
		if (fails) {
			//new Failure().start();
		}
	}

	@Override
	public boolean receiveRequest(Job job) throws FileNotFoundException, IOException {
		log(EventType.RM_JOB_ARRIVAL, job);
		job.setCluster(url);
		jobQueue.offer(job);
		saveJobQueue();
		if (!job.isRequired() && jobQueue.size() >= processors.size()) {
			delegateJob(job);
		}
		return true;
	}

	@Override
	public boolean receiveJobCompletion(int pid, Job job)
			throws RemoteException, MalformedURLException, NotBoundException {
		log(EventType.RM_JOB_COMPLETION, job);
		((Client) Naming.lookup(job.getClient())).receiveJobCompletion(job);
		return true;
	}

	@Override
	public int getLoad() throws RemoteException {
		return jobQueue.size();
	}

	@Override
	public void receiveProcessorStateUpdate(int pid, boolean idle) throws RemoteException {
		this.idleProcessors.put(pid, idle);
	}

	@Override
	public void restart() throws RemoteException, MalformedURLException, AlreadyBoundException, InterruptedException {
		UnicastRemoteObject.unexportObject(registry, true);
		log(EventType.RM_RESTART, null);
		for (String url : backups) {
			try {
				ResourceManager rm = (ResourceManager) Naming.lookup(url);
				if (rm.receiveRequests(jobQueue)) {
					jobQueue.clear();
					saveJobQueue();
					break;
				}
			} catch (Exception e) {
				continue;
			}
		}
		Thread.sleep(1000);
		System.setProperty("java.security.policy", "file:security.policy");
		registry = LocateRegistry.createRegistry(port);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new java.rmi.RMISecurityManager());
		}
		Naming.bind(url, this);
	}

	@Override
	public boolean receiveRequests(Collection<Job> jobs) throws RemoteException, FileNotFoundException, IOException {
		for (Job job : jobs) {
			receiveRequest(job);
		}
		return true;
	}

	private void saveJobQueue() throws FileNotFoundException, IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(jobQueueFile));
		oos.writeObject(jobQueue);
		oos.close();
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void loadJobQueue() throws FileNotFoundException, IOException, ClassNotFoundException {
		if (new File(jobQueueFile).exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(jobQueueFile));
			Object obj = ois.readObject();
			if (obj != null && obj instanceof Queue && ((Queue<?>) obj).size() > 0) {
				this.jobQueue = (Queue<Job>) obj;
			}
			ois.close();
		}
	}

	private void delegateJob(Job job) {
		int randomStart = new Random().nextInt(gridSchedulers.size());
		for (int i = randomStart; i < gridSchedulers.size(); i++) {
			String url = gridSchedulers.get(i);
			try {
				if (((GridScheduler) Naming.lookup(url)).receiveRequest(job)) {
					jobQueue.remove(job);
					break;
				}
			} catch (Exception e) {
				continue;
			}
		}
	}

	private class LoadBalancer extends Thread {

		public void run() {
			while (true) {
				for (int pid : idleProcessors.keySet()) {
					boolean idle = idleProcessors.get(pid);
					if (!idle) {
						try {
							Processor p = processors.get(pid);
							Job job = jobQueue.peek();
							if (job != null) {
								List<String> rmList = new ArrayList<String>();
								rmList.add(url);
								rmList.addAll(backups);
								p.execute(job, rmList);
								log(EventType.RM_JOB_START, job);
								jobQueue.remove(job);
								saveJobQueue();
							}
						} catch (Exception e) {
							continue;
						}
					}
				}
			}
		}
	}

	private void log(final EventType et, final Job job) {
		for (String url : gridSchedulers) {
			Message msg = new Message(id, et, job);
			try {
				((GridScheduler) Naming.lookup(url)).log(msg);
			} catch (Exception e) {
				continue;
			}
		}
	}

	private class Failure extends Thread {
		public void run() {
			while (true) {
				try {
					Thread.sleep(5000);
					restart();
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

}
