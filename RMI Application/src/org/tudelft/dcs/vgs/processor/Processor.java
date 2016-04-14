package org.tudelft.dcs.vgs.processor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.tudelft.dcs.vgs.message.Job;

public interface Processor extends Remote {

	void execute(Job job, List<String> rmList) throws RemoteException;
} 
