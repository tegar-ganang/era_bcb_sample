package org.gotext;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

/**
 * Manage services for goText .
 * 
 * <p>This is the most important class for the program.<br>
 * To send a SMS, a service must be installed and configured properly. In this
 * class there are all service fields and settings.<br>
 * @author Michele 'Miccar' Cardinale >> miccar AT gmail DOT com
 * @author <br>Natale 'Blues-Man'  Vinto >> ebballon AT interfree DOT it
 * @version 2.0
 */
public class Service {

    /**
     * The MAX number of services that can be installed, by default
     */
    public static final int MAX_MEM_SERVICES = 20;

    /**
     * Index constant for service Name
     */
    public static final int NAME = 0;

    /**
     * Index constant for service Address
     */
    public static final int ADDRESS = 1;

    /**
     * Index constant for service Need User value
     */
    public static final int NEED_USER = 2;

    /**
     * Index constant for service Username
     */
    public static final int USER = 3;

    /**
     * Index constant for service Need Pass value
     */
    public static final int NEED_PASS = 4;

    /**
     * Index constant for service Password
     */
    public static final int PASS = 5;

    /**
     * Index constant for service Need Nick value
     */
    public static final int NEED_NICK = 6;

    /**
     * Index constant for service Nick
     */
    public static final int NICK = 7;

    /**
     * Index constant for service Maximum Number of allowed Recipients
     */
    public static final int MAX_RECIPIENTS = 8;

    /**
     * Index constant for service Max supported Characters in a message
     */
    public static final int MAX_CHARS = 9;

    /**
     * Index constant for service Max Daily Messages
     */
    public static final int MAX_MSG = 10;

    /**
     * Index constant for service Daily Sent Messages
     */
    public static final int SENT_MSG = 11;

    /**
     * Index constant for service Is Numeric info on Recipients
     */
    public static final int IS_NUMERIC_RECIPIENT = 12;

    /**
     * Index constant for service Signature for outgoing messages
     */
    public static final int SIGN = 13;

    /**
     * Index constant for service Unavailable (Excluded) Characters
     */
    public static final int XCLUDED_CHAR = 14;

    /**
     * Index constant for service Substituive Characters
     */
    public static final int SUBS_CHAR = 15;

    /**
     * Index constant for service characters that Count More
     */
    public static final int COUNT_MORE = 16;

    /**
     * Index constant for service Count For
     */
    public static final int COUNT_FOR = 17;

    /**
     * Index constant for service Default settings
     */
    public static final int IS_DEFAULT = 18;

    /**
     * Index constant for service sessione Cookie
     */
    public static final int COOKIE = 19;

    /**
     * Index constant for service Max Monthly Messages
     */
    public static final int MAX_MONTHLY_MSG = 20;

    /**
     * Index constant for service Monthly Sent Messages
     */
    public static final int SENT_MONTHLY_MSG = 21;

    /**
     * Its value is always one more than last service config constant value (currently SENT_MONTHLY_MSG)
     */
    public static final int INDEX_MAX = SENT_MONTHLY_MSG + 1;

    /**
     * Used to separate elements in a service string
     */
    static final String token = ";";

    /**
     * Record store name for services
     */
    static final String RN = "_services";

    /**
     * This is current service string exploded
     */
    private String[] serviceStringTokenized;

    /**
     * This service's ID on the RMS db (every service is saved on RMS!)
     */
    private int ownRmsId;

    /**
     * Creates a new instance of Service
     * @param stringa The full, token-separated, service string
     * @param ownRmsId Id of this service on the RMS. Every service is saved to RMS and has its own id!
     */
    public Service(String stringa, int ownRmsId) {
        serviceStringTokenized = Utils.splitString(stringa, token);
        this.ownRmsId = ownRmsId;
    }

    /**
     * Get the service string
     * @return the service string
     */
    public String getString() {
        String serviceString = "";
        for (int pos = 0; pos < serviceStringTokenized.length - 1; pos++) {
            serviceString += serviceStringTokenized[pos] + token;
        }
        serviceString += serviceStringTokenized[serviceStringTokenized.length - 1];
        return serviceString;
    }

    /**
     * Tells how many characters are available minus the Signature
     * @return Available characters
     */
    public int getMaxCharsWithSign() {
        return getIntConfig(Service.MAX_CHARS) - (getStringConfig(SIGN).length() > 0 ? getStringConfig(SIGN).length() + 1 : 0);
    }

