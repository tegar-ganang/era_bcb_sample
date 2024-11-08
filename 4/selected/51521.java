package jtelnet;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import JiBoot.main_fen;
import JiBoot.win_flash;
import java.lang.Exception;

@SuppressWarnings("serial")
public class jTelnet extends Canvas implements Runnable {

    static boolean debug = false;

    static boolean interact = false;

    static Object to_be_notified = null;

    static boolean to_sync = false;

    static boolean conf_login = false;

    private boolean lock = false;

    String to_search = null;

    static boolean synced = false;

    String temp = "";

    public int last_error = 0;

    static final int columns = 80, lines = 25;

    static int telnetPort = 23;

    int PTWidth, PTHeight, charOffset, lineOffset, topOffset;

    char screen[][];

    Color screenfg[][];

    Color screenbg[][];

    boolean redraw = true;

    boolean lineRedraw[];

    Color bgcolor = Color.white;

    Color fgcolor = Color.black;

    Color d_bgcolor = Color.white;

    Color d_fgcolor = Color.black;

    Font PTFont;

    public boolean logged = false;

    public boolean ready = false;

    boolean wrap = true;

    int xloc = 0, yloc = 0;

    InputStream sIn;

    OutputStream sOut;

    Thread receive;

    int telnetState = 0;

    boolean option[];

    main_fen main_win = null;

    String my_login;

    String my_pass;

    void openConnection(String host, int port) throws IOException {
        Socket sock = new Socket(host, port);
        sOut = sock.getOutputStream();
        sIn = sock.getInputStream();
        option = new boolean[256];
        option[1] = true;
        receive = new Thread(this);
        receive.start();
    }

    void initProcess() {
    }

    void process(byte c) {
        if (c > 27) normal(c); else switch(c) {
            case 0:
                break;
            case 7:
                break;
            case 8:
                xloc--;
                if (xloc < 0) {
                    yloc--;
                    xloc = columns - 1;
                    if (yloc < 0) yloc = 0;
                }
                break;
            case 9:
                int n = (8 - xloc % 8);
                for (int j = 0; j < n; j++) normal((byte) 32);
                break;
            case 10:
                yloc++;
                xloc = 0;
                scrValid();
                lineRedraw[yloc] = true;
                break;
            case 11:
                break;
            case 12:
                break;
            case 13:
                break;
            default:
                normal(c);
        }
    }

    void scrValid() {
        if (xloc >= columns) if (wrap) {
            xloc = 0;
            yloc++;
        } else xloc = columns - 1;
        if (yloc >= lines) {
            for (int j = 0; j < lines - 1; j++) {
                screen[j] = screen[j + 1];
                screenfg[j] = screenfg[j + 1];
                screenbg[j] = screenbg[j + 1];
                lineRedraw[j] = true;
            }
            screen[lines - 1] = new char[columns];
            screenfg[lines - 1] = new Color[columns];
            screenbg[lines - 1] = new Color[columns];
            for (int j = 0; j < columns; j++) {
                screen[lines - 1][j] = ' ';
                screenfg[lines - 1][j] = fgcolor;
                screenbg[lines - 1][j] = bgcolor;
            }
            lineRedraw[lines - 1] = true;
            yloc--;
        }
    }

    void normal(StringBuffer s) {
        if (debug) System.out.print("normal: '");
        for (int i = 0; i < s.length(); i++) {
            normal((byte) s.charAt(i));
            if (debug) System.out.print(s.charAt(i));
        }
        if (debug) System.out.println("'");
    }

    void normal(byte c) {
        if (debug) System.out.print((char) c);
        lineRedraw[yloc] = true;
        screen[yloc][xloc] = (char) c;
        screenfg[yloc][xloc] = fgcolor;
        screenbg[yloc][xloc] = bgcolor;
        xloc++;
        scrValid();
    }

    static final byte WILL = (byte) 251;

    static final byte WONT = (byte) 252;

    static final byte DO = (byte) 253;

    static final byte DONT = (byte) 254;

    void sendcmd(byte command, byte option) throws IOException {
        byte msg[] = { (byte) 255, command, option };
        sOut.write(msg);
    }

