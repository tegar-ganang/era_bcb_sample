import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import opencard.core.terminal.*;
import opencard.core.service.*;
import opencard.core.event.*;
import opencard.core.util.*;
import com.ibutton.oc.*;
import com.ibutton.oc.terminal.jib.*;
import com.ibutton.oc.JibMultiFactory;
import com.ibutton.oc.JibMultiListener;
import com.ibutton.oc.JibMultiEvent;

/**
 *
 * This class performs the communication with the Animal applet on the button.
 * The class acts as a wrapper for the low-level OpenCard communication with
 * the JavaCard applet simulating the animal.
 *
 * @author Henrik Eriksson
 * @version 0.02
 *
 * revision 981206: File created
 * revision 000117: Modified
 * revision 030102: Modified
 *
 * @see Animal
 * @see <a href="http://www.opencard.org">OpenCard</a>
 */
public class IButtonAnimalProxy extends AnimalProxy implements AnimalConstants {

    private SlotChannel channel;

    private int[] rom_id;

    private byte gender = Byte.MIN_VALUE;

    private byte legs = Byte.MIN_VALUE;

    private String species, creator, cry, picture, sound;

    /** Indicates that the iButton has been removed and that the IButtonAnimalProxy is inactive */
    private boolean removed = false;

    /**
   * Start up the communication system. This method performs the required
   * initialization of the communication link before the proxy is instantiated.
   * This method should be called before doing anything else.
   *
   * @param yard the farmyard object
   */
    public static void start(final Farmyard yard) {
        try {
            SmartCard.start();
            CardTerminalRegistry registry = CardTerminalRegistry.getRegistry();
            registry.setPollInterval(200);
            JibMultiFactory factory = new JibMultiFactory();
            factory.addJiBListener(new JibMultiListener() {

                public void iButtonInserted(JibMultiEvent event) {
                    Farmyard.playSound("inserted.au");
                    int slot = event.getSlotID();
                    SlotChannel channel = event.getChannel();
                    iButtonCardTerminal terminal = (iButtonCardTerminal) channel.getCardTerminal();
                    int[] buttonId = terminal.getiButtonId(channel.getSlotNumber());
                    IButtonAnimalProxy re_ap = (IButtonAnimalProxy) AnimalRegistry.getReusableAnimalProxy(buttonId);
                    if (re_ap == null) {
                        boolean selected = IButtonAnimalProxy.selectApplet(channel);
                        if (selected) {
                            LocationPanel lp = yard.getSelectedLocationPanel();
                            AnimalProxy new_ap;
                            if (lp != null) new_ap = lp.getFactory().createAnimalProxy(channel, buttonId); else {
                                try {
                                    new_ap = new IButtonAnimalProxy(channel, buttonId);
                                } catch (java.rmi.RemoteException e) {
                                    return;
                                }
                            }
                            yard.addAnimal(new_ap);
                        } else {
                            System.err.println("iButton applet \"Animal\" not found");
                            Farmyard.playSound("failure.au");
                        }
                    } else {
                        re_ap.setChannel(channel);
                    }
                }

                public void iButtonRemoved(JibMultiEvent event) {
                    Farmyard.playSound("removed.au");
                }
            });
            registry.addCTListener(factory);
        } catch (CardTerminalException e) {
            e.printStackTrace();
        } catch (CardServiceException e) {
            e.printStackTrace();
        } catch (OpenCardPropertyLoadingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
   *  Shut down the communication system. This method performs the required
   *  cleanup of the communication channel when the application shuts down.
   *  This method should be called when the proxy is no longer needed (i.e.,
   *  only when the application is about to close).
   */
    public static void shutdown() {
        try {
            SmartCard.shutdown();
        } catch (CardTerminalException ex) {
            ex.printStackTrace();
        }
    }

    /**
   *  Dummy constructor used by <code>AnimalProxySimulator</code>.
   */
    protected IButtonAnimalProxy() throws java.rmi.RemoteException {
    }

    /**
   *  Constructs an <code>IButtonAnimalProxy</code>. The animal proxy is the
   *  object that manages the communication with the <code>Animal</code>
   *  iButton applet.
   *
   *  @param sc the slot channel
   *  @param buttonId the iButton id (as an array of integers)
   */
    public IButtonAnimalProxy(SlotChannel sc, int[] buttonId) throws java.rmi.RemoteException {
        channel = sc;
        rom_id = buttonId;
        AnimalRegistry.add(this);
        final CardTerminalRegistry registry = CardTerminalRegistry.getRegistry();
        final JibMultiFactory factory = new JibMultiFactory();
        factory.addJiBListener(new JibMultiListener() {

            public void iButtonInserted(JibMultiEvent event) {
            }

            public void iButtonRemoved(JibMultiEvent event) {
                if (channel.getSlotNumber() == event.getSlotID()) {
                    Thread th = new Thread() {

                        public void run() {
                            if (AnimalRegistry.awaitRemove(IButtonAnimalProxy.this, 800)) {
                                removed = true;
                                processEvent(new AnimalEvent(IButtonAnimalProxy.this, 42));
                                registry.removeCTListener(factory);
                            }
                        }
                    };
                    try {
                        th.start();
                    } catch (IllegalThreadStateException e) {
                    }
                }
            }
        });
        registry.addCTListener(factory);
    }

    /**
   *  Gets the slot channel for this IButtonAnimalProxy.
   *  Normal programs should not have to call this method.
   *
   *  @see #setChannel(SlotChannel)
   *  @return the slot channel
   */
    public SlotChannel getChannel() {
        return channel;
    }

    /**
   *  Sets the slot channel for this IButtonAnimalProxy. This method is intended to
   *  be called if the original slot channel needs to be changed.
   *  Normal programs should not have to call this method.
   *
   *  @see #getChannel()
   *  @param sc the new channel
   */
    public void setChannel(SlotChannel sc) {
        channel = sc;
    }

    /**
   *  Processes events on this animal. If an event is an instance of
   *  AnimalEvent, this method invokes the
   *  <code>processAnimalEvent()</code> method.
   *
   *  @see #processAnimalEvent(AnimalEvent)
   *  @param event the event object.
   */
    protected void processEvent(EventObject event) {
        if (event instanceof AnimalEvent) processAnimalEvent((AnimalEvent) event);
    }

    /**
   *  Processes animal events occurring on this animal by dispatching them
   *  to a registered animal listener.
   *
   *  @see #processEvent(EventObject)
   *  @param event the animal event.
   */
    protected void processAnimalEvent(AnimalEvent event) {
        for (Iterator e = animal_listeners.iterator(); e.hasNext(); ) {
            AnimalListener l = (AnimalListener) e.next();
            l.animalRemoved(event);
        }
    }

    /**
   *  Checks the connection state of IButtonAnimalProxy.
   *
   *  @return <code>true</code> if the iButton has been removed from the reader, otherwise <code>false</code>
   */
    public boolean isRemoved() {
        return removed;
    }

    /**
   *   If the attached iButton contains the applet named "Animal", select it
   *   and return <code>true</code>. Otherwise return <code>false</code>.
   *
   *   @see #selectApplet(String)
   *   @return the select status
   */
    public boolean selectApplet() {
        return selectApplet("Animal");
    }

    /**
   *   If the attached iButton contains the named applet, select it
   *   and return true. Otherwise return false.
   *
   *   @see #selectApplet(SlotChannel, String)
   *   @param appletName the name of the applet to select
   *   @return the select status
   */
    public boolean selectApplet(String appletName) {
        return selectApplet(channel, appletName);
    }

    /**
   *   If the attached iButton contains the applet named "Animal", select it
   *   and return true. Otherwise return false.
   *
   *   @see #selectApplet(SlotChannel, String)
   *   @param channel the slot channel
   *   @return the select status
   */
    public static boolean selectApplet(SlotChannel channel) {
        return selectApplet(channel, "Animal");
    }

    /**
   *   Selects a Java Card applet. If the attached iButton contains
   *   the named applet, select it and return true. Otherwise return false.
   *
   *   @see AnimalApplet#select(javacard.framework.APDU)
   *   @param channel the slot channel
   *   @param appletName the name of the applet to select
   *   @return the select status
   */
    public static synchronized boolean selectApplet(SlotChannel channel, String appletName) {
        char[] appletNameChars = appletName.toCharArray();
        byte[] buffer = new byte[appletName.length()];
        for (int i = 0; i < buffer.length; i++) buffer[i] = (byte) appletNameChars[i];
        com.ibutton.oc.CommandAPDU selectAPDU = new com.ibutton.oc.CommandAPDU((byte) SELECT_CLA, (byte) SELECT_INS, (byte) SELECT_P1, (byte) SELECT_P1, buffer, (byte) 0);
        try {
            ResponseAPDU response = channel.sendAPDU(selectAPDU);
            return (response.sw() == SW_NO_ERROR);
        } catch (CardTerminalException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
   *   Sends a who-are-you message to the animal and reads the response.
   *   The who-are-you information returned from the animal consists of a
   *   buffer containing basic information about the animal. This method
   *   parses the buffer returned and stores the information in private
   *   fields. It is then possible to use accessor methods to retrieve the
   *   values. (The rationale for transfering a block of information rather
   *   than requesting individual properties is performance. It is faster to
   *   get a larger buffer in a single transaction than performing several
   *   transactions.)
   *
   *   <p>
   *   Normal programs should not call this method. Use the corresponding
   *   <em>get</em> methods instead (such as <code>getGender(), getLegs(),
   *   getSpecies(), getCreator(), getCry(), getPicture(),</code>
   *   and <code>getSound()</code>).
   *
   *   @see #getGender()
   *   @see #getLegs()
   *   @see #getSpecies()
   *   @see #getCreator()
   *   @see #getCry()
   *   @see #getPicture()
   *   @see #getSound()
   *   @see AnimalCommunication#getWhoAreYou()
   *   @see Animal#getWhoAreYou()
   *   @see AnimalConstants#ANIMAL_WHO_ARE_YOU
   */
    protected synchronized void whoAreYou() {
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_WHO_ARE_YOU);
            if (response.sw() == SW_NO_ERROR) {
                byte[] ba = response.data();
                gender = ba[0];
                legs = ba[1];
                StringTokenizer st = new StringTokenizer(new String(ba, 2, ba.length - 2), "\0");
                if (st.hasMoreTokens()) species = st.nextToken();
                if (st.hasMoreTokens()) creator = st.nextToken();
                if (st.hasMoreTokens()) cry = st.nextToken();
                if (st.hasMoreTokens()) picture = st.nextToken();
                if (st.hasMoreTokens()) sound = st.nextToken();
            } else {
                System.err.println("Cannot read who-are-you message from the animal");
            }
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
    }

    /**
   *   Sends a where-you-are message to the animal to inform it
   *   about its present location and environment. Sending this
   *   message to the animal is part of the intitial handshaking
   *   procedure when an animal enters a location. The animal can then
   *   use this where-you-are information to behave  differently in
   *   different environments.
   *
   *   @see AnimalConstants#LIGHT_OFF
   *   @see AnimalConstants#LIGHT_DIM
   *   @see AnimalConstants#LIGHT_ON
   *   @see LocationPanel#addAnimalHandshake(AnimalProxy)
   *   @see StandardLocationPanel#addAnimalHandshake(AnimalProxy)
   *   @see #getLastLocation()
   *   @see AnimalCommunication#setWhereYouAre(byte, byte, byte, byte[])
   *   @see Animal#setWhereYouAre(byte, byte, byte, byte[])
   *   @see AnimalConstants#ANIMAL_WHERE_YOU_ARE
   *
   *   @param insideTemp the location indoor temperature in Celcius
   *   @param outsideTemp the location outdoor temperature in Celcius
   *   @param light the light code (LIGHT_OFF, LIGHT_DIM, LIGHT_ON)
   *   @param location the location name
   */
    public synchronized void whereYouAre(int insideTemp, int outsideTemp, byte light, String location) {
        try {
            char[] locationChars = location.toCharArray();
            byte[] buffer = new byte[location.length() + LOCATION_OFFSET];
            buffer[INDOOR_TEMPERATURE_OFFSET] = (byte) insideTemp;
            buffer[OUTDOOR_TEMPERATURE_OFFSET] = (byte) outsideTemp;
            buffer[LIGHT_OFFSET] = light;
            for (int i = 0; i < locationChars.length; i++) buffer[i + LOCATION_OFFSET] = (byte) locationChars[i];
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_WHERE_YOU_ARE, buffer);
            if (response.sw() != SW_NO_ERROR) System.err.println("Cannot write where-you-are information to the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to send where-you-are infomation to the animal (card)");
        }
    }

    /**
   *   Sends a feed message to the animal and gets the response from it.
   *   Feeding in this context means that the animal is <em>offered</em>
   *   some food (or beverage). The animal can then respond in different
   *   ways, such as eating or rejecting the food. Thus it is possible
   *   to develop animals with different taste, as well as animals that
   *   develop a taste for certain food over time.
   *
   *   <p>
   *   Note that this method performs both feeding and watering. Food
   *   and beverage is treated the same way.
   *
   *   @see AnimalConstants#BEVERAGE_COCA_COLA
   *   @see AnimalConstants#BEVERAGE_COFFEE
   *   @see AnimalConstants#BEVERAGE_MILK
   *   @see AnimalConstants#BEVERAGE_TEA
   *   @see AnimalConstants#BEVERAGE_UNKNOWN
   *   @see AnimalConstants#BEVERAGE_WATER
   *   @see AnimalConstants#FOOD_GRASS
   *   @see AnimalConstants#FOOD_SNACK
   *   @see AnimalConstants#FOOD_HEY
   *   @see AnimalConstants#FOOD_UNKNOWN
   *   @see AnimalConstants#RESPONSE_ATE
   *   @see AnimalConstants#RESPONSE_DELICIOUS
   *   @see AnimalConstants#RESPONSE_DRANK
   *   @see AnimalConstants#RESPONSE_NONE
   *   @see AnimalConstants#RESPONSE_NOT_HUNGRY
   *   @see AnimalConstants#RESPONSE_REJECT
   *   @see AnimalConstants#RESPONSE_VOMIT
   *   @see AnimalCommunication#feed(byte[], short)
   *   @see Animal#feed(byte[], short)
   *
   *   @param food the food or beverage code (e.g., FOOD_GRASS, and BEVERAGE_WATER)
   *   @return the feed response code (e.g., RESPONSE_ATE and RESPONSE_REJECT)
   */
    public byte feed(byte food) {
        byte[] ba = new byte[1];
        ba[0] = food;
        return feed(ba);
    }

    /**
   *   Sends a feed message to the animal and gets the response from it.
   *   Feeding in this context means that the animal is <em>offered</em>
   *   some food (or beverage). The animal can then respond in different
   *   ways, such as eating or rejecting the food. Thus it is possible
   *   to develop animals with different taste, as well as animals that
   *   develop a taste for certain food over time.
   *
   *   <p>
   *   Note that this method performs both feeding and watering. Food
   *   and beverage is treated the same way.
   *
   *   @see AnimalConstants#BEVERAGE_COCA_COLA
   *   @see AnimalConstants#BEVERAGE_COFFEE
   *   @see AnimalConstants#BEVERAGE_MILK
   *   @see AnimalConstants#BEVERAGE_TEA
   *   @see AnimalConstants#BEVERAGE_UNKNOWN
   *   @see AnimalConstants#BEVERAGE_WATER
   *   @see AnimalConstants#FOOD_GRASS
   *   @see AnimalConstants#FOOD_SNACK
   *   @see AnimalConstants#FOOD_HEY
   *   @see AnimalConstants#FOOD_UNKNOWN
   *   @see AnimalConstants#RESPONSE_ATE
   *   @see AnimalConstants#RESPONSE_DELICIOUS
   *   @see AnimalConstants#RESPONSE_DRANK
   *   @see AnimalConstants#RESPONSE_NONE
   *   @see AnimalConstants#RESPONSE_NOT_HUNGRY
   *   @see AnimalConstants#RESPONSE_REJECT
   *   @see AnimalConstants#RESPONSE_VOMIT
   *   @see AnimalCommunication#feed(byte[], short)
   *   @see Animal#feed(byte[], short)
   *   @see AnimalConstants#ANIMAL_FEED
   *
   *   @param food an array of the food/beverage codes
   *   @return the feed/water response code
   */
    public synchronized byte feed(byte[] food) {
        byte feed_response = RESPONSE_NONE;
        byte food_response;
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_FEED, food);
            if (response.sw() == SW_NO_ERROR) {
                byte[] ba = response.data();
                feed_response = ba[0];
                food_response = ba[1];
            } else {
                System.err.println("Cannot read feed-response message from the animal");
            }
        } catch (CardTerminalException e) {
            System.err.println("Unable to send feed infomation to the animal (card)");
        }
        return feed_response;
    }

