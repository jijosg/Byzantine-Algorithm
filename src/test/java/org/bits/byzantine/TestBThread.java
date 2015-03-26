package org.bits.byzantine;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TestBThread implements Runnable{
    private final static Log LOGGER = LogFactory.getLog(TestBThread.class);

    private DC_Byzantine_RMI process;
    
    public TestBThread(DC_Byzantine_RMI process){
        this.process = process;
    }
    
    public void run() {
    	try {
			LOGGER.debug("***Running process: " + process.getIndex());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    }
}