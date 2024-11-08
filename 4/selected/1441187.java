package irc.ampersand;

import irc.ampersand.event.EventChecker;
import irc.ampersand.event.EventWaiter;
import irc.ampersand.event.EventType;
import irc.ampersand.util.ReplyCode;
import java.util.List;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IRCConnection {

    private final List<EventChecker> listeners;

    private final List<EventWaiter> waiters;

    private final BlockingQueue<String> incoming;

    private IncomingThread inputThread;

    private OutgoingThread outputThread;

    private ParserThread parser;

    private Socket sock;

    private boolean connected;

    private String nick;

    public IRCConnection(String nick) {
        this.listeners = new CopyOnWriteArrayList<EventChecker>();
        this.waiters = new CopyOnWriteArrayList<EventWaiter>();
        this.incoming = new LinkedBlockingQueue<String>();
        this.nick = nick;
    }

    public void send(IRCCommand command) {
        if (connected) {
            if (command.isType(IRCCommand.PRIVMSG) || command.isType(IRCCommand.NOTICE)) outputThread.send(command.toString()); else outputThread.sendImmediately(command.toString());
        }
    }

    public void send(String target, String message) {
        send(IRCCommand.message(target, message));
    }

    public void sendBlock(String target, String[] messages) {
        String[] block = new String[messages.length];
        for (int i = 0, length = messages.length; i < length; i++) block[i] = IRCCommand.message(target, messages[i]).toString();
        outputThread.sendBlock(block);
    }

    public void sendImmediately(IRCCommand command) {
        outputThread.sendImmediately(command.toString());
    }

    public void clear() {
        outputThread.clear();
    }

    public void sendImmediately(String target, String message) {
        send(IRCCommand.message(target, message));
    }

    public void addTypeListener(IRCListener listener, final Set<EventType> types) {
        listeners.add(new EventChecker(listener) {

            public boolean check(IRCEvent e) {
                return e.isType(types);
            }
        });
    }

    public void addRegexListener(IRCListener listener, final String regex) {
        listeners.add(new EventChecker(listener) {

            public boolean check(IRCEvent e) {
                return e.message != null && Pattern.matches(regex, e.message);
            }
        });
    }

    public void removeListener(IRCListener listener) {
        listeners.remove(listener);
    }

    public IRCEvent waitForType(final Set<EventType> types, long millis) {
        EventWaiter waiter = new EventWaiter() {

            public boolean check(IRCEvent e) {
                return e.isType(types);
            }
        };
        waiters.add(waiter);
        try {
            return waiter.await(millis);
        } catch (Exception ex) {
            return null;
        }
    }

    public IRCEvent waitForRegex(final String regex, long millis) throws InterruptedException {
        EventWaiter waiter = new EventWaiter() {

            public boolean check(IRCEvent e) {
                return e.message != null && Pattern.matches(regex, e.message);
            }
        };
        waiters.add(waiter);
        try {
            IRCEvent e = waiter.await(millis);
            return e;
        } finally {
            waiters.remove(waiter);
        }
    }

    public boolean identify(String password) {
        EventWaiter waiter = new EventWaiter() {

            public boolean check(IRCEvent e) {
                return e.isType(EventType.PRIV_NOTICE) && e.source.equalsIgnoreCase("NickServ") && (e.message.equals("You have already identified") || e.message.startsWith("Password accepted") || e.message.equals("Password Incorrect"));
            }
        };
        waiters.add(waiter);
        outputThread.sendImmediately("NICKSERV IDENTIFY " + password);
        try {
            IRCEvent e = waiter.await(10 * 1000);
            if (e.message.equals("Password Incorrect")) return false; else return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean join(String channel) {
        send(IRCCommand.join(channel));
        return true;
    }

    public void connect(String network) {
        connect(network, 6667);
    }

    public void connect(String network, int port) {
        try {
            this.sock = new Socket(network, port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            IRCEvent result;
            EventWaiter waiter = new EventWaiter() {

                public boolean check(IRCEvent e) {
                    return e.isType(EventType.REPLY) && (e.replyCode == ReplyCode.RPL_MYINFO || e.replyCode == ReplyCode.ERR_NICKNAMEINUSE);
                }
            };
            waiters.add(waiter);
            this.inputThread = new IncomingThread(incoming, reader);
            this.outputThread = new OutgoingThread(writer);
            this.parser = new ParserThread();
            outputThread.start();
            inputThread.start();
            parser.start();
            outputThread.sendImmediately(IRCCommand.nick(this.nick).toString());
            outputThread.sendImmediately(IRCCommand.user("person", "Gracenotes' bot", 0).toString());
            try {
                result = waiter.await(60 * 1000);
            } catch (Exception ex) {
                return;
            }
            if (result.replyCode == ReplyCode.ERR_NICKNAMEINUSE) throw new RuntimeException("Nick in use");
            connected = true;
        } catch (IOException ex) {
        }
    }

    private class ParserThread extends Thread {

        public ParserThread() {
            super("ParserThread");
        }

        public void run() {
            try {
                while (true) {
                    String line = incoming.take();
                    IRCEvent event = new IRCEvent(line);
                    if (event.type == EventType.PING) outputThread.sendImmediately("PONG :" + event.message);
                    if (event.type == EventType.CTCP_VERSION) outputThread.sendImmediately("NOTICE " + event.source + " :VERSION Java");
                    System.out.println(event.raw);
                    for (final EventChecker checker : listeners) checker.deliverIfValid(event);
                    for (final EventWaiter waiter : waiters) waiter.signalIfValid(event);
                }
            } catch (InterruptedException ex) {
            }
        }

        public void interrupt() {
            incoming.clear();
            super.interrupt();
        }
    }
}

class IncomingThread extends Thread {

    private final BlockingQueue<String> queue;

    private final BufferedReader reader;

    public IncomingThread(BlockingQueue<String> queue, BufferedReader reader) {
        super();
        this.queue = queue;
        this.reader = reader;
        setDaemon(true);
    }

    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) queue.put(line);
        } catch (IOException ex) {
        } catch (InterruptedException ex) {
        }
    }
}

