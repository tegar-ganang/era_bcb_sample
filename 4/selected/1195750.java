package eu.goldenak.ircbot;

import java.util.HashMap;
import java.security.InvalidParameterException;

/**
 * This class will provide the bot with some essential security features. With
 * these one will be able to limit a certain command or a complete module to
 * one person or a set of people. This way, you can create some administrative
 * or controlling commands, without the danger of other people using them,
 * accidently or not.
 */
public class SecurityManager {

    /**
	 * Some standard levels, ready for use.
	 */
    public static int NONE = 0;

    public static int BOTOWNER = 99;

    /**
	 * The place where all levels will be stored.
	 */
    private HashMap<String, Level> m_aLevel;

    /**
	 * The constructor simply prepares one another.
	 */
    public SecurityManager() {
        m_aLevel = new HashMap<String, Level>();
    }

    /**
	 * This method will add a level to the level array. Returns the unique ID
	 * for the level in the levels HashMap.
	 */
    public int addLevel(Level pLevel) {
        if (pLevel.getLevel() > BOTOWNER) {
            throw new InvalidParameterException("Cannot assign a level " + "higher than the bot owner.");
        }
        return -1;
    }

    public boolean removeLevel(Level pLevel) {
        return true;
    }

    /**
	 * Returns the level for the given user (no wildcards) in the given channel
	 * (case insensitive).
	 */
    public int getLevel(String sUser, String sChannel) {
        return 0;
    }

    /**
	 * This method will check if the given user string matches the given mask.
	 * A mask can contain wildcards, such as * and ?. The * wildcard matches
	 * anything of any length, while the ? wildcard matches only one character.
	 * For now, we'll only support the * wildcard. All matching is always done
	 * in an case insensitive manner.
	 */
    public static boolean matchUserMask(String sUserMask, String sUser) {
        return true;
    }

    /**
	 * This inner class represents a single level with a certain user mask in
	 * the SecurityManager.
	 */
    public class Level {

        /**
		 * The user mask this level applies to. The only wildcards that's
		 * supported is '*'. '?' may be added in the future.
		 */
        private String m_sUserMask;

        /**
		 * The channels this level applies in, if the active channel is not in
		 * here, then the user will simply have level 0. If it's null or empty,
		 * then it applies to all channels.
		 */
        private String[] m_aChannel;

        /**
		 * The actual level this user will be given when everything matches.
		 */
        private int m_nLevel;

        /**
		 * This constructor will allow the given usermask the given levels in
		 * all thinkable channels, where this bot is in as well, of course.
		 */
        public Level(String sUserMask, int nLevel) {
            this(sUserMask, null, nLevel);
        }

        /**
		 * This constructor will fully initialize this Level object. The
		 * aChannel array can be omitted by specifying null, which will allow
		 * the given level to the given usermask in all channels.
		 */
        public Level(String sUserMask, String[] aChannel, int nLevel) {
            m_sUserMask = sUserMask;
            m_aChannel = aChannel;
            m_nLevel = nLevel;
        }

        public int getLevel() {
            return m_nLevel;
        }

        public String getUserMask() {
            return m_sUserMask;
        }

        /**
		 * Probably better to use IrcChannel?
		 */
        public String[] getChannels() {
            return m_aChannel;
        }

        public boolean matches(String sUser) {
            return m_sUserMask.equalsIgnoreCase(sUser);
        }
    }
}
