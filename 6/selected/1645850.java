package org.omnisys.services;

import java.io.IOException;
import java.util.List;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.omnisys.devices.Login;
import org.omnisys.system.Configuration;
import org.omnisys.system.ConfigurationFacade;

/**
 *
 * @author Brendan Smith
 */
public class POP3Request {

    int connectionResult;

    String commandResult = new String();

    boolean markAsDown = false;

    static Logger omnilog = Logger.getLogger("org.omnisys.services.POP3Request");

    ConfigurationFacade configurationFacade = new ConfigurationFacade();

    Configuration configuration = (Configuration) configurationFacade.getConfigurationByModule("protocol.timeout.POP3");

    int timeout = Integer.parseInt(configuration.getValue());

    /** Creates a new instance of POP3Request */
    public POP3Request() {
    }

    public int tryToConnect(long serviceId, int port) throws IOException, Exception {
        POP3Client client = new POP3Client();
        client.setConnectTimeout(timeout);
        ServiceFacade serviceFacade = new ServiceFacade();
        Session session = org.zkoss.zkplus.hibernate.HibernateUtil.getSessionFactory().getCurrentSession();
        Service service = (Service) session.load(Service.class, serviceId);
        String hostname = service.getDevice().getName();
        Login login = service.getLogin();
        String username = login.getArg1();
        String password = login.getArg2();
        try {
            client.connect(hostname, port);
            if (!username.trim().equals("") && !password.trim().equals("")) {
                if (client.login(username, password)) {
                    connectionResult = 0;
                } else {
                    connectionResult = 1;
                    service.setStatus("down");
                    serviceFacade.saveService(service);
                }
            } else {
                connectionResult = 0;
                service.setStatus("up");
                serviceFacade.saveService(service);
            }
        } catch (Exception e) {
            omnilog.error("Error trying to connect to POP3 server at " + hostname + " on port " + port);
            connectionResult = 1;
            service.setStatus("down");
            serviceFacade.saveService(service);
            throw e;
        } finally {
            client.logout();
            client.disconnect();
        }
        return connectionResult;
    }

    public String runCommand(long serviceId, int port, String commandToRun) throws Exception {
        POP3Client client = new POP3Client();
        client.setConnectTimeout(timeout);
        Session session = org.zkoss.zkplus.hibernate.HibernateUtil.getSessionFactory().getCurrentSession();
        ServiceFacade serviceFacade = new ServiceFacade();
        Service service = (Service) session.load(Service.class, serviceId);
        String hostname = service.getDevice().getName();
        Login login = service.getLogin();
        String username = login.getArg1();
        String password = login.getArg2();
        try {
            client.connect(hostname, port);
            if (!username.trim().equals("") && !password.trim().equals("")) {
                if (client.login(username, password)) {
                    client.sendCommand(commandToRun);
                    commandResult = client.getReplyString();
                } else {
                    service.setStatus("down");
                    serviceFacade.saveService(service);
                }
            } else {
                client.sendCommand(commandToRun);
                commandResult = client.getReplyString();
                service.setStatus("up");
                serviceFacade.saveService(service);
            }
        } catch (Exception e) {
            omnilog.error("Error trying to connect to POP3 server at " + hostname + " on port " + port);
            service.setStatus("down");
            serviceFacade.saveService(service);
            throw e;
        } finally {
            client.logout();
            client.disconnect();
        }
        return commandResult;
    }

    public String[] runCommands(long serviceId, int port, List commandsToRun) throws Exception {
        String[] commandResults = new String[commandsToRun.size()];
        POP3Client client = new POP3Client();
        client.setConnectTimeout(timeout);
        Session session = org.zkoss.zkplus.hibernate.HibernateUtil.getSessionFactory().getCurrentSession();
        ServiceFacade serviceFacade = new ServiceFacade();
        Service service = (Service) session.load(Service.class, serviceId);
        String hostname = service.getDevice().getName();
        Login login = service.getLogin();
        String username = login.getArg1();
        String password = login.getArg2();
        try {
            client.connect(hostname, port);
            if (!username.trim().equals("") && !password.trim().equals("")) {
                if (client.login(username, password)) {
                    for (int i = 0; i < commandsToRun.size(); i++) {
                        client.sendCommand((String) commandsToRun.get(i));
                        commandResults[i] = client.getReplyString();
                    }
                } else {
                    service.setStatus("down");
                    serviceFacade.saveService(service);
                }
            } else {
                for (int i = 0; i < commandsToRun.size(); i++) {
                    client.sendCommand((String) commandsToRun.get(i));
                    commandResults[i] = client.getReplyString();
                    service.setStatus("up");
                    serviceFacade.saveService(service);
                }
            }
        } catch (Exception e) {
            omnilog.error("Error trying to connect to POP3 server at " + hostname + " on port " + port);
            service.setStatus("down");
            serviceFacade.saveService(service);
            throw e;
        } finally {
            client.logout();
            client.disconnect();
        }
        return commandResults;
    }
}
