package org.tudelft.dcs.vgs.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.tudelft.dcs.vgs.gm.GridManager;
import org.tudelft.dcs.vgs.gs.GridScheduler;
import org.tudelft.dcs.vgs.gs.GridSchedulerImpl;
import org.tudelft.dcs.vgs.message.NodeInfo;
import org.tudelft.dcs.vgs.processor.Processor;
import org.tudelft.dcs.vgs.processor.ProcessorImpl;
import org.tudelft.dcs.vgs.rm.ResourceManager;
import org.tudelft.dcs.vgs.rm.ResourceManagerImpl;

public class Main {

	public static void main(String[] args) throws MalformedURLException, UnsupportedEncodingException,
			FileNotFoundException, AlreadyBoundException, IOException, NotBoundException, ClassNotFoundException {
		String localUrl = args[0];
		String gmUrl = args[1];
		int port = Integer.valueOf(localUrl.trim().substring(localUrl.lastIndexOf("/") + 1));

		NodeInfo ni = ((GridManager) Naming.lookup(gmUrl)).getNodeInfo(localUrl);

		System.setProperty("java.security.policy", "file:security.policy");
		Registry registry = LocateRegistry.createRegistry(port);
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new java.rmi.RMISecurityManager());
		}

		if (ni.getClassName().equals(GridScheduler.class.getName())) {
			Naming.bind(localUrl, new GridSchedulerImpl(registry, ni.getId(), localUrl, port, ni.getGridSchedulers(),
					ni.getResourceManagers(), ni.getBackupRresourceManagers(), ni.isFails()));
		} else if (ni.getClassName().equals(ResourceManager.class.getName())) {
			Naming.bind(localUrl, new ResourceManagerImpl(registry, ni.getId(), localUrl, port, ni.getGridSchedulers(),
					ni.getProcessors(), ni.getBackupRresourceManagers(), ni.isFails()));
		} else if (ni.getClassName().equals(Processor.class.getName())) {
			Naming.bind(localUrl, new ProcessorImpl(ni.getId()));
		}
	}
}
