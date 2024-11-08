package org.omnisys.services;

import java.io.IOException;
import java.util.List;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.omnisys.devices.Login;
import org.omnisys.system.Configuration;
import org.omnisys.system.ConfigurationFacade;

/**
 *
 * @author Brendan Smith
 */
public class SMTPRequest {

    int connectionResult;

    String commandResult = new String();

    boolean markAsDown = false;

    static Logger omnilog = Logger.getLogger("org.omnisys.services.SMTPRequest");

    ConfigurationFacade configurationFacade = new ConfigurationFacade();

    Configuration configuration = (Configuration) configurationFacade.getConfigurationByModule("protocol.timeout.SMTP");

    int timeout = Integer.parseInt(configuration.getValue());

    /** Creates a new instance of SMTPRequest */
    public SMTPRequest() {
    }

    public int tryToConnect(long serviceId, int port) throws IOException, Exception {
        SMTPClient client = new SMTPClient();
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
            int result = client.getReplyCode();
            if (!SMTPReply.isPositiveCompletion(result)) {
                service.setStatus("down");
                serviceFacade.saveService(service);
                omnilog.error("Couldn't connect to SMTP Service");
            } else {
                client.login();
                service.setStatus("up");
                serviceFacade.saveService(service);
                omnilog.debug("Connected to SMTP service on " + hostname);
            }
        } catch (Exception e) {
            omnilog.error("Error trying to connect to SMTP server at " + hostname + " on port " + port);
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
        SMTPClient client = new SMTPClient();
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
            int result = client.getReplyCode();
            if (!SMTPReply.isPositiveCompletion(result)) {
                service.setStatus("down");
                serviceFacade.saveService(service);
                omnilog.error("Couldn't connect to SMTP Service");
            } else {
                client.login();
                client.sendCommand(commandToRun);
                commandResult = client.getReplyString();
                service.setStatus("up");
                serviceFacade.saveService(service);
            }
        } catch (Exception e) {
            omnilog.error("Error trying to connect to SMTP server at " + hostname + " on port " + port);
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
        SMTPClient client = new SMTPClient();
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
            int result = client.getReplyCode();
            if (!SMTPReply.isPositiveCompletion(result)) {
                service.setStatus("down");
                serviceFacade.saveService(service);
                omnilog.error("Couldn't connect to SMTP Service");
            } else {
                client.login();
                for (int i = 0; i < commandsToRun.size(); i++) {
                    client.sendCommand((String) commandsToRun.get(i));
                    commandResults[i] = client.getReplyString();
                }
                service.setStatus("up");
                serviceFacade.saveService(service);
            }
        } catch (Exception e) {
            omnilog.error("Error trying to connect to SMTP server at " + hostname + " on port " + port);
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
