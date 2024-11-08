package org.slasoi.adhoc.workload;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.slasoi.adhoc.workload.agent.Agent;
import org.slasoi.adhoc.workload.common.Alive;
import org.slasoi.adhoc.workload.common.Sleep;
import org.slasoi.adhoc.workload.input.JSONMain;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.MessageListener;
import org.slasoi.common.workloadmessage.WorkloadMessage;
import org.slasoi.common.workloadmessage.WorkloadMessage.MessageType;
import eu.xtreemos.xconsole.blocks.XConsole;

/**
 * Generate workload on services.
 */
public class WorkloadGenerator {

    static Logger logger = Logger.getLogger(WorkloadGenerator.class);

    private static final String qName = ".webservices.tradingsystem.cocome.org/";

    private static String channel = PubSubManagerSingleton.CHANNEL;

    private static String guid = "";

    private static String guiGuid = null;

    private static String pubSubPropertiesFile = "manager.properties";

    private static JSONMain config = null;

    private static AdhocJobManager jobs = null;

    private static Alive alive;

    public static String getChannel() {
        return channel;
    }

    public static void setChannel(String channel) {
        WorkloadGenerator.channel = channel;
    }

    /**
	 * Starts communication over message bus 
	 * @throws MessagingException 
	 */
    public static void startCommunications() throws MessagingException {
        final XConsole console = new XConsole();
        try {
            PubSubManagerSingleton.init(pubSubPropertiesFile);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        PubSubManagerSingleton.getInstance().addMessageListener(new MessageListener() {

            @SuppressWarnings("static-access")
            public void processMessage(MessageEvent messageEvent) {
                if (messageEvent.getMessage() instanceof WorkloadMessage) {
                    WorkloadMessage workloadMessage = (WorkloadMessage) messageEvent.getMessage();
                    if (workloadMessage.getGuid().equals(guid)) {
                        if (guiGuid != null) {
                            if (guiGuid.equals(workloadMessage.getGuiGuid())) {
                                return;
                            }
                        }
                        WorkloadMessage message = new WorkloadMessage(channel, "");
                        switch(((WorkloadMessage) messageEvent.getMessage()).getMessageType()) {
                            case TEXT:
                                break;
                            case CMD:
                                String fullcommand = workloadMessage.getPayload().toString();
                                console.processLine(fullcommand);
                                break;
                            case LOCK_WORKLOAD:
                                guiGuid = workloadMessage.getPayload().toString();
                                startAlive(true);
                                break;
                            case UNLOCK_WORKLOAD:
                                guiGuid = null;
                                startAlive(false);
                                break;
                            case JSON:
                                message.setMessageType(MessageType.CMDRESPONSE);
                                message.setGuid(guid);
                                message.setPayload("gotConfig");
                                try {
                                    PubSubManagerSingleton.getInstance().publish(message);
                                } catch (MessagingException e1) {
                                    e1.printStackTrace();
                                }
                                if (jobs != null) {
                                    if (!jobs.areAllJobsIdle()) {
                                        message.setPayload("jobs are not all idle, \nnew config refused,\nstop jobs first or wait for them to finish");
                                        try {
                                            PubSubManagerSingleton.getInstance().publish(message);
                                        } catch (MessagingException e1) {
                                            e1.printStackTrace();
                                        }
                                        break;
                                    }
                                    jobs.quit(false);
                                    while (!jobs.areAllJobsEnded()) {
                                        try {
                                            logger.info("waiting for jobs to finish...");
                                            Thread.sleep(100);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                config = new JSONMain(workloadMessage.getPayload().toString());
                                jobs = config.getAdhocJobManager("", qName, guid);
                                message.setMessageType(MessageType.CMDRESPONSE);
                                message.setGuid(guid);
                                message.setPayload("parsed Config");
                                try {
                                    PubSubManagerSingleton.getInstance().publish(message);
                                } catch (MessagingException e1) {
                                    e1.printStackTrace();
                                }
                                try {
                                    jobs.run();
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                                message.setMessageType(MessageType.CMDRESPONSE);
                                message.setPayload("JobZ runing");
                                try {
                                    PubSubManagerSingleton.getInstance().publish(message);
                                } catch (MessagingException e1) {
                                    e1.printStackTrace();
                                }
                                message.setMessageType(MessageType.CONFIGURED);
                                message.setPayload("");
                                try {
                                    PubSubManagerSingleton.getInstance().publish(message);
                                } catch (MessagingException e1) {
                                    e1.printStackTrace();
                                }
                                break;
                            case TERMINATE:
                                try {
                                    PubSubManagerSingleton.getInstance().close();
                                } catch (MessagingException e) {
                                    logger.warn(e);
                                }
                                System.exit(0);
                        }
                    }
                }
            }
        });
        PubSubManagerSingleton.getInstance().createAndSubscribe(new Channel(channel));
    }

    /**
	 * stop connection to message bus
	 */
    public static void stopCommunications() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            PubSubManagerSingleton.getInstance().close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
	 * controll all   
	 * @throws MalformedURLException 
	 * @throws MessagingException 
	 */
    private static void commander() throws MalformedURLException, MessagingException {
        startCommunications();
        Agent a;
        String s = "";
        WorkloadMessage message = new WorkloadMessage(channel, "");
        message.setMessageType(MessageType.TEXT);
        message.setGuid(guid);
        startAlive(false);
        while (config == null) {
            Sleep.Sleep(300);
        }
        while (jobs == null || !(jobs.isFinished())) {
            Sleep.Sleep(500);
        }
        alive.stop();
        stopCommunications();
    }

    private static void startAlive(Boolean locked) {
        if (alive != null) {
            if (alive.isRunning()) {
                alive.stop();
            }
        }
        alive = new Alive(guid, channel, locked);
        Thread t = new Thread(alive);
        t.start();
    }

    private static void createGuid() {
        guid = "" + System.currentTimeMillis();
        PubSubManagerSingleton.GUID = guid;
    }

    /**
	 * Calls number of agents that do the simulation.
	 * @throws IOException 
	 */
    public static void main(String[] args) throws IOException {
        PropertyConfigurator.configure("log4j/log4j.properties");
        createGuid();
        try {
            commander();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
