package com.ibm.atp.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The <tt>Auth</tt> class is the class for challenge-response authentication.
 * 
 * @version 1.00 $Date: 2009/07/28 07:04:53 $
 * @author ONO Kouichi
 */
public abstract class Auth extends Object {

    /**
     * The Message Digest (one-way hash) function algorithm. "SHA", "SHA-1",
     * "MD5" and "MD2". docs/guide/security/CryptoSpec.html#AppA
     */
    private final String DEFAULTDIGESTALGORITHM = "SHA-1";

    private String _digestAlgorithm = this.DEFAULTDIGESTALGORITHM;

    /**
     * A Message Digest (one-way hash) function
     */
    private MessageDigest _digest = null;

    /**
     * The turn of protocol : No turns.
     */
    public static final int NO_TURNS = 0;

    /**
     * The turn of protocol : first turn.
     */
    public static final int FIRST_TURN = 1;

    /**
     * The turn of protocol : second turn.
     */
    public static final int SECOND_TURN = 2;

    /**
     * The pad of turn.
     */
    private static final String FIRST_TURN_PAD = "F";

    private static final String SECOND_TURN_PAD = "S";

    /**
     * The delimiter of turn pad and identifiers of turn individuals.
     */
    private static final String TURN_DELIMITER = ":";

    /**
     * The identifiers of turn individuals.
     */
    private String _idFirst = null;

    private String _idSecond = null;

    /**
     * Default constructor creates a default message digest function.
     */
    protected Auth() {
        this.setDigestAlgorithm(this.DEFAULTDIGESTALGORITHM);
    }

    /**
     * Constructor creates a specified message digest function.
     * 
     * @param name
     *            the name of message digest function algorithm
     */
    protected Auth(String name) {
        this.setDigestAlgorithm(name);
    }

    /**
     * Adds byte sequence into digest function.
     * 
     * @param bytes
     *            byte sequence to be added
     */
    protected void addBytes(byte[] bytes) {
        this._digest.update(bytes);
    }

    /**
     * Calculate response value for authentication.
     * 
     * @param turn
     *            of individual
     * @param challenge
     *            a challenge
     * @return response value for authentication
     * @exception AuthenticationException
     *                byte sequence for response is invalid
     */
    public abstract byte[] calculateResponse(int turn, Challenge challenge) throws AuthenticationException;

    /**
     * Returns the name of message digest function algorithm.
     * 
     * @return the name of message digest function algorithm.
     */
    public String getDigestAlgorithm() {
        return this._digestAlgorithm;
    }

    /**
     * Returns digest value by digest function.
     * 
     * @return digest value by digest function
     */
    protected byte[] getDigestValue() {
        return this._digest.digest();
    }

    /**
     * Gets the identifier of first turn individual.
     * 
     * @return identifier of first turn individual
     */
    public String getFirstTurnIdentifier() {
        return this._idFirst;
    }

    /**
     * Gets the identifier of second turn individual.
     * 
     * @return identifier of second turn individual
     */
    public String getSecondTurnIdentifier() {
        return this._idSecond;
    }

    /**
     * Returns the pad of turn individual.
     * 
     * @param turn
     *            turn of infividual
     * @return pad of turn individual
     */
    protected String getTurnPad(int turn) {
        String pad = null;
        switch(turn) {
            case FIRST_TURN:
                pad = FIRST_TURN_PAD + TURN_DELIMITER + this._idSecond + TURN_DELIMITER + this._idFirst;
                break;
            case SECOND_TURN:
                pad = SECOND_TURN_PAD + TURN_DELIMITER + this._idFirst + TURN_DELIMITER + this._idSecond;
                break;
            default:
            case NO_TURNS:
                pad = null;
                break;
        }
        return pad;
    }

    /**
     * Returns hased value by digest function for the turn player.
     * 
     * @param turn
     *            turn of infividual
     * @param challenge
     *            a challenge
     * @return hased value by digest function for the turn player
     * @exception AuthenticationException
     *                byte sequence to be hased is invalid
     */
    protected abstract byte[] hash(int turn, Challenge challenge) throws AuthenticationException;

    /**
     * Resets message digest function.
     */
    protected void resetDigest() {
        this._digest.reset();
    }

    /**
     * Sets the name of message digest function algorithm.
     * 
     * @param name
     *            the name of message digest function algorithm
     */
    protected void setDigestAlgorithm(String name) {
        this._digestAlgorithm = name;
        try {
            this._digest = MessageDigest.getInstance(this._digestAlgorithm);
        } catch (NoSuchAlgorithmException excpt) {
            System.out.println("Exception: Authenticate: " + excpt);
            this._digestAlgorithm = null;
            this._digest = null;
        }
    }

    /**
     * Sets the identifier of first turn individual.
     * 
     * @param id
     *            identifier of first turn individual
     */
    public void setFirstTurnIdentifier(String id) {
        this._idFirst = id;
    }

    /**
     * Sets the identifier of second turn individual.
     * 
     * @param id
     *            identifier of second turn individual
     */
    public void setSecondTurnIdentifier(String id) {
        this._idSecond = id;
    }

    /**
     * Verify response value for authentication.
     * 
     * @param turn
     *            of individual
     * @param challenge
     *            a challenge
     * @param response
     *            response value for authentication
     * @exception AuthenticationException
     *                byte sequence for response is invalid
     */
    public abstract boolean verify(int turn, Challenge challenge, byte[] response) throws AuthenticationException;

    /**
     * Verify response value for authentication.
     * 
     * @param turn
     *            of individual
     * @param challenge
     *            a challenge
     * @param response
     *            response value for authentication
     * @exception AuthenticationException
     *                byte sequence for response is invalid
     */
    public abstract boolean verify(int turn, Challenge challenge, ByteSequence response) throws AuthenticationException;
}