    void write(byte buf[], int off, int read) throws IOException {
        int i;
        for (i = off; i < read; ) {
            if (telnetState == 0) {
                if (buf[i] == (byte) 0xFF) {
                    telnetState = 1;
                    i++;
                } else process(buf[i++]);
            } else if (telnetState == 1) {
                switch(buf[i]) {
                    case (byte) 240:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 241:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 242:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 243:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 244:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 245:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 246:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 247:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 248:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 249:
                        telnetState = 0;
                        i++;
                        break;
                    case (byte) 250:
                        telnetState = 10;
                        i++;
                        break;
                    case (byte) 251:
                        telnetState = 11;
                        i++;
                        break;
                    case (byte) 252:
                        telnetState = 12;
                        i++;
                        break;
                    case (byte) 253:
                        telnetState = 13;
                        i++;
                        break;
                    case (byte) 254:
                        telnetState = 14;
                        i++;
                        break;
                    case (byte) 255:
                        telnetState = 0;
                        process(buf[i++]);
                        break;
                    default:
                        telnetState = 0;
                        i++;
                }
            } else switch(telnetState) {
                case 10:
                    telnetState = 0;
                    i++;
                    break;
                case 11:
                    telnetState = 0;
                    if (option[buf[i]] == false) {
                        sendcmd(DONT, buf[i]);
                        option[buf[i]] = true;
                    }
                    i++;
                    break;
                case 12:
                    telnetState = 0;
                    i++;
                    break;
                case 13:
                    telnetState = 0;
                    if (option[buf[i]] == false) {
                        sendcmd(WONT, buf[i]);
                        option[buf[i]] = true;
                    }
                    i++;
                    break;
                case 14:
                    telnetState = 0;
                    i++;
                    break;
            }
        }
        repaint();
    }

