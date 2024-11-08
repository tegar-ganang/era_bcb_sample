package org.rakiura.mbot;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.Serializable;

/**
 * Represent abstract user information container.
 *<br><br>
 * AbstractUser.java<br>
 * <br>
 * Created: Tue Aug  3 18:14:20 1999<br>
 *
 *@author Mariusz Nowostawski
 *@version $Revision: 1.3 $  
 */
public abstract class AbstractUser implements Serializable {

    protected HashSet flags;

    protected Hashtable chanflags;

    public AbstractUser() {
        flags = new HashSet();
        chanflags = new Hashtable();
    }

    public void addFlag(Character ch) {
        flags.add(ch);
    }

    public void removeFlag(Character ch) {
        flags.remove(ch);
    }

    public void addFlags(String s) {
        synchronized (flags) {
            for (int i = 0; i < s.length(); i++) addFlag(new Character(s.charAt(i)));
        }
    }

    public void removeFlags(String s) {
        synchronized (flags) {
            for (int i = 0; i < s.length(); i++) removeFlag(new Character(s.charAt(i)));
        }
    }

    public boolean isFlag(Character ch) {
        return (flags.get(ch) != null);
    }

    public boolean isFlag(char ch) {
        return isFlag(new Character(ch));
    }

    public boolean isFlags(String f) {
        synchronized (flags) {
            int how_many = 0;
            int max = f.length();
            for (int i = 0; i < max; i++) {
                if (isFlag(new Character(f.charAt(i)))) how_many++;
            }
            return how_many == max;
        }
    }

    public void addFlag(String chan, Character ch) {
        synchronized (chanflags) {
            HashSet s = (HashSet) chanflags.get(chan);
            if (s == null) {
                s = new HashSet();
                chanflags.put(chan, s);
            }
            s.add(ch);
        }
    }

    public void removeFlag(String chan, Character ch) {
        synchronized (chanflags) {
            HashSet s = (HashSet) chanflags.get(chan);
            if (s == null) return;
            s.remove(ch);
        }
    }

    public void addFlags(String ch, String s) {
        synchronized (chanflags) {
            for (int i = 0; i < s.length(); i++) addFlag(ch, new Character(s.charAt(i)));
        }
    }

    public void removeFlags(String ch, String s) {
        synchronized (chanflags) {
            for (int i = 0; i < s.length(); i++) removeFlag(ch, new Character(s.charAt(i)));
        }
    }

    public Enumeration getFlags() {
        return flags.elements();
    }

    public Enumeration getChannels() {
        return chanflags.keys();
    }

    public Enumeration getChanFlags(String chan) {
        return ((HashSet) chanflags.get(chan)).elements();
    }

    public boolean isFlag(String f, char ch) {
        return isFlag(f, new Character(ch));
    }

    public boolean isFlag(String chan, Character ch) {
        if (isFlag(ch)) return true;
        HashSet s = (HashSet) chanflags.get(chan);
        if (s == null) return false;
        if (s.get(ch) != null) return true; else return false;
    }

    public boolean isFlags(String chan, String f) {
        int how_many = 0;
        int max = f.length();
        for (int i = 0; i < max; i++) {
            if (isFlag(chan, new Character(f.charAt(i)))) how_many++;
        }
        return how_many == max;
    }

    public void setOp(String chan) {
        addFlag(chan, new Character('o'));
    }

    public boolean isOp(String chan) {
        return isFlag(chan, 'o');
    }

    public void setVoice(String chan) {
        addFlag(chan, new Character('v'));
    }

    public void resetVoice(String chan) {
        removeFlag(chan, new Character('v'));
    }

    public boolean isVoice(String chan) {
        return isFlag(chan, 'v');
    }
}
