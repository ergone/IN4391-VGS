package org.tudelft.dcs.vgs.rm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import org.tudelft.dcs.vgs.message.Job;

public interface ResourceManager extends Remote {

	boolean receiveRequest(Job job) throws RemoteException, FileNotFoundException, IOException;

	boolean receiveRequests(Collection<Job> jobs) throws RemoteException, FileNotFoundException, IOException;
	
	boolean receiveJobCompletion(int pid, Job job) throws RemoteException, MalformedURLException, NotBoundException;
	
	void receiveProcessorStateUpdate(int pid, boolean idle) throws RemoteException;
	
	int getLoad() throws RemoteException;
	
	void restart() throws RemoteException, MalformedURLException, AlreadyBoundException, InterruptedException;
	
}
