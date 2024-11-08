package fipa.adst.agents.amc.rsfp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.commons.codec.binary.Base64;
import fipa.adst.util.classloader.ByteArrayJarClassLoader;
import fipa.adst.util.ip.RequestInitiator;
import fipa.adst.util.ip.TimeOutException;
import fipa.cs.ontology.CipherDataAction;
import fipa.cs.ontology.CipherDescription;
import fipa.cs.ontology.CryptoServiceOntology;
import fipa.cs.ontology.DecipherDataAction;
import fipa.cs.ontology.GetHashAction;
import fipa.cs.ontology.InformCipheredDataPredicate;
import fipa.cs.ontology.InformDecipheredDataPredicate;
import fipa.cs.ontology.InformHashPredicate;
import fipa.cs.ontology.InformSignaturePredicate;
import fipa.cs.ontology.InformVerifiedDataPredicate;
import fipa.cs.ontology.SignDataAction;
import fipa.cs.ontology.SignDescription;
import fipa.cs.ontology.SignatureVerifierDescription;
import fipa.cs.ontology.VerifySignatureAction;
import jade.content.Predicate;
import jade.content.lang.leap.LEAPCodec;
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

public class LeapCodecPerformanceAgentModel extends Agent {

    public LeapCodecPerformanceAgentModel() {
    }

    /**
	 * Method to initialise the agent.
	 */
    protected void setup() {
        Object[] args = getArguments();
        if (args != null) {
            if (args.length == 4) {
                _iterations = Integer.valueOf(args[1].toString());
                _fatherName = args[2].toString();
                _perfInstance = args[3].toString();
                _finalItinerary = new LinkedList<AID>();
                _protectedData = null;
                addBehaviour(new ControlCode());
            } else help();
        } else help();
    }

    private void help() {
        System.out.println("\n\nUse: MobileAgent(\"agent_properties_file_name\",\"msg_to_display\")\n");
        System.out.println("This is a mobile agent that travels between agencies. It follows the");
        System.out.println("itinerary written in the agent properties file and shows a");
        System.out.println("message in every platform. The arguments are: \n\n");
        System.out.println("   agent_properties_file_name      name of the file containing");
        System.out.println("                                   the agent itinerary.\n");
        System.out.println("   msg_to_display                  message that agent displays");
        System.out.println("                                   on every platform\n\n");
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
            switch(_state) {
                case 0:
                    {
                        _logger = Logger.getMyLogger(getClass().getName());
                        _state = 4;
                        try {
                            Properties p = new Properties();
                            JarClassLoader jcl = (JarClassLoader) myAgent.getClass().getClassLoader();
                            InputStream streamItinerary = jcl.getResourceAsStream(JAR_ITINERARY);
                            p.load(streamItinerary);
                            _suggestedItinerary = new LinkedList<ItineraryItem>();
                            _homeAgentPlatform = new AID();
                            parseItinerary(p, _suggestedItinerary, _homeAgentPlatform);
                            if (_suggestedItinerary.size() > 0) {
                                _itItem = _suggestedItinerary.remove();
                                _state = 1;
                            } else {
                                _state = 4;
                            }
                        } catch (Exception e) {
                            System.out.println("Error reading agent file properties.");
                            System.out.println(e);
                        }
                        break;
                    }
                case 1:
                    {
                        getContentManager().registerLanguage(new LEAPCodec());
                        getContentManager().registerOntology(CryptoServiceOntology.getInstance());
                        if (_tasks == null) _tasks = new LinkedList<Behaviour>();
                        if (_logger == null) _logger = Logger.getMyLogger(getClass().getName());
                        loadAgentCode(_tasks, _previousLocation);
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
                        if (_suggestedItinerary.size() > 0) {
                            _itItem = _suggestedItinerary.remove();
                            AID remoteAMM = _itItem.getLocation();
                            _finalItinerary.addLast(remoteAMM);
                            _state = 1;
                            _protectedCode = protectAgentCode(_unProtectedCode, remoteAMM.getHap());
                            _previousLocation = getAMS().getHap();
                            doMove((new PlatformID(remoteAMM)));
                        } else {
                            _protectedCode = protectAgentCode(_unProtectedCode, _homeAgentPlatform.getHap());
                            doMove((new PlatformID(_homeAgentPlatform)));
                            _state = 4;
                        }
                        break;
                    }
                case 4:
                    {
                        _iterations--;
                        if (_iterations == 0) {
                            _done = true;
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            AID aidAddress = new AID(_fatherName, AID.ISGUID);
                            msg.addReceiver(aidAddress);
                            msg.setInReplyTo(_perfInstance);
                            msg.setContent(getLocalName());
                            send(msg);
                            if (_logger == null) _logger = Logger.getMyLogger(getClass().getName());
                            doDelete();
                        } else {
                            _state = 0;
                        }
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
            return _done;
        }

        /**
		 * List of running tasks. 
		 */
        LinkedList<Behaviour> _tasks;
    }

    private Hashtable getAgentData(byte[] protectedData, String previousLocation) {
        byte[] decipheredData = null;
        byte[] verifiedData = null;
        Hashtable table = null;
        if (protectedData != null) {
            decipheredData = decipherCode(protectedData);
            verifiedData = verifyData(decipheredData, previousLocation);
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(verifiedData);
                ObjectInputStream ois = new ObjectInputStream(bais);
                table = (Hashtable) ois.readObject();
            } catch (IOException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: GetData: " + getName() + ": " + e);
            } catch (ClassNotFoundException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: GetData: " + getName() + ": " + e);
            }
        }
        return table;
    }

