package org.tudelft.dcs.vgs.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.tudelft.dcs.vgs.message.Job;

public interface Client extends Remote {
	
	void receiveJobCompletion(Job job) throws RemoteException;
}