    /**
   *   Determines the animal gender.
   *   @return <code>true</code> if the animal is male, otherwise <code>false</code>
   */
    public boolean isMale() {
        return getGender() == ANIMAL_GENDER_MALE;
    }

    /**
   *   Determines the animal gender.
   *   @return <code>true</code> if the animal is female, otherwise <code>false</code>
   */
    public boolean isFemale() {
        return !isMale();
    }

    /**
   *   Gets the animal gender. The gender is returned as a code.
   *
   *   @see #isMale()
   *   @see #isFemale()
   *   @see #getGenderString(byte)
   *   @see AnimalConstants#ANIMAL_GENDER_MALE
   *   @see AnimalConstants#ANIMAL_GENDER_FEMALE
   *
   *   @return The gender code (ANIMAL_GENDER_MALE or ANIMAL_GENDER_FEMALE)
   */
    public byte getGender() {
        if (gender == Byte.MIN_VALUE) whoAreYou();
        return gender;
    }

    /**
   *   Gets the number of the legs for the animal.
   *   @return The number of legs the animal has
   */
    public byte getLegs() {
        if (legs == Byte.MIN_VALUE) whoAreYou();
        return legs;
    }

    /**
   *   Gets the animal species.
   *   @return The species of the animal
   */
    public String getSpecies() {
        if (species == null) whoAreYou();
        return species;
    }