    private byte[] protectAgentData(Hashtable table, String keyAlias) {
        byte[] serialisedData = null;
        byte[] cipheredData = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(table);
            serialisedData = baos.toByteArray();
            oos.close();
            baos.close();
        } catch (IOException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: ProtectData: " + getName() + ": " + e);
        }
        if (serialisedData != null) {
            byte[] signedData;
            signedData = signData(serialisedData, false);
            cipheredData = cipherData(signedData, keyAlias);
        }
        return cipheredData;
    }

    private byte[] protectAgentCode(byte[] code, String keyAlias) {
        byte[] cipheredData = null;
        if (code != null) {
            byte[] signedData;
            signedData = signData(code, true);
            cipheredData = cipherData(signedData, keyAlias);
        }
        return cipheredData;
    }

    /**
	 * Load dynamically protected code.
	 * @param previousLocation
	 */
    private void loadAgentCode(LinkedList<Behaviour> scheduledTasks, String previousLocation) {
        byte[] decipheredCode;
        byte[] verifiedCode;
        byte[] publicKey = null;
        boolean firstHop = false;
        if (_protectedCode == null) {
            firstHop = true;
            String file = ((JarClassLoader) getClass().getClassLoader()).getJarFileName();
            file = file.substring(0, file.indexOf(".jar")) + ".scode";
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                _protectedCode = readFully(fis);
                fis.close();
            } catch (FileNotFoundException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: LoadAgentCode: " + getName() + ": " + e);
            } catch (IOException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: LoadAgentCode: " + getName() + ": " + e);
            }
        }
        decipheredCode = decipherCode(_protectedCode);
        if (firstHop) {
            try {
                InputStream certificateIS = getClass().getClassLoader().getResourceAsStream(JAR_AGENT_CERT);
                if ((certificateIS) == null) throw new IOException();
                publicKey = readFully(certificateIS);
            } catch (IOException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error reading agent public key: " + e);
                return;
            }
            _unProtectedCode = verifyCode(decipheredCode, publicKey);
        } else {
            _unProtectedCode = verifyData(decipheredCode, previousLocation);
        }
        decipheredCode = null;
        loadTasksFinal(scheduledTasks, _unProtectedCode);
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
        code = decipherAndVerifyCode(cipheredCode);
        loadTasksFinal(scheduledTasks, code);
    }

    private void loadTasksFinal(LinkedList<Behaviour> scheduledTasks, byte[] code) {
        if (code != null) {
            try {
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
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: LoadTasks: " + getName() + ": " + e);
            } catch (ClassNotFoundException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: LoadTasks: " + getName() + ". Class not found: " + e);
            } catch (InstantiationException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: LoadTasks: " + getName() + ". Error creating task: " + e);
            } catch (IllegalAccessException e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: LoadTasks: " + getName() + ". Error creating task: " + e);
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
	 * Method that requests the signature of data with the private key of the current platform.
	 * @param data Data to sign.
	 * @return Data signed.
	 */
    private byte[] signData(byte[] data, boolean encapsulateData) {
        DataStore privateDS = new DataStore();
        try {
            privateDS.put(RequestSignature.DS_UNSIGNED_CODE, data);
            privateDS.put(RequestSignature.DS_ENCAPSULATE_DATA, new Boolean(encapsulateData));
            new RequestSignature(privateDS, this).init();
        } catch (TimeOutException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error requesting the data signature: " + e);
        }
        return (byte[]) privateDS.remove(RequestSignature.DS_SIGNED_CODE);
    }

    class RequestSignature extends RequestInitiator {

        public RequestSignature(DataStore ds, Agent agent) {
            super(agent);
            _ds = ds;
            _agent = agent;
        }

        @Override
        protected void handleFailureRequest(ACLMessage failure) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestSignature: " + getName() + ". Error signing the agent code: " + failure.getContent());
        }

        @Override
        protected void handleInformRequest(ACLMessage inform) {
            Predicate predicate = null;
            byte[] signature;
            try {
                predicate = (Predicate) _agent.getContentManager().extractContent(inform);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestSignature: " + getName() + ": " + ERR_EXTRACT_CONTENT + e);
            }
            if (predicate != null) {
                if (predicate instanceof InformSignaturePredicate) {
                    signature = ((InformSignaturePredicate) predicate).getSignature();
                    if (signature != null) {
                        _ds.put(DS_SIGNED_CODE, signature);
                    } else {
                        if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestSignature: " + getName() + ": " + ERR_NULL_CONTENT);
                    }
                } else {
                    if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestSignature: " + getName() + ": " + ERR_INCORRECT_ACTION);
                }
            } else {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestSignature: " + getName() + ": " + ERR_NULL_ACTION);
            }
        }

        /**
		 * Method to request the code deciphering.
		 */
        protected ACLMessage prepareInitiation(ACLMessage request) {
            AID csAID = new AID();
            csAID.setLocalName(CS_AGENT_NAME);
            request.addReceiver(csAID);
            request.setLanguage(LEAPCodec.NAME);
            request.setOntology(CryptoServiceOntology.getInstance().getName());
            request.setReplyByDate(new Date(System.currentTimeMillis() + MSG_TIMEOUT));
            byte[] data = (byte[]) _ds.get(DS_UNSIGNED_CODE);
            boolean encapsulateData = ((Boolean) _ds.get(DS_ENCAPSULATE_DATA)).booleanValue();
            SignDescription sd = new SignDescription();
            sd.setData(data);
            sd.setEncapsulateData(encapsulateData);
            sd.setIncludeHash(true);
            SignDataAction sda = new SignDataAction();
            sda.setSignDataSignDescription(sd);
            Action act = new Action();
            act.setAction(sda);
            act.setActor(_agent.getAID());
            try {
                _agent.getContentManager().fillContent(request, act);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestSignature: " + getName() + ". Error filling message content: " + e);
                return null;
            }
            return request;
        }

        public static final String DS_SIGNED_CODE = "ciphered-code";

        public static final String DS_UNSIGNED_CODE = "deciphered-code";

        public static final String DS_ENCAPSULATE_DATA = "encapsulate-data";

        DataStore _ds;

        Agent _agent;
    }

    /**
	 * Method that requests the signature of data with the private key of the current platform.
	 * @param data Data to sign.
	 * @return Data signed.
	 */
    private byte[] cipherData(byte[] data, String keyAlias) {
        DataStore privateDS = new DataStore();
        try {
            privateDS.put(RequestCipher.DS_UNCIPHERED_CODE, data);
            privateDS.put(RequestCipher.DS_KEY_ALIAS, keyAlias);
            new RequestCipher(privateDS, this).init();
        } catch (TimeOutException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher:" + getName() + ". Error requesting the data ciphering: " + e);
        }
        return (byte[]) privateDS.remove(RequestCipher.DS_CIPHERED_CODE);
    }

    class RequestCipher extends RequestInitiator {

        public RequestCipher(DataStore ds, Agent agent) {
            super(agent);
            _ds = ds;
            _agent = agent;
        }

        @Override
        protected void handleFailureRequest(ACLMessage failure) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher: " + getName() + ". Error signing the agent code: " + failure.getContent());
        }

        @Override
        protected void handleInformRequest(ACLMessage inform) {
            Predicate predicate = null;
            byte[] cipheredData;
            try {
                predicate = (Predicate) _agent.getContentManager().extractContent(inform);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher: " + getName() + ": " + ERR_EXTRACT_CONTENT + e);
            }
            if (predicate != null) {
                if (predicate instanceof InformCipheredDataPredicate) {
                    cipheredData = ((InformCipheredDataPredicate) predicate).getCipheredData();
                    if (cipheredData != null) {
                        _ds.put(DS_CIPHERED_CODE, cipheredData);
                    } else {
                        if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher: " + getName() + ": " + ERR_NULL_CONTENT);
                    }
                } else {
                    if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher: " + getName() + ": " + ERR_INCORRECT_ACTION);
                }
            } else {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher: " + getName() + ": " + ERR_NULL_ACTION);
            }
        }

        /**
		 * Method to request the code deciphering.
		 */
        protected ACLMessage prepareInitiation(ACLMessage request) {
            AID csAID = new AID();
            csAID.setLocalName(CS_AGENT_NAME);
            request.addReceiver(csAID);
            request.setLanguage(LEAPCodec.NAME);
            request.setOntology(CryptoServiceOntology.getInstance().getName());
            request.setReplyByDate(new Date(System.currentTimeMillis() + MSG_TIMEOUT));
            byte[] data = (byte[]) _ds.get(DS_UNCIPHERED_CODE);
            String keyAlias = (String) _ds.get(DS_KEY_ALIAS);
            CipherDescription cd = new CipherDescription();
            cd.setData(data);
            cd.setTarget(keyAlias);
            CipherDataAction cda = new CipherDataAction();
            cda.setCipherDataCipherDescription(cd);
            Action act = new Action();
            act.setAction(cda);
            act.setActor(_agent.getAID());
            try {
                _agent.getContentManager().fillContent(request, act);
            } catch (Exception e) {
                if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: RequestCipher: " + getName() + ". Error filling message content: " + e);
                return null;
            }
            return request;
        }

        public static final String DS_CIPHERED_CODE = "ciphered-code";

        public static final String DS_UNCIPHERED_CODE = "deciphered-code";

        public static final String DS_KEY_ALIAS = "key-alias";

        DataStore _ds;

        Agent _agent;
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
                        _ds.put(DS_DECIPHERED_CODE, data);
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
            request.setLanguage(LEAPCodec.NAME);
            request.setOntology(CryptoServiceOntology.getInstance().getName());
            request.setReplyByDate(new Date(System.currentTimeMillis() + MSG_TIMEOUT));
            byte[] cipheredCode = (byte[]) _ds.get(DS_CIPHERED_CODE);
            DecipherDataAction dda = new DecipherDataAction();
            dda.setDecipherDataByteSequence(cipheredCode);
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

    private byte[] verifyData(byte[] codeAndSignature, String keyAlias) {
        DataStore privateDS = new DataStore();
        try {
            privateDS.put(RequestVerify.DS_SIGNED_CODE, codeAndSignature);
            privateDS.put(RequestVerify.DS_TARGET, keyAlias);
            privateDS.put(RequestVerify.DS_VERIFY_HASH, new Boolean(true));
            new RequestVerify(privateDS, this).init();
        } catch (TimeOutException e) {
            if (_logger.isLoggable(Logger.SEVERE)) _logger.log(Logger.SEVERE, "SelfProtectedAgent: " + getName() + ". Error requesting the code verification: " + e);
        }
        return (byte[]) privateDS.get(RequestVerify.DS_PLAIN_CODE);
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
                        _ds.put(DS_PLAIN_CODE, data);
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
            request.setLanguage(LEAPCodec.NAME);
            request.setOntology(CryptoServiceOntology.getInstance().getName());
            request.setReplyByDate(new Date(System.currentTimeMillis() + MSG_TIMEOUT));
            String target = null;
            byte[] signedCode = (byte[]) _ds.get(DS_SIGNED_CODE);
            byte[] certificate = null;
            boolean verifyHash = false;
            if (_ds.containsKey(DS_CERTIFICATE)) certificate = (byte[]) _ds.get(DS_CERTIFICATE);
            if (_ds.containsKey(DS_TARGET)) target = (String) _ds.get(DS_TARGET);
            if (_ds.containsKey(DS_VERIFY_HASH)) verifyHash = ((Boolean) _ds.get(DS_VERIFY_HASH)).booleanValue();
            VerifySignatureAction vsa = new VerifySignatureAction();
            SignatureVerifierDescription svd = new SignatureVerifierDescription();
            svd.setSignature(signedCode);
            if (certificate != null) svd.setCertificate(certificate);
            if (target != null) svd.setTarget(target);
            svd.setGetData(true);
            svd.setIncludesHash(verifyHash);
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

        public static final String DS_TARGET = "target";

        public static final String DS_VERIFY_HASH = "verify-hash";

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
        } else {
            System.out.println("Error: Home platform not defined!");
        }
    }

    private class ItineraryItem implements Serializable {

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

    private static final int MSG_TIMEOUT = 600000;

    private static final String AGENT_DATA_ENTRY = "entry";

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

    LinkedList<ItineraryItem> _suggestedItinerary;

    LinkedList<AID> _finalItinerary;

    AID _homeAgentPlatform;

    transient String _exception;

    transient byte[] _agentToken;

    transient Logger _logger;

    ItineraryItem _itItem;

    int index = 0;

    String _fatherName = null;

    String _perfInstance = null;

    int _iterations = 0;

    boolean _round = false;

    boolean _done = false;

    String _previousLocation = null;

    byte[] _protectedData = null;

    byte[] _protectedCode = null;

    byte[] _unProtectedCode = null;

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
}
