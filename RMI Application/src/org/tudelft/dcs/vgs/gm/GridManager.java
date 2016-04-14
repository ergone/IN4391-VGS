package org.tudelft.dcs.vgs.gm;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.tudelft.dcs.vgs.message.NodeInfo;

public interface GridManager extends Remote {

	NodeInfo getNodeInfo(String url) throws RemoteException;
}