    /**
   *   Gets the creator of the applet (name or company). Typically,
   *   this would be <em>your</em> name.
   *   @return The creator of the animal
   */
    public String getCreator() {
        if (creator == null) whoAreYou();
        return creator;
    }

    /**
   *   Gets the cry of the animal as a text. The cry is a textual
   *   representation of the sound attributed the animal
   *   (e.g., <em>moo</em> for cows and <em>woff</em> for dogs).
   *
   *   @return The cry of the animal
   */
    public String getCry() {
        if (cry == null) whoAreYou();
        return cry;
    }

    /**
   *   Gets the picture for the animal. This method returns a
   *   <em>reference</em> (filename/URL) to the picture rather than the actual
   *   picture (due to Java Card performance and memory constraints).
   *
   *   <p>
   *   Note that the picture should be encoded in a format that Java
   *   supports, such as gif.
   *
   *   @return The picture filename/URL as a string
   */
    public String getPicture() {
        if (picture == null) whoAreYou();
        return picture;
    }

    /**
   *   Gets the animal's sound. This method returns a
   *   <em>reference</em> (filename/URL) to the recorded sound than the actual
   *   sound (due to Java Card performance and memory constraints).
   *
   *   <p>
   *   Note that the animal sould is different from the cry, which is a
   *   text rather than a reference to an encoded sound.
   *
   *   @return The sound filename/URL as a string
   */
    public String getSound() {
        if (sound == null) whoAreYou();
        return sound;
    }