    public void run() {
        int read;
        byte[] str_login = (my_login + "\n").getBytes();
        byte[] str_pass = (my_pass + "\n").getBytes();
        boolean login_prompted = false;
        byte buf[] = new byte[1024 * 4];
        if (interact) this.requestFocus();
        synced = true;
        while (true) {
            try {
                receive.sleep(100);
                if ((read = sIn.read(buf)) >= 0) {
                    write(buf, 0, read);
                    if (conf_login) {
                        if (read > 2) {
                            conf_login = false;
                            if (Search_word(buf, "incorrect", read) == -1) {
                                logged = true;
                                if (main_win != null) main_win.telnet_thread_logged = true;
                            } else {
                                logged = false;
                                if (main_win != null) main_win.telnet_thread_logged = false;
                                try {
                                    throw new Exception("login failed");
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                    if (!logged) {
                        if (!login_prompted) {
                            if (Search_word(buf, "login:", read) >= 0) {
                                login_prompted = true;
                                sOut.write(str_login);
                            }
                        } else {
                            if (Search_word(buf, "assword:", read) >= 0) {
                                sOut.write(str_pass);
                                if (main_win != null) main_win.telnet_thread_logged = false;
                                conf_login = true;
                                synced = true;
                            }
                        }
                    } else {
                        if (!ready) {
                            if (interact) {
                                sOut.write("clear\n".getBytes());
                                this.requestFocus();
                            }
                            ready = true;
                        }
                        if (!synced && !interact) {
                            if ((to_search != null) && (!to_search.isEmpty())) {
                                if (Search_word(buf, to_search, read) >= 0) {
                                    lock = false;
                                    synced = true;
                                    if (to_be_notified != null) {
                                        ActionEvent actionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "telnet_synced");
                                        String target = to_be_notified.getClass().toString();
                                        if (target.contains("win_flash")) {
                                            win_flash qualified = (win_flash) to_be_notified;
                                            qualified.actionPerformed(actionEvent);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (read == -1) {
                    if (interact) this.getParent().setVisible(false);
                    break;
                }
            } catch (IOException e) {
                System.out.println("Cought Something(IO)");
            } catch (InterruptedException e) {
                System.out.println("Cought Something(Interrupted)");
            }
        }
    }

    private int Search_word(byte[] buff, String searched_word, int buf_len) {
        int pointer = 0;
        byte[] tmp_buff = buff.clone();
        int ptcur = 0;
        temp = "";
        for (ptcur = 0; ptcur < buf_len; ptcur++) {
            if (tmp_buff[ptcur] < 32 || tmp_buff[ptcur] > 127) if (tmp_buff[ptcur] != 10) tmp_buff[ptcur] = 32;
            temp = temp + ((char) tmp_buff[ptcur]);
        }
        if (!interact && !synced && logged) if (main_win.buff_telnet == null) main_win.buff_telnet = temp; else main_win.buff_telnet += temp;
        pointer = temp.indexOf(searched_word);
        if (pointer >= 0) {
            return (pointer);
        }
        return (-1);
    }

    public jTelnet(String host) throws IOException {
        this(null, host, telnetPort, null, null, null);
        this.to_be_notified = null;
    }

    public jTelnet(String host, int port) throws IOException {
        this(null, host, port, null, null, null);
        this.to_be_notified = null;
    }

    public jTelnet(String host, int port, String login) throws IOException {
        this(null, host, port, login, null, null);
        this.to_be_notified = null;
    }

    public jTelnet(main_fen caller, String host, int port, String login, String pass, Object to_notify) throws IOException {
        super();
        this.to_be_notified = to_notify;
        main_win = caller;
        my_login = login;
        my_pass = pass;
        interact = false;
        screen = new char[lines][columns];
        lineRedraw = new boolean[lines];
        screenfg = new Color[lines][columns];
        screenbg = new Color[lines][columns];
        for (int i = 0; i < lines; i++) {
            lineRedraw[i] = true;
            for (int j = 0; j < columns; j++) {
                screenfg[i][j] = fgcolor;
                screenbg[i][j] = bgcolor;
            }
        }
        initProcess();
        openConnection(host, port);
        PTFont = new Font("Courier", Font.PLAIN, 12);
        FontMetrics fm = getFontMetrics(PTFont);
        topOffset = fm.getMaxAscent() + 1;
        charOffset = fm.stringWidth("X");
        lineOffset = fm.getHeight();
        PTWidth = charOffset * columns;
        PTWidth = PTWidth + 5;
        PTHeight = lineOffset * lines + 4;
        super.resize(PTWidth + 6, PTHeight + 6);
    }

    public Boolean send_line(Object client, String to_send, String wait_for) {
        to_be_notified = client;
        this.send_line(to_send, wait_for);
        return true;
    }

    public Boolean send_line(String to_send, String wait_for) {
        if (logged) {
            byte[] str_line = to_send.getBytes();
            if ((wait_for != null) && (!wait_for.isEmpty())) {
                synced = false;
                to_search = wait_for;
                lock = true;
            } else {
                to_be_notified = null;
                synced = true;
                to_search = null;
                lock = false;
            }
            try {
                sOut.write(str_line);
            } catch (IOException e) {
                System.out.println("jtelnet : IO Exception sending command line");
                last_error = 2;
                return false;
            }
            if (to_be_notified != null) {
                last_error = 0;
                return true;
            }
            while (!synced) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException t) {
                }
            }
            last_error = 0;
            return true;
        }
        System.out.printf("jtelnet : not logged ! \n");
        last_error = 1;
        return false;
    }

    public boolean gotFocus(Event evt, Object what) {
        return (super.gotFocus(evt, what));
    }

    public boolean lostFocus(Event evt, Object what) {
        return (super.lostFocus(evt, what));
    }

    public boolean keyDown(Event evt, int key) {
        return (super.keyDown(evt, key));
    }

    public boolean keyUp(Event evt, int key) {
        byte msg[] = { (byte) key };
        try {
            sOut.write(msg);
        } catch (Exception e) {
        }
        ;
        return (super.keyUp(evt, key));
    }

    public void update(Graphics g) {
        int j, beg;
        boolean ok;
        g.setFont(PTFont);
        for (int i = 0; i < lines; i++) {
            if (lineRedraw[i] == true) {
                lineRedraw[i] = false;
                j = 0;
                Color fg, bg;
                while (j < columns - 1) {
                    fg = screenfg[i][j];
                    bg = screenbg[i][j];
                    beg = j;
                    ok = true;
                    while (++j < columns && ok) if (fg != screenfg[i][j] || bg != screenbg[i][j]) ok = false;
                    if (ok == false) j--;
                    g.setColor(bg);
                    g.fillRect(3 + beg * charOffset, 2 + i * lineOffset, charOffset * (j - beg), lineOffset);
                    g.setColor(fg);
                    g.drawChars(screen[i], beg, j - beg, 3 + beg * charOffset, topOffset + i * lineOffset);
                }
            }
        }
        g.setColor(new Color(screenbg[yloc][xloc].getRGB() ^ 0xFFFFFF));
        g.fillRect(3 + xloc * charOffset, 2 + yloc * lineOffset, charOffset, lineOffset);
        g.setColor(new Color(screenfg[yloc][xloc].getRGB() ^ 0xFFFFFF));
        g.drawChars(screen[yloc], xloc, 1, 3 + xloc * charOffset, topOffset + yloc * lineOffset);
        lineRedraw[yloc] = true;
    }

    public void paint(Graphics g) {
        for (int i = 0; i < lines; i++) lineRedraw[i] = true;
        g.setColor(d_bgcolor);
        g.fillRect(1, 1, PTWidth - 1, PTHeight - 1);
        g.setColor(d_fgcolor);
        g.drawRect(0, 0, PTWidth, PTHeight);
        update(g);
    }
}
