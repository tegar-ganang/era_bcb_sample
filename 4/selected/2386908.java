package com.objectwave.appArch.security;

/**
* Essentially, this is just a centralized class for 
* registering security listeners.
*/
public class SecurityController implements SecurityListener {

    public static final int READBIT = 1;

    public static final int WRITEBIT = 2;

    public static final int EXECUTEBIT = 4;

    SecuritySupport support = new SecuritySupport(this);

    static SecurityController defaultManager;

    boolean verbose = System.getProperty("ow.securityVerbose") != null;

    /**
	*/
    public void addSecurityListener(SecurityListener item) {
        support.addSecurityListener(item);
    }

    public boolean checkExecuteAccess(Object action, Object user) {
        SecurityType type = getAccessRights(action, user);
        boolean result;
        if (type == NONE) result = true; else result = type.hasExecuteAccess();
        if (verbose) System.out.println("Check execute access " + result);
        return result;
    }

    public boolean checkReadAccess(Object action, Object user) {
        SecurityType type = getAccessRights(action, user);
        boolean result;
        if (type == NONE) result = true; else result = type.hasReadAccess();
        if (verbose) System.out.println("Check read access " + result);
        return result;
    }

    /**
	* -1 = no rights.
	* 1 = Read - Bit indicated read value.
	* 2 = Write -
	* 4 = Execute -
	*/
    public int checkReadWriteExecute(Object action, Object user) {
        SecurityType type = getAccessRights(action, user);
        boolean result;
        int resultInt = -1;
        if (type == NONE) {
            resultInt = 7;
        } else {
            result = type.hasWriteAccess();
            if (result) resultInt = WRITEBIT;
            if (type.hasReadAccess()) resultInt += READBIT;
            if (type.hasExecuteAccess()) resultInt += EXECUTEBIT;
        }
        if (verbose) System.out.println("Check read write access " + resultInt);
        return resultInt;
    }

    public boolean checkWriteAccess(Object target, Object user) {
        SecurityType type = getAccessRights(target, user);
        boolean result;
        if (type == NONE) result = true; else result = type.hasWriteAccess();
        if (verbose) System.out.println("Check write access " + result);
        return result;
    }

    /**
	* Thread safe dispatching of an event.
	*/
    public SecurityType getAccessRights(Object action, Object user) {
        return support.getAccessRights(action, user);
    }

    /**
	*/
    public static SecurityController getDefaultManager() {
        if (defaultManager == null) defaultManager = new SecurityController();
        return defaultManager;
    }

    /**
	*/
    public void removeSecurityListener(SecurityListener item) {
        support.removeSecurityListener(item);
    }

    /**
	*/
    public static void setDefaultManager(SecurityController mgr) {
        defaultManager = mgr;
    }
}