    /**
   *   Get a message from the animal. This method sends a
   *   message request to the animal applet. The animal can then respond with
   *   different salutes, such as hello, hi, greetings, and goodmorning. Some
   *   animals may just respond with the same string every time, whereas
   *   other animals may respond differently every time. The respons can be
   *   context dependent in some way (e.g., depending on the time of day,
   *   mode, environment, and past experiences).
   *
   *   @see AnimalCommunication#getMessage()
   *   @see Animal#getMessage()
   *   @see AnimalConstants#ANIMAL_MESSAGE
   *
   *   @return The respose from the animal (salute)
   */
    public synchronized String getMessage() {
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_MESSAGE);
            if (response.sw() == SW_NO_ERROR) return new String(response.data()); else System.err.println("Cannot read the message from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return "";
    }

    private String last_location_cache = null;

    /**
   *   Asks the animal applet about its last location. The animal will
   *   than respond with the last location it visited. (Normally, the
   *   animal gets the current location name from the where-you-are
   *   message, and stores it.)
   *
   *   @see #whereYouAre(int,int,byte,String)
   *   @see AnimalCommunication#getLastLocation()
   *   @see Animal#getLastLocation()
   *   @see AnimalConstants#ANIMAL_LAST_LOCATION
   *   @return The the animal's last location
   */
    public synchronized String getLastLocation() {
        if (last_location_cache != null) return last_location_cache;
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_LAST_LOCATION);
            if (response.sw() == SW_NO_ERROR) {
                if (response.data() != null) {
                    String result = new String(response.data());
                    last_location_cache = result;
                    return result;
                } else return "";
            } else System.err.println("Cannot read last-location message from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return "";
    }

