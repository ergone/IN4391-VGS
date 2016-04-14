package org.tudelft.dcs.vgs.gm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.tudelft.dcs.vgs.gs.GridScheduler;
import org.tudelft.dcs.vgs.message.NodeInfo;
import org.tudelft.dcs.vgs.processor.Processor;
import org.tudelft.dcs.vgs.rm.ResourceManager;

public class GridManagerImpl extends UnicastRemoteObject implements GridManager {

	private static final long serialVersionUID = 1L;

	private Properties gs;
	private Properties rm;
	private Properties processors;

	private List<String> allGs;
	private List<String> allRm;
	private List<String> allBackupRm;
	private Map<String, List<String>> processorsPerRm;

	private boolean failingRm;
	private boolean failingGs;

	public GridManagerImpl(String url, String gsPath, String rmPath, String processorsPath)
			throws UnsupportedEncodingException, FileNotFoundException, IOException, RemoteException {
		gs = new Properties();
		rm = new Properties();
		processors = new Properties();
		gs.load(new InputStreamReader(new FileInputStream(gsPath)));
		rm.load(new InputStreamReader(new FileInputStream(rmPath)));
		processors.load(new InputStreamReader(new FileInputStream(processorsPath)));
		allGs = getAllGs();
		allRm = getAllRm();
		allBackupRm = getAllBackupRm();
		processorsPerRm = getProcessorsPerRm();
	}

	private Map<String, List<String>> getProcessorsPerRm() {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		int step = processors.size() / allRm.size();

		List<String> used = new ArrayList<String>();
		for (String rm : allRm) {
			List<String> rmProcessors = new ArrayList<String>();
			for (Object p : processors.values()) {
				if (!used.contains(p)) {
					rmProcessors.add((String) p);
					used.add((String) p);
				}
				if (rmProcessors.size() == step) {
					break;
				}
			}
			result.put(rm, rmProcessors);
		}

		used.clear();
		for (String rm : allBackupRm) {
			List<String> rmProcessors = new ArrayList<String>();
			for (Object p : processors.values()) {
				if (!used.contains(p)) {
					rmProcessors.add((String) p);
					used.add((String) p);
				}
				if (rmProcessors.size() == step) {
					break;
				}
			}
			result.put(rm, rmProcessors);
		}

		return result;
	}

	private List<String> getAllGs() {
		List<String> result = new ArrayList<String>();
		for (Object url : gs.values()) {
			result.add((String) url);
		}
		return result;
	}

	private List<String> getAllRm() {
		List<String> result = new ArrayList<String>();
		int i = 0;
		for (Object url : rm.values()) {
			if (i % 2 == 0) {
				result.add((String) url);
			}
			i++;
		}
		return result;
	}

	private List<String> getAllBackupRm() {
		List<String> result = new ArrayList<String>();
		int i = 0;
		for (Object url : rm.values()) {
			if (i % 2 != 0) {
				result.add((String) url);
			}
			i++;
		}
		return result;
	}

	@Override
	public NodeInfo getNodeInfo(String url) throws RemoteException {
		NodeInfo ni = new NodeInfo();
		if (gs.containsValue(url)) {
			ni.setClassName(GridScheduler.class.getName());
			ni.setGridSchedulers(allGs);
			ni.setResourceManagers(allRm);
			ni.setBackupRresourceManagers(allBackupRm);
			for (Entry<Object, Object> e : gs.entrySet()) {
				if (e.getValue().equals(url)) {
					ni.setId(Integer.valueOf((String) e.getKey()));
					synchronized (this) {
						if (!failingGs) {
							ni.setFails(true);
							failingGs = true;
						}
					}
					break;
				}
			}
		} else if (rm.containsValue(url)) {
			ni.setClassName(ResourceManager.class.getName());
			ni.setGridSchedulers(allGs);
			for (Entry<Object, Object> e : rm.entrySet()) {
				if (e.getValue().equals(url)) {
					ni.setId(Integer.valueOf((String) e.getKey()));
					synchronized (this) {
						if (!failingRm && allRm.contains(url)) {
							ni.setFails(true);
							failingRm = true;
						}
					}
					break;
				}
			}
			List<String> backups = new ArrayList<String>();
			if (allRm.contains(url)) {
				backups.add(allBackupRm.get(allRm.indexOf(url)));
			} else if (allBackupRm.contains(url)) {
				backups.add(allRm.get(allBackupRm.indexOf(url)));
			}
			ni.setBackupRresourceManagers(backups);
			ni.setProcessors(processorsPerRm.get(url));
		} else if (processors.containsValue(url)) {
			ni.setClassName(Processor.class.getName());
			for (Entry<Object, Object> e : processors.entrySet()) {
				if (e.getValue().equals(url)) {
					ni.setId(Integer.valueOf((String) e.getKey()));
					break;
				}
			}
		} else {
			throw new RemoteException();
		}
		return ni;
	}

	public static void main(String[] args)
			throws UnsupportedEncodingException, FileNotFoundException, IOException, AlreadyBoundException {
		String localUrl = args[0];
		int port = Integer.valueOf(localUrl.trim().substring(localUrl.lastIndexOf("/") + 1));

		String gsPath = "gs.properties";
		String rmPath = "rm.properties";
		String processorsPath = "processors.properties";

		System.setProperty("java.security.policy", "file:security.policy");
		LocateRegistry.createRegistry(port);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new java.rmi.RMISecurityManager());
		}
		Naming.bind(localUrl, new GridManagerImpl(localUrl, gsPath, rmPath, processorsPath));
	}

}