class OutgoingThread extends Thread {

    private volatile long defaultDelay;

    private final BlockingQueue<DelayedMessage> queue;

    private final BufferedWriter writer;

    private final Object addingLock;

    public OutgoingThread(BufferedWriter writer) {
        super();
        this.queue = new LinkedBlockingQueue<DelayedMessage>();
        this.writer = writer;
        this.defaultDelay = 1000;
        this.addingLock = new Object();
        setDaemon(true);
    }

    public void run() {
        while (true) {
            try {
                DelayedMessage message = queue.take();
                sendToServer(message.get());
                if (message.delay() > 0) Thread.sleep(message.delay());
            } catch (Exception ex) {
                break;
            }
        }
    }

    public void interrupt() {
        queue.clear();
        super.interrupt();
    }

    public void clear() {
        queue.clear();
    }

    public void setDefaultDelay(long delay) {
        defaultDelay = delay;
    }

    public void send(String message, long delay) {
        synchronized (addingLock) {
            try {
                queue.put(new DelayedMessage(message, delay));
            } catch (InterruptedException ex) {
            }
        }
    }

    public void send(String message) {
        synchronized (addingLock) {
            try {
                queue.put(new DelayedMessage(message, defaultDelay));
            } catch (InterruptedException ex) {
            }
        }
    }

    public void sendBlock(String[] messages, long delay) {
        synchronized (addingLock) {
            try {
                for (String message : messages) queue.put(new DelayedMessage(message, delay));
            } catch (InterruptedException ex) {
            }
        }
    }

    public void sendBlock(String[] messages) {
        this.sendBlock(messages, defaultDelay);
    }

    public void sendImmediately(String message) {
        try {
            sendToServer(message);
        } catch (IOException ex) {
        }
    }

    private synchronized void sendToServer(String line) throws IOException {
        if (line.length() > 510) line = line.substring(0, 510);
        writer.write(line + "\r\n");
        writer.flush();
        System.out.println(line);
    }

    private class DelayedMessage {

        private final String message;

        private final long delay;

        public DelayedMessage(String message, long delay) {
            this.message = message;
            this.delay = delay;
        }

        public DelayedMessage(String message) {
            this.message = message;
            this.delay = 0;
        }

        public long delay() {
            return delay;
        }

        public String get() throws InterruptedException {
            return message;
        }
    }
}
