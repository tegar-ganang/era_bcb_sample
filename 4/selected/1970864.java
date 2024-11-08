package org.maverickdbms.basic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.IOException;
import org.maverickdbms.basic.term.PseudoTerminal;
import org.maverickdbms.basic.term.Termios;

/**
* Class provides input routines needed by the compiler
*/
public class InputChannel {

    private static final String PROP_HOLD_FILE = "org.maverickdbms.basic.hold_file";

    private Session session;

    private PseudoTerminal pty;

    private Factory factory;

    private Reader in;

    private boolean termEnabled = false;

    InputChannel(Session session, PseudoTerminal pty) {
        this.session = session;
        this.pty = pty;
        this.factory = session.getFactory();
        this.in = pty.getReader();
    }

    public MaverickString positionScreenAt(MaverickString result, ConstantString col, ConstantString row) throws MaverickException {
        result.clear();
        Terminal e = session.getEmulation();
        if (!termEnabled) {
            result.set(e.getEscapeSequence(Terminal.TERM_CLEARSCREEN));
            termEnabled = true;
        }
        result.append(e.positionScreenAt(row.intValue(), col.intValue()));
        return result;
    }

    public MaverickString getEscapeSequence(MaverickString result, ConstantString num) throws MaverickException {
        result.clear();
        Terminal e = session.getEmulation();
        if (!termEnabled) {
            result.set(e.getEscapeSequence(Terminal.TERM_CLEARSCREEN));
            termEnabled = true;
        }
        int val = num.intValue();
        if (val < 0) {
            result.append(e.getEscapeSequence(val));
        } else {
            result.append(e.positionScreenAt(val));
        }
        return result;
    }

    public void CLEARDATA() throws MaverickException {
        try {
            while (in.ready()) {
                in.read();
            }
        } catch (IOException ioe) {
            throw new MaverickException(0, ioe);
        }
    }

    public void DATA(ConstantString mvs) throws MaverickException {
        try {
            Class[] params = { char[].class, Integer.TYPE, Integer.TYPE };
            Method method = in.getClass().getMethod("unread", params);
            Object[] params2 = { mvs.toString().toCharArray(), new Integer(0), new Integer(mvs.length()) };
            method.invoke(in, params2);
            Object[] params3 = { ConstantString.LINE_SEPARATOR.toString().toCharArray(), new Integer(0), new Integer(ConstantString.LINE_SEPARATOR.length()) };
            method.invoke(in, params3);
        } catch (IllegalAccessException iae) {
            throw new MaverickException(0, iae);
        } catch (InvocationTargetException ivte) {
            throw new MaverickException(0, ivte);
        } catch (NoSuchMethodException nsme) {
            throw new MaverickException(0, nsme);
        }
    }

    public Reader getReader() {
        return in;
    }

    /**
     * Returns a single character from the input buffer
     * @param result the character
     * @param ordinal whether to return ordinal value rather than char itself
     */
    public MaverickString IN(MaverickString result, boolean ordinal, MaverickString status) throws MaverickException {
        result.clear();
        try {
            boolean icanon = (pty.getAttribute(Termios.ICANON) != 0);
            boolean echo = (pty.getAttribute(Termios.ECHO) != 0);
            if (icanon) {
                pty.setAttribute(Termios.ICANON, 0);
            }
            if (echo) {
                pty.setAttribute(Termios.ECHO, 0);
            }
            int ch = in.read();
            if (ordinal) {
                result.set(ch);
            } else {
                result.set((char) ch);
            }
            if (echo) {
                pty.setAttribute(Termios.ECHO, 1);
            }
            if (icanon) {
                pty.setAttribute(Termios.ICANON, 1);
            }
            return result;
        } catch (java.io.IOException e) {
            throw new MaverickException(0, e);
        }
    }

    /**
     * @param result input read from user
     * @param length max length to input, 0 for unlimited, &lt; for checking
     *          typeahead buffer.
     * @param processLine whether to process until end of line
     * @param newline output newline
     * @return input read from user
     */
    public ConstantString INPUT(MaverickString result, ConstantString length, boolean processLine, boolean newline, MaverickString status) throws MaverickException {
        result.clear();
        try {
            boolean icanon = (pty.getAttribute(Termios.ICANON) != 0);
            boolean echo = (pty.getAttribute(Termios.ECHO) != 0);
            boolean localEcho = false;
            int max = length.intValue();
            if (max < 0) {
                if (in.ready()) {
                    result.set(1);
                    return ConstantString.RETURN_SUCCESS;
                } else {
                    result.set(0);
                    return ConstantString.RETURN_ELSE;
                }
            } else if (max > 0 || !newline) {
                if (icanon) {
                    pty.setAttribute(Termios.ICANON, 0);
                }
                if (echo) {
                    pty.setAttribute(Termios.ECHO, 0);
                }
                localEcho = true;
            }
            PrintChannel out = session.getChannel(Session.SCREEN_CHANNEL);
            ConstantString prompt = session.getPrompt();
            boolean showPrompt = (prompt != null && prompt.length() > 0);
            if (showPrompt && !in.ready()) {
                out.PRINT(prompt.charAt(0), status);
                showPrompt = false;
            }
            int ch;
            int erase = pty.getAttribute(Termios.VERASE);
            while ((processLine || (max > 0 && result.length() < max)) && ((ch = in.read()) != -1 && ch != Termios.CR && ch != Termios.LF)) {
                if (ch == erase) {
                    if (result.length() > 0) {
                        result.setLength(result.length() - 1);
                        if (localEcho) {
                            out.PRINT(erase, status);
                            out.PRINT(' ', status);
                            out.PRINT(erase, status);
                        }
                    } else if (localEcho) {
                        out.PRINT(Termios.BELL, status);
                    }
                } else if (max > 0 && result.length() >= max) {
                    if (localEcho) {
                        out.PRINT(Termios.BELL, status);
                    }
                } else {
                    result.append((char) ch);
                    if (localEcho) {
                        out.PRINT(ch, status);
                    }
                }
                if (showPrompt && !in.ready()) {
                    out.PRINT(prompt.charAt(0), status);
                    showPrompt = false;
                }
            }
            if (localEcho && newline) {
                out.PRINT(ConstantString.EMPTY, true, status);
            }
            if (localEcho) {
                if (icanon) {
                    pty.setAttribute(Termios.ICANON, 1);
                }
                if (echo) {
                    pty.setAttribute(Termios.ECHO, 1);
                }
            }
            return ConstantString.RETURN_SUCCESS;
        } catch (IOException ioe) {
            throw new MaverickException(0, ioe);
        }
    }

    public boolean getLocalEcho() throws MaverickException {
        try {
            return (pty.getAttribute(Termios.ECHO) != 0);
        } catch (IOException ioe) {
            throw new MaverickException(0, ioe);
        }
    }

    public void setLocalEcho(boolean localEcho) {
        pty.setAttribute(Termios.ECHO, (localEcho) ? 1 : 0);
    }
}

;
