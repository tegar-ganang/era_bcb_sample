package edu.cmu.vlis.wassup.sender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Topic;
import org.mybeans.dao.DAOException;
import edu.cmu.vlis.wassup.databean.Event;
import edu.cmu.vlis.wassup.databean.User;
import edu.cmu.vlis.wassup.db.EventsForUserDAO;
import edu.cmu.vlis.wassup.db.UserDAO;
import edu.cmu.vlis.wassup.db.UserInterestDAO;
import edu.cmu.vlis.wassup.utils.MailClient;

public class TopicQueueFetcher implements MessageListener, Runnable {

    DBHandler dbHandler = null;

    Topic topic;

    BufferedWriter out1 = null;

    Event ann;

    String ret = null;

    Set<User> users = null;

    TopicQueueFetcher(Topic topic, BufferedWriter out1) {
        this.out1 = out1;
        try {
            out1.write("constructir TopicQfetcher");
            out1.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        this.topic = topic;
        this.initialize();
    }

    public void initialize() {
        try {
            out1.write("I initialized topic queue fetcher");
            out1.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            this.dbHandler = DBHandler.getInstance();
            dbHandler.setTopic(topic.getTopicName());
        } catch (JMSException e) {
            try {
                out1.write(e.toString());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Couldnt connect to DB.");
            try {
                out1.write(e.toString());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void onMessage(Message message) {
        System.err.println("I received a message");
        try {
            out1.write("I received a message");
            out1.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        ObjectMessage objMesg = (ObjectMessage) message;
        try {
            ann = (Event) objMesg.getObject();
            if (this.dbHandler == null) this.initialize();
            fetchProfile();
            if (users != null && users.size() != 0) {
                ret = this.dbHandler.commitToDB(ann);
                sendAlerts();
                out1.write("1 message insrted in the database. User found " + users.size() + "\n");
                out1.flush();
                users = null;
            } else {
                System.out.println("\nNo users for this Location");
                out1.write("\nNo users for this Location " + ann.getCity() + "\n");
                out1.flush();
            }
        } catch (JMSException e) {
            System.out.println("Error in connecting to JMS");
            System.out.println(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error while inserting message in DB.");
        }
    }

    void test(Event ann) {
        String subject = "Whats up Event on " + ann.getStartDate() + " " + ann.getStartTime() + " in " + ann.getCity();
        String message = "What : " + ann.getName() + "\n" + "Where: " + ann.getStreet() + "," + ann.getCity() + "," + ann.getState() + "\n" + "When: " + ann.getStartDate() + " - " + ann.getEndDate() + "\n" + "Time : " + ann.getStartTime() + " - " + ann.getEndTime() + "\n";
        System.out.println(subject + "\n" + message);
    }

    void sendAlerts() {
        try {
            EventsForUserDAO euEventsForUserDAO = new EventsForUserDAO();
            for (User p : users) {
                if (!euEventsForUserDAO.isEventExist(ann, p)) {
                    String subject = "Whats up Event on " + ann.getStartDate() + " " + ann.getStartTime() + " in " + ann.getCity();
                    String message = "What : " + ann.getName() + "\n" + "Where: " + ann.getStreet() + "," + ann.getCity() + "," + ann.getState() + "\n" + "When: " + ann.getStartDate() + " - " + ann.getEndDate() + "\n" + "Time : " + ann.getStartTime() + " - " + ann.getEndTime() + "\n";
                    if (ann.getUrl() != null && ann.getUrl() != "" && ann.getUrl() != " ") message = message + "Have a look at :" + ann.getUrl();
                    try {
                        MailClient client = new MailClient();
                        String to = p.getUserName();
                        System.out.println("Sending email to " + to);
                        out1.write("Sending email to " + to + "\n");
                        out1.flush();
                        client.sendMail(to, subject, message);
                        euEventsForUserDAO.insert(ann, p);
                        out1.write("Sent email to " + to + "\n");
                        out1.flush();
                    } catch (Exception e) {
                        continue;
                    }
                } else {
                    System.out.println("already send to user " + p.getUserName());
                    try {
                        out1.write("already send to user " + p.getUserName() + "\n");
                        out1.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (DAOException e1) {
            e1.printStackTrace();
        }
    }

    void fetchProfile() {
        User[] userLoc;
        User[] userTag;
        try {
            UserDAO um = new UserDAO();
            UserInterestDAO uim = new UserInterestDAO();
            out1.write("\nCity" + ann.getCity() + "Topic " + topic.getTopicName() + "1\n");
            out1.flush();
            userLoc = um.getLocationUsers(ann.getCity());
            if (topic.getTopicName() != null) {
                userTag = uim.getUsersInterestedIn(topic.getTopicName().trim());
                Set<User> u1 = new HashSet<User>();
                for (int i = 0; i < userLoc.length; i++) {
                    u1.add(userLoc[i]);
                }
                Set<User> u2 = new HashSet<User>();
                for (int i = 0; i < userTag.length; i++) {
                    u2.add(userTag[i]);
                }
                u1.retainAll(u2);
                users = u1;
            }
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
    }
}
