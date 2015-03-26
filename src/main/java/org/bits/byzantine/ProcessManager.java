package org.bits.byzantine;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessManager {

    private static final Log LOGGER = LogFactory.getLog(ProcessManager.class);
    
    private static final String RMI_PREFIX ="rmi://";
    private ArrayList<DC_Byzantine_RMI> processes;
    private InetAddress inetAddress;

    /**
     * Launches server instance
     */
    public void startServer(){

        //read network configuration
        Configuration config;
        try{
            config = new PropertiesConfiguration("network.cfg");
        } catch (ConfigurationException e) {
            try{
                config = new PropertiesConfiguration("network.cfg.default");
            } catch (ConfigurationException e2) {
                LOGGER.error("Cannot read network configuration");
                throw new RuntimeException(e2);
            }
        }

        //instantiating InetAddress to resolve local IP
        try{
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e){
            LOGGER.error("Cannot instantiate IP resolver");
            throw new RuntimeException(e);
        }

        String[] urls = config.getStringArray("node.url");
        processes = new ArrayList<DC_Byzantine_RMI>();

        //bind local processes and locate remote ones
        int processIndex = 0;
        for (String url : urls){
            try{
                DC_Byzantine_RMI process;
                if (isProcessLocal(url)){
                    process = new DC_Byzantine(urls, processIndex);
                    LOGGER.debug("Process " + processIndex + " is local.");
                    new Thread((DC_Byzantine)process).start();
                    Naming.bind(url, process);
                } else {
                    process = (DC_Byzantine_RMI)Naming.lookup(url);
                    LOGGER.debug("Looking up for process with URL " + url);
                }
                processIndex++;
                processes.add(process);
                
            } catch (RemoteException e1){
                throw new RuntimeException(e1);
            } catch (AlreadyBoundException e2){
                throw new RuntimeException(e2);
            } catch (NotBoundException e3){
                throw new RuntimeException(e3);
            } catch (MalformedURLException e4){
                throw new RuntimeException(e4);
            }
            
        }
    }

    private boolean isProcessLocal(String url){
        return url.startsWith(RMI_PREFIX + inetAddress.getHostAddress()) ||
               url.startsWith(RMI_PREFIX + "localhost") ||
               url.startsWith(RMI_PREFIX + "127.0.0.1");
    }

    public List<DC_Byzantine_RMI> getProcesses(){
        return processes;
    }

}
