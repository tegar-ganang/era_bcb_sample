package diet.server.ConversationController;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.JOptionPane;
import diet.message.MessageChatTextFromClient;
import diet.message.MessageKeypressed;
import diet.message.MessageWYSIWYGDocumentSyncFromClientInsert;
import diet.parameters.ExperimentSettings;
import diet.server.Conversation;
import diet.server.Participant;

public class CCWEBPAGEDISPLAY extends DefaultConversationController {

    String VERSIONDIRKVAR = "VERSIONDIRK=19nov2008, gui build";

    Hashtable displaysOfParticipants = new Hashtable();

    int displaysOfParticipantsCounter = 0;

    int widthOfDisplay = 350;

    int heightOfDisplay = 280;

    Integer maxFrameNum = 8548;

    int DMovieLoopCountMax = 10;

    int DMovieLoopCounter = 0;

    Hashtable alteredNames = new Hashtable();

    String[] publicnamelist = { "Aparna", "Beth" };

    String DMovieServer = "http://padfoot.kent.ac.uk/~dpj/fauxvideo/";

    String dirsep = "/";

    public CCWEBPAGEDISPLAY() {
        alteredNames.put("1", "Aparna");
        alteredNames.put("2", "Beth");
    }

    @Override
    public void initialize(Conversation c, ExperimentSettings expSettings) {
        super.initialize(c, expSettings);
    }

    @Override
    public void processLoop() {
        c.resetFromIsTypingtoNormalChatAllAllowedParticipants(super.getIsTypingTimeOut());
        DMovieLoopCounter += 1;
        if (DMovieLoopCounter > DMovieLoopCountMax) {
            DMovieLoopCounter = 0;
            DMovieNextFrame();
        }
    }

    public void DMovieNextFrame() {
        Enumeration pplist = displaysOfParticipants.elements();
        while (pplist.hasMoreElements()) {
            ParticipantMovie pm = (ParticipantMovie) pplist.nextElement();
            Participant p = pm.me;
            Participant other = pm.other;
            if (other != null) {
                String readwrite = other.isTyping(super.getIsTypingTimeOut()) ? "write" : "read";
                if (readwrite.contentEquals("read")) {
                    String URL = pm.baseURL + readwrite + dirsep + String.format("%05d.html", pm.readingFrameNumber);
                    c.changeWebpage(p, "webpage", URL);
                    pm.readingFrameNumber = (pm.readingFrameNumber + 1) % maxFrameNum;
                } else {
                    String URL = pm.baseURL + readwrite + dirsep + String.format("%05d.html", pm.writingFrameNumber);
                    c.changeWebpage(p, "webpage", URL);
                    pm.writingFrameNumber = (pm.writingFrameNumber + 1) % maxFrameNum;
                }
            }
        }
    }

    @Override
    public void processChatText(Participant sender, MessageChatTextFromClient mct) {
        pTurnsElapsed.setValue(((Integer) pTurnsElapsed.getValue()) + 1);
        super.expSettings.generateParameterEvent(pTurnsElapsed);
        String participantID = sender.getParticipantID();
        String alteredName = "";
        Enumeration altlist = alteredNames.keys();
        while (altlist.hasMoreElements()) {
            String key = (String) altlist.nextElement();
            if (key.contentEquals(participantID)) {
                alteredName = (String) alteredNames.get(key);
                c.relayTurnToAllOtherParticipantsWithAlteredOriginName(sender, alteredName, mct);
                break;
            }
        }
        if (alteredName.contentEquals("")) {
            c.relayTurnToAllOtherParticipants(sender, mct);
        }
        c.sendLabelDisplayToAllowedParticipantsFromApparentOrigin(sender, "Status: OK", false);
    }

    @Override
    public void processKeyPress(Participant sender, MessageKeypressed mkp) {
    }

    @Override
    public void processWYSIWYGTextInserted(Participant sender, MessageWYSIWYGDocumentSyncFromClientInsert mWYSIWYGkp) {
        String participantID = sender.getParticipantID();
        String alteredName = "";
        Enumeration altlist = alteredNames.keys();
        while (altlist.hasMoreElements()) {
            String key = (String) altlist.nextElement();
            if (key.contentEquals(participantID)) {
                alteredName = (String) alteredNames.get(key);
                c.informIsTypingToAllowedParticipantswithAlteredOriginName(sender, alteredName);
                break;
            }
        }
        if (alteredName.contentEquals("")) {
            c.informIsTypingToAllowedParticipants(sender);
        }
    }

    @Override
    public void participantJoinedConversation(Participant p) {
        String alteredName = (String) alteredNames.get(p.getParticipantID());
        if (alteredName == null) {
            System.out.println("Participant joined, but ID not listed in alteredNames");
            JOptionPane.showMessageDialog(null, "Participant ID not recognised, this experiment may not run correctly");
            return;
        }
        ParticipantMovie pm = new ParticipantMovie(p, alteredName, DMovieServer);
        this.displaysOfParticipants.put(p.getParticipantID(), pm);
        System.out.println("Participant joined, altered name: " + p.getParticipantID() + " " + p.getUsername() + " " + alteredName);
        if (displaysOfParticipants.size() == 2) {
            Object[] plist = displaysOfParticipants.keySet().toArray();
            ParticipantMovie pm0 = (ParticipantMovie) displaysOfParticipants.get(plist[0]);
            ParticipantMovie pm1 = (ParticipantMovie) displaysOfParticipants.get(plist[1]);
            pm0.other = pm1.me;
            pm1.other = pm0.me;
            pm0.setBaseURL(pm1.alteredName);
            pm1.setBaseURL(pm0.alteredName);
            c.displayNEWWebpage(pm0.me, "webpage", "RemoteVideo for " + pm0.me.getUsername(), pm0.baseURL + "index.html", widthOfDisplay, heightOfDisplay, false, false);
            c.displayNEWWebpage(pm1.me, "webpage", "RemoteVideo for " + pm1.me.getUsername(), pm1.baseURL + "index.html", widthOfDisplay, heightOfDisplay, false, false);
        }
    }
}

class ParticipantMovie {

    public Participant other;

    public Participant me;

    public int readingFrameNumber = 0;

    public int writingFrameNumber = 0;

    public String baseURL;

    public String alteredName;

    String dirsep = "/";

    String DMovieServer = "??";

    public ParticipantMovie(Participant p, String alt, String DMovieServer) {
        me = p;
        alteredName = alt;
        this.DMovieServer = DMovieServer;
        setBaseURL(alteredName);
    }

    public void setBaseURL(String dirname) {
        baseURL = DMovieServer + dirname + dirsep;
    }
}
