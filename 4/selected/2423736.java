package fipa.adst.agents.amc.sfp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.commons.codec.binary.Base64;
import fipa.adst.util.classloader.ByteArrayJarClassLoader;
import fipa.adst.util.ip.RequestInitiator;
import fipa.adst.util.ip.TimeOutException;
import fipa.cs.ontology.CryptoServiceOntology;
import fipa.cs.ontology.DecipherDataAction;
import fipa.cs.ontology.GetHashAction;
import fipa.cs.ontology.InformDecipheredDataPredicate;
import fipa.cs.ontology.InformHashPredicate;
import fipa.cs.ontology.InformVerifiedDataPredicate;
import fipa.cs.ontology.SignatureVerifierDescription;
import fipa.cs.ontology.VerifySignatureAction;
import jade.content.Predicate;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.PlatformID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.management.JarClassLoader;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class AgentModel extends Agent {

    public AgentModel() {
    }

    /**
	 * Method to initialise the agent.
	 */
    protected void setup() {
        _finalItinerary = new LinkedList<AID>();
        addBehaviour(new ControlCode());
    }

    private class Repeater extends CyclicBehaviour {

        @Override
        public void action() {
            System.out.println("working " + count);
            count++;
            this.block(1000);
        }

        int count = 0;
    }

    private class ControlCode extends SimpleBehaviour {

        /**
		 * Main method of the control code.
		 */
        public void action() {
            System.out.println(_state);
            switch(_state) {
                case 0:
                    {
                        _logger = Logger.getMyLogger(getClass().getName());
                        _tasks = new LinkedList<Behaviour>();
                        getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
                        getContentManager().registerOntology(CryptoServiceOntology.getInstance());
                        if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + myAgent.getName() + ": Loading itinerary.");
                        try {
                            Properties p = new Properties();
                            JarClassLoader jcl = (JarClassLoader) myAgent.getClass().getClassLoader();
                            InputStream streamItinerary = jcl.getResourceAsStream(JAR_ITINERARY);
                            p.load(streamItinerary);
                            _suggestedItinerary = new LinkedList<ItineraryItem>();
                            _homeAgentPlatform = new AID();
                            parseItinerary(p, _suggestedItinerary, _homeAgentPlatform);
                        } catch (Exception e) {
                            System.out.println("Error reading agent file properties.");
                            System.out.println(e);
                        }
                        _state = 1;
                        break;
                    }
                case 1:
                    {
                        if (_logger == null) _logger = Logger.getMyLogger(getClass().getName());
                        if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + myAgent.getName() + ": Searching itinerary position.");
                        System.out.println("--------------------------------------------");
                        System.out.println(_suggestedItinerary.size());
                        _itineraryIterator = _suggestedItinerary.iterator();
                        ItineraryItem itItem;
                        boolean stop = false;
                        while (_itineraryIterator.hasNext() && !stop) {
                            itItem = _itineraryIterator.next();
                            System.out.println(itItem.getLocation().getHap());
                            if (itItem.getLocation().getHap().equals(myAgent.getHap())) {
                                if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + myAgent.getName() + ": Itinerary position: " + myAgent.getHap());
                                loadTasks(itItem.getCode(), _tasks, myAgent.getName());
                                stop = true;
                            }
                        }
                        _state = 2;
                        break;
                    }
                case 2:
                    {
                        if (_tasks.isEmpty()) {
                            _state = 3;
                        } else {
                            Iterator<Behaviour> it = _tasks.iterator();
                            while (it.hasNext()) {
                                if (it.next().done()) it.remove();
                            }
                        }
                        break;
                    }
                case 3:
                    {
                        if (_itineraryIterator.hasNext()) {
                            AID remoteAMM = _itineraryIterator.next().getLocation();
                            _finalItinerary.addLast(remoteAMM);
                            if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + myAgent.getName() + ": Moving to " + remoteAMM.getName());
                            doMove((new PlatformID(remoteAMM)));
                        } else {
                            _state = 4;
                        }
                        break;
                    }
                case 4:
                    {
                        if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + myAgent.getName() + ": Ending agent.");
                        doDelete();
                        break;
                    }
                default:
                    {
                        if (_logger.isLoggable(Logger.WARNING)) _logger.log(Logger.WARNING, "SelfProtectedAgent: " + myAgent.getName() + ": Bad state: " + _state);
                    }
            }
        }

        /**
		 * States the behaviour has not ended its execution.
		 */
        public boolean done() {
            return false;
        }

        /**
		 * Possible states for the control code:
		 * 
		 *   0 - START   - Starting agent.
		 *   1 - PREPARE - Preparing behaviours to execute.
		 *   2 - TASKS   - Checking tasks finalisation.
		 *   3 - MOVE    - Setting new destination and moving.
		 *   4 - 1ST-FAILURE  - Mobility failed (sending to HAP).
		 *   5 - 2ND-FAILURE  - Mobility failed (killing agent).
		 *   
		 */
        int _state = 0;

        /**
		 * List of running tasks. 
		 */
        LinkedList<Behaviour> _tasks;
    }

    /**
	 * Method that decipher and loads the tasks (behaviours) 
	 * assigned to the current location.
	 * 
	 * @param codeFileName
	 */
    private void loadTasks(String codeFileName, LinkedList<Behaviour> scheduledTasks, String agentName) {
        InputStream cipheredCode;
        byte[] code;
        cipheredCode = getClass().getClassLoader().getResourceAsStream(JAR_CODE_PATH + codeFileName + CIPHERED_CODE_EXTENSION);
        if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + agentName + ": Deciphering and verifying code.");
        code = decipherAndVerifyCode(cipheredCode);
        if (code != null) {
            try {
                if (_logger.isLoggable(Logger.INFO)) _logger.log(Logger.INFO, "SelfProtectedAgent: " + agentName + ": Loading tasks.");
                ByteArrayJarClassLoader classloader = new ByteArrayJarClassLoader(code, getClass().getClassLoader());
                ByteArrayInputStream bais = new ByteArrayInputStream(code);
                JarInputStream jis = new JarInputStream(bais);
                Manifest manifest = jis.getManifest();
                String tasksValue = manifest.getMainAttributes().getValue(JAR_AGENT_TASKS);
                String[] tasks = tasksValue.split(";");
                Class clazz;
                Behaviour b;
                for (int i = 0; i < tasks.length; i++) {
                    clazz = Class.forName(tasks[i], true, classloader);
                    b = (Behaviour) clazz.newInstance();
                    scheduledTasks.add(b);
                    addBehaviour(b);
                }
            } catch (IOException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ": " + e);
            } catch (ClassNotFoundException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Class not found: " + e);
            } catch (InstantiationException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error creating task: " + e);
            } catch (IllegalAccessException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error creating task: " + e);
            }
        }
    }

    /**
	 * Method to decipher a piece of code. It contacts with the
	 * local cryptographic service to request for the code deciphering.
	 *  
	 * @param cipheredCode Budget of data with the ciphered code.
	 * @return 
	 */
    private byte[] decipherAndVerifyCode(InputStream cipheredCode) {
        byte[] code, publicKey;
        try {
            code = decipherCode(readFully(cipheredCode));
            if (code == null) throw new IOException();
        } catch (IOException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error deciphering code: " + e);
            return null;
        }
        try {
            InputStream certificateIS = getClass().getClassLoader().getResourceAsStream(JAR_AGENT_CERT);
            if ((certificateIS) == null) throw new IOException();
            publicKey = readFully(certificateIS);
        } catch (IOException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error reading agent public key: " + e);
            return null;
        }
        code = verifyCode(code, publicKey);
        return code;
    }

    /**
	 * 
	 * @param cipheredCode
	 * @return
	 */
    private byte[] decipherCode(byte[] cipheredCode) {
        DataStore privateDS = new DataStore();
        try {
            privateDS.put(RequestDecipher.DS_CIPHERED_CODE, cipheredCode);
            new RequestDecipher(privateDS, this).init();
        } catch (TimeOutException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error requesting the code deciphering: " + e);
        }
        return (byte[]) privateDS.remove(RequestDecipher.DS_DECIPHERED_CODE);
    }

    class RequestDecipher extends RequestInitiator {

        public RequestDecipher(DataStore ds, Agent agent) {
            super(agent);
            _ds = ds;
            _agent = agent;
        }

        @Override
        protected void handleFailureRequest(ACLMessage failure) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestDecipher: " + getName() + ". Error deciphering the agent code: " + failure.getContent());
        }

        @Override
        protected void handleInformRequest(ACLMessage inform) {
            Predicate predicate = null;
            byte[] data;
            try {
                predicate = (Predicate) _agent.getContentManager().extractContent(inform);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestDecipher: " + getName() + ": " + ERR_EXTRACT_CONTENT + e);
            }
            if (predicate != null) {
                if (predicate instanceof InformDecipheredDataPredicate) {
                    data = ((InformDecipheredDataPredicate) predicate).getDecipheredData();
                    if (data != null) {
                        _ds.put(DS_DECIPHERED_CODE, Base64.decodeBase64(data));
                    } else {
                        if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestDecipher: " + getName() + ": " + ERR_NULL_CONTENT);
                    }
                } else {
                    if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestDecipher: " + getName() + ": " + ERR_INCORRECT_ACTION);
                }
            } else {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestDecipher: " + getName() + ": " + ERR_NULL_ACTION);
            }
        }

        /**
		 * Method to request the code deciphering.
		 */
        protected ACLMessage prepareInitiation(ACLMessage request) {
            AID csAID = new AID();
            csAID.setLocalName(CS_AGENT_NAME);
            request.addReceiver(csAID);
            request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
            request.setOntology(CryptoServiceOntology.getInstance().getName());
            request.setReplyByDate(new Date(System.currentTimeMillis() + MSG_TIMEOUT));
            byte[] cipheredCode = (byte[]) _ds.get(DS_CIPHERED_CODE);
            DecipherDataAction dda = new DecipherDataAction();
            dda.setDecipherDataByteSequence(Base64.encodeBase64(cipheredCode));
            Action act = new Action();
            act.setAction(dda);
            act.setActor(_agent.getAID());
            try {
                _agent.getContentManager().fillContent(request, act);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestDecipher: " + getName() + ". Error filling message content: " + e);
                return null;
            }
            return request;
        }

        public static final String DS_CIPHERED_CODE = "ciphered-code";

        public static final String DS_DECIPHERED_CODE = "deciphered-code";

        DataStore _ds;

        Agent _agent;
    }

    private byte[] verifyCode(byte[] codeAndSignature, byte[] certificate) {
        DataStore privateDS = new DataStore();
        try {
            privateDS.put(RequestVerify.DS_SIGNED_CODE, codeAndSignature);
            privateDS.put(RequestVerify.DS_CERTIFICATE, certificate);
            new RequestVerify(privateDS, this).init();
        } catch (TimeOutException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error requesting the code verification: " + e);
        }
        return (byte[]) privateDS.get(RequestVerify.DS_PLAIN_CODE);
    }

    class RequestVerify extends RequestInitiator {

        public RequestVerify(DataStore ds, Agent agent) {
            super(agent);
            _ds = ds;
            _agent = agent;
        }

        @Override
        protected void handleFailureRequest(ACLMessage failure) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestVerify: " + getName() + ". Error deciphering the agent code: " + failure.getContent());
        }

        @Override
        protected void handleInformRequest(ACLMessage inform) {
            Predicate predicate = null;
            byte[] data;
            try {
                predicate = (Predicate) _agent.getContentManager().extractContent(inform);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestVerify: " + getName() + ": " + ERR_EXTRACT_CONTENT + e);
            }
            if (predicate != null) {
                if (predicate instanceof InformVerifiedDataPredicate) {
                    data = ((InformVerifiedDataPredicate) predicate).getVerifiedData();
                    if (data != null) {
                        _ds.put(DS_PLAIN_CODE, Base64.decodeBase64(data));
                    } else {
                        if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestVerify: " + getName() + ": " + ERR_NULL_CONTENT);
                    }
                } else {
                    if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestVerify: " + getName() + ": " + ERR_INCORRECT_ACTION);
                }
            } else {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestVerify: " + getName() + ": " + ERR_NULL_ACTION);
            }
        }

        /**
		 * Method to request the code deciphering.
		 */
        protected ACLMessage prepareInitiation(ACLMessage request) {
            AID csAID = new AID();
            csAID.setLocalName(CS_AGENT_NAME);
            request.addReceiver(csAID);
            request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
            request.setOntology(CryptoServiceOntology.getInstance().getName());
            request.setReplyByDate(new Date(System.currentTimeMillis() + MSG_TIMEOUT));
            byte[] signedCode = (byte[]) _ds.get(DS_SIGNED_CODE);
            byte[] certificate = (byte[]) _ds.get(DS_CERTIFICATE);
            VerifySignatureAction vsa = new VerifySignatureAction();
            SignatureVerifierDescription svd = new SignatureVerifierDescription();
            svd.setSignature(Base64.encodeBase64(signedCode));
            svd.setCertificate(Base64.encodeBase64(certificate));
            svd.setGetData(true);
            svd.setIncludesHash(false);
            vsa.setVerifySignatureSignatureVerifierDescription(svd);
            Action act = new Action();
            act.setAction(vsa);
            act.setActor(_agent.getAID());
            try {
                _agent.getContentManager().fillContent(request, act);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestVerify: " + getName() + ". Error filling message content: " + e);
                return null;
            }
            return request;
        }

        public static final String DS_SIGNED_CODE = "signed-code";

        public static final String DS_CERTIFICATE = "certificate";

        public static final String DS_PLAIN_CODE = "plain-code";

        DataStore _ds;

        Agent _agent;
    }

    /**
	 * Support class to create a byte array from an input stream.
	 * @param is Source input stream.
	 * @return Target byte array.
	 * @throws IOException
	 */
    private byte[] readFully(InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buffer)) >= 0) baos.write(buffer, 0, read);
        return baos.toByteArray();
    }

    /**
	 * Method to parse the itinerary of the agent.
	 * 
	 * @param Properties to fulfill.
	 */
    private void parseItinerary(Properties p, LinkedList<ItineraryItem> itinerary, AID homeAgentPlatform) {
        String platform;
        int i = 0;
        ItineraryItem item;
        AID aid;
        while (!(platform = p.getProperty("hop" + i + "_platform", "")).equals("")) {
            item = new ItineraryItem();
            aid = new AID();
            aid.setName(platform);
            aid.addAddresses(p.getProperty("hop" + i + "_address", ""));
            item.setLocation(aid);
            item.setCode(p.getProperty("hop" + i + "_code", ""));
            itinerary.add(item);
            i++;
        }
        if (!(platform = p.getProperty("home_platform", "")).equals("")) {
            item = new ItineraryItem();
            homeAgentPlatform.setName(platform);
            homeAgentPlatform.addAddresses(p.getProperty("home_address", ""));
            item.setLocation(homeAgentPlatform);
            item.setCode(p.getProperty("home_code", ""));
            itinerary.add(item);
        } else {
            System.out.println("Error: Home platform not defined!");
        }
    }

    private class ItineraryItem {

        public AID getLocation() {
            return location;
        }

        public void setLocation(AID location) {
            this.location = location;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        private AID location;

        private String code;
    }

    private static final String JAR_ITINERARY = "conf/itinerary.conf";

    private static final String JAR_CODE_PATH = "codes/";

    private static final String JAR_AGENT_TASKS = "tasks";

    private static final String JAR_AGENT_CERT = "conf/agent.cert";

    private static final String CIPHERED_CODE_EXTENSION = ".scode";

    private static final String CS_AGENT_NAME = "cs";

    private static final int MSG_TIMEOUT = 30000;

    private static final String ERR_FILLING_MSG_CONTENT = "Error filling message content";

    private static final String ERR_EXTRACT_CONTENT = "Error: The message content cannot be extracted";

    private static final String ERR_INCORRECT_ACTION = "Error: Incorrect action received";

    private static final String ERR_NULL_ACTION = "Error: Null action received";

    private static final String ERR_NULL_CONTENT = "Error: Null content received";

    private static final String ERR_HANDLE_NOT_UNDERSTOOD = "Error: Handle not understood";

    private static final String ERR_OUT_OF_SEQUENCE = "Error: Out of sequence message";

    private static final String ERR_HANDLE_REFUSE = "Error: Refuse message received";

    private static final String ERR_HANDLE_AGREE = "Error: Agree message received";

    private static final String ERR_HANDLE_FAILURE = "Error: The hash cannot be obtained";

    transient LinkedList<ItineraryItem> _suggestedItinerary;

    transient Iterator<ItineraryItem> _itineraryIterator;

    LinkedList<AID> _finalItinerary;

    transient AID _homeAgentPlatform;

    transient String _exception;

    transient byte[] _agentToken;

    transient Logger _logger;
}
