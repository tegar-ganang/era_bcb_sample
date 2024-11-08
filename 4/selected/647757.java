package com.objectwave.transactSecurity;

import com.objectwave.transactSecurity.*;

/**
 *  Use this class to create Object level access restrictions and to process
 *  ObjectAccessed events. You would use this class to enforce Security policys.
 *  It shouldn't be used to do general security issues. Generally works like
 *  this. >>objectAccessed() Get current ExternalActorProfile. Get
 *  ObjectAccessRights Determine access allowed, throw exception if not! This
 *  class is really a raw implementation. Use appendClassProfile(Class,
 *  readProfiles, writeProfiles) to create the access lists. Examples This would
 *  force mandatory granting of write access for every object that is to be
 *  written to. userProfilePolicy.appendClassProfile(Object.class, -1, 0); Read
 *  access is granted to all profiles. Only those of profile type 2 have write
 *  access to implementors of AccessSecurityIF
 *  userProfilePolicy.appendClassProfile(AccessSecurityIF.class, -1, 2);
 *
 * @author  dhoag
 * @version  $Id: UserProfilePolicy.java,v 2.0 2001/06/11 16:00:04 dave_hoag Exp $
 */
public class UserProfilePolicy implements com.objectwave.transactSecurity.ObjectAccessListener {

    long currentActorProfiles = 0L;

    ClassProfiles profiles = new ClassProfiles();

    boolean verbose = true;

    /**
	 *  Test method.
	 *
	 * @param  args The command line arguments
	 */
    public static void main(String[] args) {
        AccessSecurityManager mgr = new AccessSecurityManager();
        UserProfilePolicy list = new UserProfilePolicy();
        mgr.addClassObjectAccessListener(list, Object.class);
        mgr.addClassObjectAccessListener(list, String.class);
        list.setCurrentActorProfiles(5);
        list.appendClassProfile(Object.class, -1, 1);
        list.appendClassProfile(com.objectwave.transactionalSupport.AccessSecurityIF.class, -1, 2);
        mgr.checkWriteAccess("StringValue", null);
        System.out.println("Success");
        mgr.checkWriteAccess(mgr, null);
        System.out.println("Success");
    }

    /**
	 *  Used to establish the rights for the current ExternalActor Each bit
	 *  represents a profile of the current ExternalActor.
	 *
	 * @param  l The new CurrentActorProfiles value
	 */
    public void setCurrentActorProfiles(final long l) {
        currentActorProfiles = l;
    }

    /**
	 *  Do a bitwise or with any existing class profiles for this class. To start
	 *  blank, first remove profile for this class.
	 *
	 * @param  read - All profiles that have read access to Class c should be
	 *      marked in this parameter.
	 * @param  write - All profiles that have write access to Class c should be
	 *      marked in this parameter.
	 * @param  c - The class for which access rights are being established.
	 */
    public void appendClassProfile(final Class c, final long read, final long write) {
        profiles = profiles.addClassProfile(c, read, write);
    }

    /**
	 *  Called when someone is trying to access an object.
	 *
	 * @param  event
	 */
    public void objectAccessed(final ObjectAccessEvent event) {
        final Object source = event.getSource();
        if (verbose) {
            System.out.println("Source " + source);
        }
        final long val = currentActorProfiles & profiles.getClassProfileTotal(source, event == ObjectAccessEvent.READ);
        if (val == 0) {
            throw new SecurityException("Attempt to access: " + source.getClass() + " User Profile Policy prohibits action!");
        }
    }

    /**
	 *  Remove any access restrictions for class C. The class, 'c', has to be an
	 *  Exact Match to one of the classes in the list to have any effect.
	 *
	 * @param  c
	 */
    public void removeProfile(final Class c) {
        profiles = profiles.removeClassProfile(c);
    }

    /**
	 *  This implementation of class profiles assumes a limited number of classes
	 *  with rules. This class should never be used by outside systems.
	 *
	 * @author  dhoag
	 * @version  $Id: UserProfilePolicy.java,v 2.0 2001/06/11 16:00:04 dave_hoag Exp $
	 */
    class ClassProfiles {

        ClassProfiles next = null;

        Class type;

        long readProfile;

        long writeProfile;

        /**
		 * @param  target
		 * @return  The ClassProfile value
		 */
        public ClassProfiles getClassProfile(final Object target) {
            if (type == target.getClass()) {
                return this;
            }
            if (next != null) {
                return next.getClassProfile(target);
            }
            return null;
        }

        /**
		 *  Current design: Grant the most possible access rights. Start with full
		 *  rights and slowly remove them. If there are no matches, then nothing will
		 *  be removed and everything will be granted.
		 *
		 * @param  target
		 * @param  read
		 * @return  The ClassProfileTotal value
		 */
        public long getClassProfileTotal(final Object target, final boolean read) {
            long val = -1;
            if (next == null) {
                return val;
            }
            val = next.getClassProfileTotal(target, read);
            if (type.isInstance(target)) {
                if (type == target.getClass()) {
                    return read ? readProfile & val : writeProfile & val;
                } else {
                    return read ? readProfile & val : writeProfile & val;
                }
            }
            return -1 & val;
        }

        /**
		 *  Do a bitwise or with any existing class profiles for this class.
		 *
		 * @param  c The feature to be added to the ClassProfile attribute
		 * @param  read The feature to be added to the ClassProfile attribute
		 * @param  write The feature to be added to the ClassProfile attribute
		 * @return
		 */
        public ClassProfiles addClassProfile(final Class c, final long read, final long write) {
            if (next != null) {
                if (type == c) {
                    readProfile = readProfile | read;
                    writeProfile = writeProfile | write;
                    return this;
                }
                next = next.addClassProfile(c, read, write);
                return this;
            }
            ClassProfiles result = new ClassProfiles();
            result.type = c;
            result.next = this;
            result.readProfile = read;
            result.writeProfile = write;
            return result;
        }

        /**
		 * @param  c
		 * @return
		 */
        public ClassProfiles removeClassProfile(final Class c) {
            ClassProfiles head = this;
            if (type == c) {
                return next;
            }
            if (next != null) {
                next = next.removeClassProfile(c);
            }
            return head;
        }
    }
}
