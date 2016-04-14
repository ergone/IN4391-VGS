package org.tudelft.dcs.vgs.gs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;

import org.tudelft.dcs.vgs.message.Job;
import org.tudelft.dcs.vgs.message.Message;

public interface GridScheduler extends Remote {

	boolean receiveRequest(Job job) throws RemoteException, FileNotFoundException, IOException;
	
	boolean receiveRequests(Collection<Job> jobs) throws RemoteException, FileNotFoundException, IOException;
	
	void log(Message msg) throws RemoteException, IOException;
	
	Map<String, StringBuilder> getLogs() throws RemoteException, IOException;
	
	void restart() throws RemoteException, MalformedURLException, AlreadyBoundException, InterruptedException, IOException;
	
}
