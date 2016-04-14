package org.tudelft.dcs.vgs.gs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.tudelft.dcs.vgs.gs.GridScheduler;
import org.tudelft.dcs.vgs.message.EventType;
import org.tudelft.dcs.vgs.message.Job;
import org.tudelft.dcs.vgs.message.Message;
import org.tudelft.dcs.vgs.rm.ResourceManager;

public class GridSchedulerImpl extends UnicastRemoteObject implements GridScheduler {

	private static final long serialVersionUID = 1L;

	private Registry registry;
	private final int id;
	private final String jobQueueFile;
	private final String msgQueueFile;
	private final String url;
	private final int port;
	private final List<String> gridSchedulers;
	private final List<String> resourceManagers;
	private final List<String> backupResourceManagers;

	private Queue<Job> jobQueue;
	private Queue<Message> msgQueue;

	public GridSchedulerImpl(Registry registry, int id, String url, int port, List<String> gridSchedulers,
			List<String> resourceManagers, List<String> backupResourceManagers, boolean fails)
					throws FileNotFoundException, ClassNotFoundException, IOException {
		this.registry = registry;
		this.id = id;
		this.jobQueueFile = "jobQueue" + id + ".ser";
		this.msgQueueFile = "msgQueue" + id + ".ser";
		this.url = url;
		this.port = port;
		gridSchedulers.remove(url);
		this.gridSchedulers = gridSchedulers;
		this.resourceManagers = resourceManagers;
		this.backupResourceManagers = backupResourceManagers;
		this.jobQueue = new ConcurrentLinkedQueue<Job>();
		this.msgQueue = new ConcurrentLinkedQueue<Message>();
		new LoadBalancer().start();
		new MessageProcessor().start();
		if (fails) {
			//new Failure().start();
		}
	}

	@Override
	public boolean receiveRequest(Job job) throws FileNotFoundException, IOException {
		log(new Message(id, EventType.GS_JOB_ARRIVAL, job));
		job.setRequired(true);
		jobQueue.add(job);
		saveJobQueue();
		return true;
	}

	@Override
	public void log(Message msg) throws RemoteException, IOException {
		msgQueue.offer(msg);
		saveMsgQueue();
	}

	@Override
	public Map<String, StringBuilder> getLogs() throws IOException {
		Map<String, StringBuilder> logs = new HashMap<String, StringBuilder>();
		File dir = new File(".");
		FileFilter fileFilter = new WildcardFileFilter("log_gs_" + id + "_*.txt");
		File[] files = dir.listFiles(fileFilter);
		for (File f : files) {
			StringBuilder log = new StringBuilder();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String line = null;
			while ((line = br.readLine()) != null) {
				log.append(line);
			}
			logs.put(f.getName(), log);
			br.close();
		}
		return logs;
	}

	@Override
	public void restart() throws AlreadyBoundException, InterruptedException, IOException {
		UnicastRemoteObject.unexportObject(registry, true);
		log(new Message(id, EventType.GS_RESTART));
		for (String url : gridSchedulers) {
			try {
				GridScheduler gs = (GridScheduler) Naming.lookup(url);
				if (gs.receiveRequests(jobQueue)) {
					jobQueue.clear();
					saveJobQueue();
					break;
				}
			} catch (Exception e) {
				continue;
			}
		}
		Thread.sleep(1000);
		synchronizeLogs();
		System.setProperty("java.security.policy", "file:security.policy");
		registry = LocateRegistry.createRegistry(port);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new java.rmi.RMISecurityManager());
		}
		Naming.bind(url, this);
	}

	@Override
	public boolean receiveRequests(Collection<Job> jobs) throws RemoteException, FileNotFoundException, IOException {
		jobQueue.addAll(jobs);
		return true;
	}

	private void synchronizeLogs() {
		for (String url : gridSchedulers) {
			try {
				GridScheduler gs = (GridScheduler) Naming.lookup(url);
				Map<String, StringBuilder> logs = gs.getLogs();
			} catch (Exception e) {
				continue;
			}
		}
	}

	private void saveJobQueue() throws FileNotFoundException, IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(jobQueueFile));
		oos.writeObject(jobQueue);
		oos.close();
	}

	private void saveMsgQueue() throws FileNotFoundException, IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(msgQueueFile));
		oos.writeObject(msgQueue);
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

	@SuppressWarnings({ "unchecked", "unused" })
	private void loadMsgQueue() throws FileNotFoundException, IOException, ClassNotFoundException {
		if (new File(msgQueueFile).exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(msgQueueFile));
			Object obj = ois.readObject();
			if (obj != null && obj instanceof Queue && ((Queue<?>) obj).size() > 0) {
				this.msgQueue = (Queue<Message>) obj;
			}
			ois.close();
		}
	}

	private class LoadBalancer extends Thread {

		public void run() {
			while (true) {
				if (!jobQueue.isEmpty()) {
					String leastLoadedRm = null;
					int minLoad = Integer.MAX_VALUE;
					for (int i = 0; i < resourceManagers.size(); i++) {
						try {
							String url = resourceManagers.get(i);
							ResourceManager rm = (ResourceManager) Naming.lookup(url);
							int load = rm.getLoad();
							if (load < minLoad) {
								leastLoadedRm = url;
								minLoad = load;
							}
						} catch (Exception ex1) {
							try {
								String url = backupResourceManagers.get(i);
								ResourceManager rm = (ResourceManager) Naming.lookup(url);
								int load = rm.getLoad();
								if (load < minLoad) {
									leastLoadedRm = url;
									minLoad = load;
								}
							} catch (Exception ex2) {
								continue;
							}
						}
					}
					if (leastLoadedRm != null) {
						try {
							Collection<Job> c = new ConcurrentLinkedQueue<Job>(jobQueue);
							if (((ResourceManager) Naming.lookup(leastLoadedRm)).receiveRequests(c)) {
								jobQueue.removeAll(c);
								saveJobQueue();
							}
						} catch (Exception ex) {
							continue;
						}
					}
				}
			}
		}
	}

	private class MessageProcessor extends Thread {
		public void run() {
			while (true) {
				if (!msgQueue.isEmpty()) {
					try {
						Message msg = msgQueue.poll();
						String logFile = "log_gs_" + id + "_" + msg.getPid() + ".txt";
						PrintWriter pw = new PrintWriter(new FileWriter(logFile, true));
						pw.println(msg.toString());
						pw.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
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