    /**
   *  Gets the mode of the animal. The mode describes the physical
   *  and/or emotional state of the animal.
   *
   *  @see AnimalConstants#MODE_HAPPY
   *  @see AnimalConstants#MODE_NORMAL
   *  @see AnimalConstants#MODE_SAD
   *  @see AnimalConstants#MODE_SLEEP
   *  @see AnimalCommunication#getMode()
   *  @see Animal#getMode()
   *  @see AnimalConstants#ANIMAL_MODE
   *
   *  @return The the animal's mode (e.g., MODE_HAPPY and MODE_SAD)
   */
    public synchronized byte getMode() {
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_MODE);
            if (response.sw() == SW_NO_ERROR) {
                byte[] ba = response.data();
                return ba[0];
            } else System.err.println("Cannot read mode from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return MODE_NORMAL;
    }

    private List op_names = null;

    private List op_ids = null;

    /**
   *   Gets the operations you can perform on the animal. This method
   *   asks the animal for the operations and returns the names of the
   *   operation as a vector of stings.
   *
   *   <p>
   *   Specifically, the animal can return a list of operation name
   *   and identifier pairs, which describes the set of
   *   animal-specific commands you can issue. This method only
   *   returns a vector containing the operation names as strings. Use
   *   the method <code>getOperationIds()</code> to retrieve a vector
   *   of the corresponding operations id numbers. Locations can use
   *   this vector to create buttons (or other user-interface
   *   components) on some kind of animal-control panel for performing
   *   operations on the animal.
   *
   *   <p>
   *   Examples of operations that an animal can support are exercise,
   *   groom, pat, play, punish, ride, and scratch. The animal can
   *   then change its state, such as its mode, when the user performs
   *   these operations.
   *
   *   @see #getOperationIds()
   *   @see AnimalCommunication#getOperations()
   *   @see Animal#getOperations()
   *   @see #performOperation(byte)
   *   @see AnimalConstants#ANIMAL_OPERATIONS
   *
   *   @return the operation names as a vector of strings
   */
    public synchronized List getOperations() {
        if (op_names != null) return op_names;
        op_names = new ArrayList();
        op_ids = new ArrayList();
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_OPERATIONS);
            if (response.sw() == SW_NO_ERROR) {
                byte[] ba = response.data();
                int i = 0;
                while (i < ba.length) {
                    StringBuffer s = new StringBuffer();
                    while (i < ba.length && ba[i] != 0) {
                        s.append((char) ba[i++]);
                    }
                    op_names.add(s.toString());
                    if (i++ < ba.length) op_ids.add(new Byte(ba[i++]));
                }
            } else {
                System.err.println("Cannot read the operations from the animal");
            }
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return op_names;
    }

    /**
   *   Gets the operation id numbers for the animal-specific operations.
   *   This methods returns a vector of numbers that identify the
   *   operations that the animal supports. The numeric id is useful
   *   for envoking the operation with the
   *   <code>performOperation()</code> method.
   *   The elements of the id vector corresponds to the vector of
   *   operation names return by the <code>getOperations()</code> method.
   *
   *   <p>
   *   Normally, this method is called after <code>getOperations()</code>.
   *
   *   @see #getOperations()
   *   @see AnimalCommunication#getOperations()
   *   @see Animal#getOperations()
   *   @see #performOperation(byte)
   *
   * @return the operation identifiers as a vector of identifier codes
   */
    public List getOperationIds() {
        if (op_ids == null) getOperations();
        return op_ids;
    }

    /**
   *   Performs an operation on an animal. This method sends the
   *   operation id number to the animal. The animal is supposed to
   *   perform the operation requested, and perhaps change its state
   *   (e.g., its mode).  It is possible to obtain the valid operation
   *   names by calling <code>getOperations()</code>, and the valid
   *   operation id numbers by calling <code>getOperationIds()</code>.
   *
   *   @see #getOperations()
   *   @see #getOperationIds()
   *   @see AnimalCommunication#performOperation(byte)
   *   @see Animal#performOperation(byte)
   *   @see AnimalConstants#ANIMAL_OP
   *
   *   @param operation_id the operation to perform (the identifier code)
   */
    public synchronized void performOperation(byte operation_id) {
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_OP, operation_id);
            if (response.sw() == SW_NO_ERROR) {
                return;
            } else {
                System.err.println("Cannot perform animal-specific operation");
            }
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
    }

    /**
   *   Gets the id for the Animal applet's ROM (iButton).
   *   Although this method returns the iButton's ROM number, it does
   *   not perform any communication with the iButton. For performance
   *   reasons, the ROM id is read when the iButton is inserted and
   *   then stored in the <code>IButtonAnimalProxy</code> object.
   *
   *   @see #setId(int[])
   *
   *   @return the ROM id as an array of numbers
   */
    public int[] getId() {
        return rom_id;
    }

    /**
   *   Sets the id for the Animal applet's ROM (iButton). Normally,
   *   programs should not call this method unless they set up an
   *   <code>IButtonAnimalProxy</code>.
   *
   *   @see #getId()
   *
   *   @param id the ROM id as an array of numbers
   */
    protected void setId(int[] id) {
        rom_id = id;
    }

    private String animal_name_cache = null;

    /**
   *   Gets the animal name. The animal name is the individual
   *   animal's name. Typically, the name is set at animal
   *   configuration time.
   *
   *   @see AnimalCommunication#getAnimalName()
   *   @see Animal#getAnimalName()
   *   @see #setAnimalName(String)
   *   @see AnimalConstants#ANIMAL_NAME
   *   @return The animal name
   */
    public synchronized String getAnimalName() {
        if (animal_name_cache != null) return animal_name_cache;
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_NAME);
            if (response.sw() == SW_NO_ERROR) {
                if (response.data() != null) {
                    animal_name_cache = new String(response.data());
                    return new String(response.data());
                } else {
                    animal_name_cache = "";
                    return "";
                }
            } else System.err.println("Cannot read animal-name message from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return null;
    }

    /**
   *   Sets the animal name. Each animal can have an individual name.
   *
   *   <p>
   *   Typically, this method is called at animal-configuration
   *   time to set the value. That is, when the animal is entered at a
   *   location for the very first time.
   *
   *   @see AnimalCommunication#setAnimalName(byte[])
   *   @see Animal#setAnimalName(byte[])
   *   @see #getAnimalName()
   *   @see AnimalConstants#ANIMAL_SET_NAME
   *   @param str the new animal name
   */
    public synchronized void setAnimalName(String str) {
        animal_name_cache = null;
        byte[] buffer = new byte[str.length()];
        for (int i = 0; i < buffer.length; i++) buffer[i] = (byte) str.charAt(i);
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_SET_NAME, buffer);
            if (response.sw() == SW_NO_ERROR) animal_name_cache = str;
        } catch (CardTerminalException e) {
            System.err.println("Unable to send animal-name infomation to the animal (card)");
        }
    }

    private String owner_name_cache = null;

    /**
   *   Gets the animal owner name. The owner name is the individual
   *   animal's owner. Typically, the name is set at animal
   *   configuration time.
   *
   *   @see AnimalCommunication#getOwnerName()
   *   @see Animal#getOwnerName()
   *   @see #setOwnerName(String)
   *   @see AnimalConstants#ANIMAL_OWNER_NAME
   *   @return The owner name
   */
    public synchronized String getOwnerName() {
        if (owner_name_cache != null) return owner_name_cache;
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_OWNER_NAME);
            if (response.sw() == SW_NO_ERROR) {
                if (response.data() != null) {
                    owner_name_cache = new String(response.data());
                    return new String(response.data());
                } else {
                    owner_name_cache = "";
                    return "";
                }
            } else System.err.println("Cannot read owner-name message from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return "";
    }

    /**
   *   Sets the animal owner name. Each animal can have an owner.
   *
   *   <p>
   *   Typically, this method is called at animal-configuration
   *   time to set the value. That is, when the animal is entered at a
   *   location for the very first time.
   *
   *   @see AnimalCommunication#setOwnerName(byte[])
   *   @see Animal#setOwnerName(byte[])
   *   @see #getOwnerName()
   *   @see AnimalConstants#ANIMAL_OWNER_SET_NAME
   *   @param str the new owner name
   */
    public synchronized void setOwnerName(String str) {
        owner_name_cache = null;
        byte[] buffer = new byte[str.length()];
        for (int i = 0; i < buffer.length; i++) buffer[i] = (byte) str.charAt(i);
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_OWNER_SET_NAME, buffer);
            if (response.sw() == SW_NO_ERROR) owner_name_cache = str;
        } catch (CardTerminalException e) {
            System.err.println("Unable to send owner-name infomation to the animal (card)");
        }
    }

    private String owner_email_cache = null;

    /**
   *   Gets the animal owner email. The owner e-mail is the Internet
   *   e-mail address of the animal's owner. Typically, the e-mail
   *   address is set at animal configuration time.
   *
   *   @see AnimalCommunication#getOwnerEmail()
   *   @see Animal#getOwnerEmail()
   *   @see #setOwnerEmail(String)
   *   @see AnimalConstants#ANIMAL_OWNER_EMAIL
   *   @return The owner email
   */
    public synchronized String getOwnerEmail() {
        if (owner_email_cache != null) return owner_email_cache;
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_OWNER_EMAIL);
            if (response.sw() == SW_NO_ERROR) {
                if (response.data() != null) {
                    owner_email_cache = new String(response.data());
                    return new String(response.data());
                } else {
                    owner_email_cache = "";
                    return "";
                }
            } else System.err.println("Cannot read owner-email message from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return "";
    }

    /**
   *   Sets the animal owner e-mail. Each animal owner can have an
   *   e-mail address.
   *
   *   <p>
   *   Typically, this method is called at animal-configuration
   *   time to set the value. That is, when the animal is entered at a
   *   location for the very first time.
   *
   *   @see AnimalCommunication#setOwnerEmail(byte[])
   *   @see Animal#setOwnerEmail(byte[])
   *   @see #getOwnerEmail()
   *   @see AnimalConstants#ANIMAL_OWNER_SET_EMAIL
   *   @param str the new owner e-mail address
   */
    public synchronized void setOwnerEmail(String str) {
        owner_email_cache = null;
        byte[] buffer = new byte[str.length()];
        for (int i = 0; i < buffer.length; i++) buffer[i] = (byte) str.charAt(i);
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_OWNER_SET_EMAIL, buffer);
            if (response.sw() == SW_NO_ERROR) owner_email_cache = str;
        } catch (CardTerminalException e) {
            System.err.println("Unable to send owner-email infomation to the animal (card)");
        }
    }

    /**
   *   Gets an integer from a little endian byte array.
   *   @param ba the byte array
   *   @return the resulting integer
   */
    private int getInt(byte[] ba) {
        return (((((int) ba[3]) << 24) & 0xFF000000) | ((((int) ba[2]) << 16) & 0x00FF0000) | ((((int) ba[1]) << 8) & 0x0000FF00) | (((int) ba[0]) & 0x000000FF));
    }

    /**
   *   Gets the internal iButton time.
   *   @see AnimalConstants#ANIMAL_CLOCK
   *   @return The iButton time
   */
    public synchronized int getClock() {
        try {
            ResponseAPDU response = sendCommandAPDU(ANIMAL_CLA, ANIMAL_CLOCK);
            if (response.sw() == SW_NO_ERROR) {
                if (response.data() != null) return getInt(response.data()); else return 0;
            } else System.err.println("Cannot read clock message from the animal");
        } catch (CardTerminalException e) {
            System.err.println("Unable to communicate with the animal (card)");
        }
        return 0;
    }

    /**
   *  Creates a command APDU and sends it.
   *  This method is a utility method for other methods in the
   *  <code>IButtonAnimalProxy</code> class.
   *  Normal programs should not have to call this method.
   *
   *  @see AnimalConstants#ANIMAL_CLA
   *
   *  @param CLA the operation class
   *  @param INS the instruction code
   *  @return The response APDU from the card
   */
    protected ResponseAPDU sendCommandAPDU(byte CLA, byte INS) throws CardTerminalException {
        com.ibutton.oc.CommandAPDU animalAPDU = new com.ibutton.oc.CommandAPDU(CLA, INS, (byte) 0, (byte) 0);
        return channel.sendAPDU(animalAPDU);
    }

    /**
   *  Creates a command APDU and send it.
   *  This method is a utility method for other methods in the
   *  <code>IButtonAnimalProxy</code> class.
   *  Normal programs should not have to call this method.
   *
   *  @see AnimalConstants#ANIMAL_CLA
   *
   *  @param CLA the operation class
   *  @param INS the instruction code
   *  @param buffer the byte buffer to send
   *  @return The response APDU from the card
   */
    protected ResponseAPDU sendCommandAPDU(byte CLA, byte INS, byte[] buffer) throws CardTerminalException {
        com.ibutton.oc.CommandAPDU animalAPDU = new com.ibutton.oc.CommandAPDU(CLA, INS, (byte) 0, (byte) 0, buffer);
        return channel.sendAPDU(animalAPDU);
    }
}
