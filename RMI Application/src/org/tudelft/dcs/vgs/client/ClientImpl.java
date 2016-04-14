package org.tudelft.dcs.vgs.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.tudelft.dcs.vgs.message.Job;
import org.tudelft.dcs.vgs.rm.ResourceManager;

public class ClientImpl extends UnicastRemoteObject implements Client {

	private static final long serialVersionUID = 1L;
	private static final long JOB_DURATION = 100;
	private static final long JOB_NUM = 10000;

	private int jobsCounter;
	private final Map<Long, Long> jobsStartTime;
	private final Map<Long, Long> jobsEndTime;
	private Map<Long, Job> allJobs;

	public ClientImpl() throws RemoteException {
		this.jobsStartTime = new ConcurrentHashMap<Long, Long>();
		this.jobsEndTime = new ConcurrentHashMap<Long, Long>();
		this.allJobs = new ConcurrentHashMap<Long, Job>();
	}

	@Override
	public void receiveJobCompletion(final Job job) throws RemoteException {
		new Thread() {
			public void run() {
				allJobs.put(job.getId(), job);
				jobsEndTime.put(job.getId(), System.nanoTime());
				if (jobsEndTime.size() == jobsCounter) {
					Properties prop = new Properties();
					for (Long id : jobsStartTime.keySet()) {
						long elapsedTime = ((jobsEndTime.get(id) - jobsStartTime.get(id)) / 1000000L - JOB_DURATION);
						prop.setProperty(allJobs.get(id).toString(), String.valueOf(elapsedTime));
					}
					try {
						Properties gs = new Properties();
						Properties rm = new Properties();
						Properties processors = new Properties();
						gs.load(new InputStreamReader(new FileInputStream("gs.properties")));
						rm.load(new InputStreamReader(new FileInputStream("rm.properties")));
						processors.load(new InputStreamReader(new FileInputStream("processors.properties")));
						String fileName = "GS=" + gs.size() + "_RM=" + rm.size() + "_PROC=" + processors.size();
						prop.store(new FileOutputStream(fileName + ".properties"), null);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}.start();
	}

	public static void main(String[] args)
			throws NotBoundException, FileNotFoundException, IOException, AlreadyBoundException {
		final String url = args[0];
		final int port = Integer.valueOf(url.trim().substring(url.lastIndexOf("/") + 1));

		final ClientImpl ci = new ClientImpl();
		System.setProperty("java.security.policy", "file:security.policy");
		LocateRegistry.createRegistry(port);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new java.rmi.RMISecurityManager());
		}
		Naming.bind(url, ci);

		Properties rmp = new Properties();
		rmp.load(new InputStreamReader(new FileInputStream("rm.properties")));
		final List<String> rms = new ArrayList<String>();
		final List<String> backupRms = new ArrayList<String>();
		int r = 0;
		for (Object u : rmp.values()) {
			if (r % 2 == 0) {
				rms.add((String) u);
			} else {
				backupRms.add((String) u);
			}
			r++;
		}

		int leastLoaded = rms.size() - 1;
		int mostLoaded = 0;

		final Map<Integer, List<Job>> jobs = new HashMap<Integer, List<Job>>();
		for (int i = 0; i < rms.size(); i++) {
			double limit = 0;
			if (rms.size() == 1) {
				limit = JOB_NUM;
			} else {
				if (i == leastLoaded) {
					limit = Math.ceil(JOB_NUM / 25);
				} else if (i == mostLoaded) {
					limit = Math.ceil(JOB_NUM / 5);
				} else {
					limit = ((JOB_NUM - Math.ceil((Math.ceil(JOB_NUM / 25) + Math.ceil(JOB_NUM / 5))))
							/ (rms.size() - 2));
				}
			}
			List<Job> jobList = new ArrayList<Job>();
			for (int j = 0; j < limit; j++) {
				Job job = new Job(++ci.jobsCounter, JOB_DURATION, url);
				jobList.add(job);
			}
			jobs.put(i, jobList);
		}

		for (int i = 0; i < rms.size(); i++) {
			final int j = i;
			new Thread() {
				public void run() {
					for (Job job : jobs.get(j)) {
						ci.jobsStartTime.put(job.getId(), System.nanoTime());
						try {
							((ResourceManager) Naming.lookup(rms.get(j))).receiveRequest(job);
						} catch (Exception e) {
							try {
								((ResourceManager) Naming.lookup(backupRms.get(j))).receiveRequest(job);
							} catch (Exception e1) {
								e.printStackTrace();
							}
						}
					}
				}
			}.start();
		}
	}

}