    /**
     * Tells how many characters messages are left, in string form.
     * 
     * It shows daily remaining, monthly remaining, or both, depending on service
     *  count mode.
     * 
     * @return Available characters
     */
    public String getRemainingMsgString() {
        String dailyCount = "", monthlyCount = "", combinedCount = "";
        if (getIntConfig(Service.MAX_MSG) > 0) {
            dailyCount = (getIntConfig(Service.MAX_MSG) - getIntConfig(Service.SENT_MSG)) + "";
            combinedCount += dailyCount;
        }
        if (getIntConfig(Service.MAX_MONTHLY_MSG) > 0) {
            monthlyCount = (getIntConfig(Service.MAX_MONTHLY_MSG) - getIntConfig(Service.SENT_MONTHLY_MSG)) + "";
            if (combinedCount.length() > 0) {
                combinedCount += "/";
            }
            combinedCount += monthlyCount + Lang.LMS_MONTH_FIRST_LETTER;
        }
        return combinedCount;
    }

    /**
     * Retrieves a configuration element of this service as Integer
     * @param fieldIndex One of the allowed int costants representing a config field
     * @return The requested configuration element, if it is an integer value
     */
    public int getIntConfig(int fieldIndex) {
        try {
            return Integer.parseInt(serviceStringTokenized[fieldIndex]);
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    /**
     * Retrieves a configuration element of this service as String
     * @param fieldIndex One of the allowed int costants representing a config field
     * @return The requested configuration element, if it is a String value
     */
    public String getStringConfig(int fieldIndex) {
        try {
            String cfg = serviceStringTokenized[fieldIndex];
            if (cfg == null) {
                return "";
            } else {
                return cfg;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Set service fields
     * @return modService result call
     * @param needuser <B>1</B> if username is <B>required</B> for this service, <B>0</B> if it's not needed, <B>2</B> if it's <B>optional</B>
     * @param maxmonthlymsg Max number of messages a user can send in a month.
     * <br>If <B>0</B> counting of monthly limit is <B>disabled</B>.
     * @param sentmonthlymsg Number of messages sent in a month
     * @param name Service Name
     * @param address the service installation URL
     * @param user Username for this service's website, if required or optional
     * @param needpass <B>1</B> if password is <B>required</B> for this service, <B>0</B> if it's not needed, <B>2</B> if it's <B>optional</B>
     * @param pass Password for this service's website, if required or optional
     * @param neednick <B>1</B> if nick is <B>required</B> for this service, <B>0</B> if it's not needed, <B>2</B> if it's <B>optional</B>
     * @param nick Nickname for this service's website, if required or optional
     * @param maxrecipients Max number of recipients for a message sent with this service
     * @param maxchars max chars that can be used for this service to write a {@link org.gotext.Message Message}
     * @param maxmsg Max number of messages a user can send daily.
     * <br>If <B>0</B> counting of daily limit is <B>disabled</B>.
     * @param sentmsg The number of SMS sent in a day
     * @param isnumericrecipient if there is a numeric recipients
     * @param sign Default signature to put at message end
     * @param xcludedchar Lists characters of not available characters for this service
     * <br>It is a <I>pipe</I>-separated string of characters (eg: "à|è|ì|")
     * @param subschar List of characters to replace to the not available ones.
     * <br>It is a <I>pipe</I>-separated string of strings (eg: "a'|e'|i'|euro")
     * @param countmore Lists characters that counts more than 1 for this service.
     * <br>It is a <I>pipe</I>-separated string of characters (eg: "à|?|\\n")
     * @param countfor Lists how much each char of the <I>countmore</I> list counts.
     * <br>It is a <I>pipe</I>-separated string of characters (eg: "3|2|2")
     * @param isdefault 1 if if the service is used as default one, 0 otherwise
     * @param cookie last session cookie (if sent back in last service reply)
     * @see #modService(String,int)
     */
    public int set(String name, String address, int needuser, String user, int needpass, String pass, int neednick, String nick, int maxrecipients, int maxchars, int maxmsg, int sentmsg, int isnumericrecipient, String sign, String xcludedchar, String subschar, String countmore, String countfor, int isdefault, String cookie, int maxmonthlymsg, int sentmonthlymsg) {
        if (isnumericrecipient == 2) {
            maxrecipients = 2;
        }
        String newstr = name;
        newstr += token + address;
        newstr += token + needuser;
        newstr += token + user;
        newstr += token + needpass;
        newstr += token + pass;
        newstr += token + neednick;
        newstr += token + nick;
        newstr += token + maxrecipients;
        newstr += token + maxchars;
        newstr += token + maxmsg;
        newstr += token + sentmsg;
        newstr += token + isnumericrecipient;
        newstr += token + sign;
        newstr += token + xcludedchar;
        newstr += token + subschar;
        newstr += token + countmore;
        newstr += token + countfor;
        newstr += token + isdefault;
        newstr += token + cookie;
        newstr += token + maxmonthlymsg;
        newstr += token + sentmonthlymsg;
        return Service.modService(newstr, getRmsId());
    }

    /**
     * Synchronize (set) the number of SMS still available today
     * @param rem Number of SMS remaining today
     * @return -1 if there was an error<br>else a value >= 0
     */
    public int synchRemMsg(int rem) {
        int sent = getIntConfig(MAX_MSG) - rem;
        int result = this.set(getStringConfig(NAME), getStringConfig(ADDRESS), getIntConfig(NEED_USER), getStringConfig(USER), getIntConfig(NEED_PASS), getStringConfig(PASS), getIntConfig(NEED_NICK), getStringConfig(NICK), getIntConfig(MAX_RECIPIENTS), getIntConfig(MAX_CHARS), getIntConfig(MAX_MSG), sent, getIntConfig(IS_NUMERIC_RECIPIENT), getStringConfig(SIGN), getStringConfig(XCLUDED_CHAR), getStringConfig(SUBS_CHAR), getStringConfig(COUNT_MORE), getStringConfig(COUNT_FOR), getIntConfig(IS_DEFAULT), getStringConfig(COOKIE), getIntConfig(MAX_MONTHLY_MSG), getIntConfig(SENT_MONTHLY_MSG));
        if (result > 0) {
            serviceStringTokenized[SENT_MSG] = String.valueOf(sent);
            return sent;
        } else {
            return -1;
        }
    }

    /**
     * Synchronize (set) the number of SMS still available the current month
     * @param rem Number of SMS remaining this month
     * @return -1 if there was an error<br>else a value >= 0
     */
    public int synchRemMonthlyMsg(int rem) {
        int sent = getIntConfig(MAX_MONTHLY_MSG) - rem;
        int result = this.set(getStringConfig(NAME), getStringConfig(ADDRESS), getIntConfig(NEED_USER), getStringConfig(USER), getIntConfig(NEED_PASS), getStringConfig(PASS), getIntConfig(NEED_NICK), getStringConfig(NICK), getIntConfig(MAX_RECIPIENTS), getIntConfig(MAX_CHARS), getIntConfig(MAX_MSG), getIntConfig(SENT_MSG), getIntConfig(IS_NUMERIC_RECIPIENT), getStringConfig(SIGN), getStringConfig(XCLUDED_CHAR), getStringConfig(SUBS_CHAR), getStringConfig(COUNT_MORE), getStringConfig(COUNT_FOR), getIntConfig(IS_DEFAULT), getStringConfig(COOKIE), getIntConfig(MAX_MONTHLY_MSG), sent);
        if (result > 0) {
            serviceStringTokenized[SENT_MONTHLY_MSG] = String.valueOf(sent);
            return sent;
        } else {
            return -1;
        }
    }

    /**
     * Increase the number of SMS sent with this service
     * @param cookie The updated cookie string
     * @param incr Number of SMS sent
     * @return -1 if there was an error<br>else the new number of SMS sent today
     */
    public int increaseSentMsg(int incr, String cookie) {
        int sentmsg;
        if (getIntConfig(MAX_MSG) > 0) {
            sentmsg = getIntConfig(SENT_MSG) + incr;
        } else {
            sentmsg = 0;
        }
        int sentmonthlymsg;
        if (getIntConfig(MAX_MONTHLY_MSG) > 0) {
            sentmonthlymsg = getIntConfig(SENT_MONTHLY_MSG) + incr;
        } else {
            sentmonthlymsg = 0;
        }
        int result = this.set(getStringConfig(NAME), getStringConfig(ADDRESS), getIntConfig(NEED_USER), getStringConfig(USER), getIntConfig(NEED_PASS), getStringConfig(PASS), getIntConfig(NEED_NICK), getStringConfig(NICK), getIntConfig(MAX_RECIPIENTS), getIntConfig(MAX_CHARS), getIntConfig(MAX_MSG), sentmsg, getIntConfig(IS_NUMERIC_RECIPIENT), getStringConfig(SIGN), getStringConfig(XCLUDED_CHAR), getStringConfig(SUBS_CHAR), getStringConfig(COUNT_MORE), getStringConfig(COUNT_FOR), getIntConfig(IS_DEFAULT), cookie, getIntConfig(MAX_MONTHLY_MSG), sentmonthlymsg);
        if (result > 0) {
            serviceStringTokenized[COOKIE] = cookie;
            serviceStringTokenized[SENT_MSG] = String.valueOf(sentmsg);
            serviceStringTokenized[SENT_MONTHLY_MSG] = String.valueOf(sentmonthlymsg);
            return sentmsg;
        } else {
            return -1;
        }
    }

    /**
     * Set a service as default
     * @param isDefault true to set as default, false to unset
     * @return -1 if there was an error,else 1
     */
    public int setDefault(boolean isDefault) {
        int defaultBit = 0;
        if (isDefault) {
            defaultBit = 1;
        } else {
            defaultBit = 0;
        }
        int result = this.set(getStringConfig(NAME), getStringConfig(ADDRESS), getIntConfig(NEED_USER), getStringConfig(USER), getIntConfig(NEED_PASS), getStringConfig(PASS), getIntConfig(NEED_NICK), getStringConfig(NICK), getIntConfig(MAX_RECIPIENTS), getIntConfig(MAX_CHARS), getIntConfig(MAX_MSG), getIntConfig(SENT_MSG), getIntConfig(IS_NUMERIC_RECIPIENT), getStringConfig(SIGN), getStringConfig(XCLUDED_CHAR), getStringConfig(SUBS_CHAR), getStringConfig(COUNT_MORE), getStringConfig(COUNT_FOR), defaultBit, getStringConfig(COOKIE), getIntConfig(MAX_MONTHLY_MSG), getIntConfig(SENT_MONTHLY_MSG));
        if (result > 0) {
            serviceStringTokenized[IS_DEFAULT] = String.valueOf(defaultBit);
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Reset the number of SMS sent for the service
     * @param onlyDaily true to reset only daily messages, false to reset monthly too
     * @return -1  if there was an error,else 1
     */
    public int resetSentMsg(boolean onlyDaily) {
        int sentmsg = 0;
        int sentmonthlymsg = getIntConfig(SENT_MONTHLY_MSG);
        if (!onlyDaily) {
            sentmonthlymsg = 0;
        }
        int result = this.set(getStringConfig(NAME), getStringConfig(ADDRESS), getIntConfig(NEED_USER), getStringConfig(USER), getIntConfig(NEED_PASS), getStringConfig(PASS), getIntConfig(NEED_NICK), getStringConfig(NICK), getIntConfig(MAX_RECIPIENTS), getIntConfig(MAX_CHARS), getIntConfig(MAX_MSG), sentmsg, getIntConfig(IS_NUMERIC_RECIPIENT), getStringConfig(SIGN), getStringConfig(XCLUDED_CHAR), getStringConfig(SUBS_CHAR), getStringConfig(COUNT_MORE), getStringConfig(COUNT_FOR), getIntConfig(IS_DEFAULT), getStringConfig(COOKIE), getIntConfig(MAX_MONTHLY_MSG), sentmonthlymsg);
        if (result > 0) return getIntConfig(SENT_MSG); else return -1;
    }

    /**
     * Get the RMS DB id of the current sevice
     * @return the RMS DB id
     */
    public int getRmsId() {
        return ownRmsId;
    }

    /**
     * Check is a service is configured properly with all required fields filled
     * @return true if it is configurated, else false
     */
    public boolean isConfigured() {
        if ((getIntConfig(NEED_USER) == 1) && (getStringConfig(USER).length() < 1)) return false;
        if ((getIntConfig(NEED_PASS) == 1) && (getStringConfig(PASS).length() < 1)) return false;
        if ((getIntConfig(NEED_NICK) == 1) && (getStringConfig(NICK).length() < 1)) return false;
        return true;
    }

    /**
     * Build a services array from services RMS RecorStore
     * @return An array of all services in RMS
     */
    private static Service[] servicesArray() {
        RecordStore rs = null;
        int records = 0;
        Service[] services_array = new Service[0];
        try {
            rs = RecordStore.openRecordStore(RN, true);
            records = rs.getNumRecords();
            if (records > 0) {
                services_array = new Service[records];
                RecordEnumeration e = rs.enumerateRecords(null, null, true);
                int arraypos = 0;
                while (e.hasNextElement()) {
                    int record = e.nextRecordId();
                    Service serviceTemp = new Service(new String(rs.getRecord(record)), record);
                    services_array[arraypos] = serviceTemp;
                    arraypos++;
                }
                e.destroy();
            }
        } catch (Exception e) {
        } finally {
            try {
                rs.closeRecordStore();
            } catch (Exception e) {
            }
        }
        return services_array;
    }

    /**
     * Checks the string passed to create/modify a service.
     * @param stringa A full, token-separated, service string
     * @return true if the string is well formed and can be used to create 
     * the service, FALSE otherwise
     */
    private static boolean isValidServiceString(String stringa) {
        String[] splitted = Utils.splitString(stringa, token);
        if (splitted.length != (INDEX_MAX)) {
            return false;
        }
        if (splitted[NAME].length() < 1) {
            return false;
        }
        if (splitted[ADDRESS].length() < 8) {
            return false;
        }
        if (!(splitted[NEED_USER].equals("0") || splitted[NEED_USER].equals("1") || splitted[NEED_USER].equals("2"))) {
            return false;
        }
        if (!(splitted[NEED_PASS].equals("0") || splitted[NEED_PASS].equals("1") || splitted[NEED_PASS].equals("2"))) {
            return false;
        }
        if (!(splitted[NEED_NICK].equals("0") || splitted[NEED_NICK].equals("1") || splitted[NEED_NICK].equals("2"))) {
            return false;
        }
        try {
            if (Integer.parseInt(splitted[MAX_RECIPIENTS]) < 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        try {
            if (Integer.parseInt(splitted[MAX_CHARS]) < 1) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        try {
            if (Integer.parseInt(splitted[MAX_MSG]) < 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        try {
            if (Integer.parseInt(splitted[SENT_MSG]) < 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        if (!(splitted[IS_NUMERIC_RECIPIENT].equals("0") || splitted[IS_NUMERIC_RECIPIENT].equals("1") || splitted[IS_NUMERIC_RECIPIENT].equals("2"))) {
            return false;
        }
        return true;
    }

    /**
     * Insert a new service into the RMS.
     * @param name Name for this service
     * @param address Installation URL
     * @param nu <B>1</B> if username is <B>required</B> for this service, <B>0</B> if it's not needed, <B>2</B> if it's <B>optional</B>
     * @param np <B>1</B> if password is <B>required</B> for this service, <B>0</B> if it's not needed, <B>2</B> if it's <B>optional</B>
     * @param nn <B>1</B> if nick is <B>required</B> for this service, <B>0</B> if it's not needed, <B>2</B> if it's <B>optional</B>
     * @param mr Max number of recipients for a message sent with this service
     * @param mc max chars that can be used for this service to write a {@link org.gotext.Message Message}
     * @param mm Max number of messages a user can send daily.
     * @param in <b>1</b> if service supports numeric recipients only, <B>0</B> if supports email 
     * recipients only, <b>2</b> if service supports <i>mixed mode</i> recipients (1 numeric and 1 mail, only one at time)
     * @param xc Lists characters of not available characters for this service
     * <br>It is a <I>pipe</I>-separated string of characters (eg: "à|è|ì|")
     * @param sc List of characters to replace to the not available ones.
     * <br>It is a <I>pipe</I>-separated string of strings (eg: "a'|e'|i'|euro")
     * @param cm Lists characters that counts more than 1 for this service.
     * <br>It is a <I>pipe</I>-separated string of characters (eg: "à|?|\\n")
     * @param cf Lists how much each char of the <I>countmore</I> list counts.
     * <br>It is a <I>pipe</I>-separated string of characters (eg: "3|2|2")
     * @param mmm Max number of messages a user can send in a month.
     * <br>If <B>0</B> counting of monthly limit is <B>disabled</B>.
     * @return same as {@link #insService(String)}
     */
    public static int insServiceConfig(String name, String address, String nu, String np, String nn, String mr, String mc, String mm, String in, String xc, String sc, String cm, String cf, String mmm) {
        nu = nu.trim();
        np = np.trim();
        nn = nn.trim();
        mr = mr.trim();
        mc = mc.trim();
        mm = mm.trim();
        String sm = 0 + "";
        in = in.trim();
        xc = xc.trim();
        cm = cm.trim();
        cf = cf.trim();
        String id = 0 + "";
        String cookie = "";
        mmm = mmm.trim();
        String smm = 0 + "";
        return insService(name + token + address + token + nu + token + token + np + token + token + nn + token + token + mr + token + mc + token + mm + token + sm + token + in + token + token + xc + token + sc + token + cm + token + cf + token + id + token + cookie + token + mmm + token + smm);
    }

    /**
     * Creates a new RMS record to store the supplied Service.
     * Inserisce nel RMS la stringa del servizio passata come parametro.
     * @param stringa A full, token-separated, service string to save
     * @return <ul>
     *    <li>0 : service string can't be saved (it is not well formed)</li>
     *    <li>-1 : unspecified error in RMS</li>
     *    <li>n(>0) : string has been correctly saved with the ID <b>n</b>.</li>
     * </ul>
     */
    private static int insService(String stringa) {
        int response = 0;
        RecordStore rs = null;
        if (isValidServiceString(stringa)) {
            try {
                rs = RecordStore.openRecordStore(RN, true);
                response = rs.addRecord(stringa.getBytes(), 0, stringa.getBytes().length);
            } catch (Exception ee) {
                response = -1;
            } finally {
                try {
                    rs.closeRecordStore();
                } catch (Exception eee) {
                }
                Service.generateServicesArray();
            }
        }
        return response;
    }

    /**
     * Updates a service in the RMS
     * @param stringa A full, token-separated string to be put in place of the old service string
     * @param id The RMS ID of the service you want to update
     * @return <ul>
     *    <li>-1 : if errors occured</li>
     *    <li>id : if this service with RMS the supplied <I>id</I> has been successfuly updated.</li>
     * </ul>
     */
    public static int modService(String stringa, int id) {
        int response = 0;
        RecordStore rs = null;
        if (isValidServiceString(stringa)) {
            try {
                rs = RecordStore.openRecordStore(RN, true);
                rs.setRecord(id, stringa.getBytes(), 0, stringa.getBytes().length);
                response = id;
            } catch (Exception ee) {
                response = -1;
            } finally {
                try {
                    rs.closeRecordStore();
                } catch (Exception eee) {
                }
                Service.generateServicesArray();
            }
        }
        return response;
    }

    /**
     * Deletes a service from the RMS
     * @param id The RMS ID of the service you want to delete
     * @return <ul>
     *    <li>-1 : if errors occured</li>
     *    <li>id : if this service with RMS the supplied <I>id</I> has been successfuly deleted.</li>
     * </ul>
     */
    public static int delService(int id) {
        int temp = 0;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RN, false);
            rs.deleteRecord(id);
            temp = id;
        } catch (Exception ee) {
            temp = -1;
        } finally {
            try {
                rs.closeRecordStore();
            } catch (Exception eee) {
            }
            Service.generateServicesArray();
        }
        return temp;
    }

    /**
     * Empties service list and RMS
     */
    public static void formatServices() {
        try {
            RecordStore.deleteRecordStore(RN);
        } catch (Exception e) {
        } finally {
            Service.generateServicesArray();
        }
    }

    /**
     * Checks if a service Name is already existing in Service List.
     * @param name desired name to check
     * @return true if name is already used, false otherwise
     */
    public static boolean existServiceName(String name) {
        for (int i = 0; i < goText.services.length; i++) {
            if (goText.services[i].getStringConfig(NAME).equals(name)) return true;
        }
        return false;
    }

    /**
     * Switches services position in sevice list (and in RMS).
     * <p>
     * This works fine on most mobiles but not all (fine on Nokias, not fine on Sony Ericssons)
     * @param service_1 The first service to move
     * @param service_2 The second service to move
     * @see org.gotext.Service.ServiceSwitcherAlt Service.ServiceSwitcherAlt
     */
    public static void switchServicePosition(Service service_1, Service service_2) {
        int id_1 = service_1.getRmsId();
        String string_1 = service_1.getString();
        int id_2 = service_2.getRmsId();
        String string_2 = service_2.getString();
        goText.log("Service 1: " + Service.modService(string_1, id_2));
        goText.log("Service 2: " + Service.modService(string_2, id_1));
    }

    /**
     * This is used to run the alternative service reordering algorithm.
     * <p>
     * It is a runnable class that supports WaitingScreen because this longer algo
     * requires more time to be completed.
     * <br>This is necessary on SonyEricssons (and maybe others) that are not working with 
     * the standard service switching way.
     * @see Service#switchServicePosition(Service, Service)
     * @author Zydio >> zydio AT users DOT sf DOT net
     */
    protected static class ServiceSwitcherAlt implements Utils.WaitProgressSupporter {

        /**
         * original position of the first service to move
         */
        private int pos1;

        /**
         * original position of the second service to move
         */
        private int pos2;

        /**
         * goText services array
         */
        private Service[] services;

        /**
         * waiting screen to be updated in the process
         */
        private Utils.WaitProgressScreen waitscreen;

        /**
         * Creates a new istance of the alternative service switcher
         * @param pos1 Position of the first service to switch
         * @param pos2 Position of the second service to switch
         */
        public ServiceSwitcherAlt(int pos1, int pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            services = goText.services;
            waitscreen = null;
        }

        /**
         * Associates a waiting screen to this process, to be updated while it works
         * @param waitscreen The waiting screen that will display the progress
         */
        public void setWaitScreen(Utils.WaitProgressScreen waitscreen) {
            this.waitscreen = waitscreen;
        }

        /**
         * Starts service position switching
         */
        public void run() {
            if (waitscreen != null) {
                int increment = 0;
                int progress = 0;
                increment = (20 / services.length) % 10;
                waitscreen.setPercent(0);
                Service services_new[] = new Service[services.length];
                if (pos1 == -1) {
                    for (int i = 0; i < services.length - 1; i++) {
                        services_new[i] = services[i + 1];
                        progress += increment;
                        waitscreen.setPercent(progress);
                        Thread.yield();
                    }
                    services_new[services.length - 1] = services[0];
                } else if (pos2 == -1) {
                    for (int i = 1; i < services.length; i++) {
                        services_new[i] = services[i - 1];
                        progress += increment;
                        waitscreen.setPercent(progress);
                        Thread.yield();
                    }
                    services_new[0] = services[services.length - 1];
                } else {
                    for (int i = 0; i < services.length; i++) {
                        services_new[i] = services[i];
                        if (i == pos1) {
                            services_new[i] = services[pos2];
                        } else if (i == pos2) {
                            services_new[i] = services[pos1];
                        }
                        progress += increment;
                        waitscreen.setPercent(progress);
                        Thread.yield();
                    }
                }
                formatServices();
                for (int i = (services_new.length - 1); i >= 0; i--) {
                    Service.insService(services_new[i].getString());
                    progress += (increment * 4);
                    waitscreen.setPercent(progress);
                    Thread.yield();
                }
                waitscreen.setPercent(Utils.WaitProgressScreen.PERCENT_MAX);
            }
        }

        /**
         * Task to do when switching ends: it shows again services list
         */
        public void doAtEnd() {
            new ListModServices();
        }
    }

    /**
     * Rebuilds goText's global static list of services
     */
    public static void generateServicesArray() {
        goText.services = Service.servicesArray();
    }

    /**
     * If there is a default services is reset to non-default
     */
    public static void resetDefaultService() {
        int current_default = Service.getDefaultService();
        if (current_default != -1) {
            goText.services[current_default].setDefault(false);
        }
    }

    /**
     * Gets the default service, if any
     * @return The default service
     */
    public static int getDefaultService() {
        int temp_def = -1;
        for (int i = 0; i < goText.services.length; i++) {
            if (goText.services[i].getIntConfig(IS_DEFAULT) == 1) temp_def = i;
        }
        return temp_def;
    }
}
