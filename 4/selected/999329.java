package com.sshtools.tunnel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import com.sshtools.j2ssh.forwarding.ForwardingChannel;

public class ActiveChannelModel extends AbstractTableModel {

    private Vector active;

    public ActiveChannelModel() {
        active = new Vector();
    }

    public void addActiveChannel(ForwardingChannel channel) {
        synchronized (active) {
            int r = getRowCount();
            active.addElement(new ActiveChannelWrapper(channel));
            fireTableRowsInserted(r, r);
        }
    }

    public void removeActiveChannel(ForwardingChannel channel) {
        synchronized (active) {
            int idx = indexOf(channel);
            if (idx != -1) {
                active.removeElementAt(idx);
                fireTableRowsDeleted(idx, idx);
            }
        }
    }

    public synchronized void dataSent(ForwardingChannel channel) {
        final int i = indexOf(channel);
        synchronized (active) {
            try {
                if (i != -1) {
                    final ActiveChannelWrapper w = (ActiveChannelWrapper) active.elementAt(i);
                    if (w.getSentTimer() == null) {
                        w.setSentTimer(new javax.swing.Timer(500, new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                w.setSending(false);
                                fireTableRowsUpdated(i, i);
                            }
                        }));
                        w.getSentTimer().setRepeats(false);
                    }
                    if (!w.getSentTimer().isRunning()) {
                        w.setSending(true);
                        fireTableRowsUpdated(i, i);
                        w.getSentTimer().start();
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
        }
    }

    public synchronized void dataReceived(ForwardingChannel channel) {
        final int i = indexOf(channel);
        synchronized (active) {
            try {
                if (i != -1) {
                    final ActiveChannelWrapper w = (ActiveChannelWrapper) active.elementAt(i);
                    if (w.getReceivedTimer() == null) {
                        w.setReceivedTimer(new javax.swing.Timer(500, new ActionListener() {

                            public void actionPerformed(ActionEvent evt) {
                                w.setReceiving(false);
                                fireTableRowsUpdated(i, i);
                            }
                        }));
                        w.getReceivedTimer().setRepeats(false);
                    }
                    if (!w.getReceivedTimer().isRunning()) {
                        w.setReceiving(true);
                        fireTableRowsUpdated(i, i);
                        w.getReceivedTimer().start();
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
        }
    }

    public void updateActiveChannel(ForwardingChannel channel) {
        synchronized (active) {
            int idx = indexOf(channel);
            if (idx != -1) {
                fireTableRowsUpdated(idx, idx);
            }
        }
    }

    public Class getColumnClass(int c) {
        switch(c) {
            case 0:
                return Boolean.class;
            case 1:
                return Boolean.class;
            case 2:
                return String.class;
            default:
                return String.class;
        }
    }

    public String getColumnName(int c) {
        switch(c) {
            case 0:
                return "SD";
            case 1:
                return "RD";
            case 2:
                return "Name";
            default:
                return "Host";
        }
    }

    public int getColumnCount() {
        return 4;
    }

    public int getRowCount() {
        synchronized (active) {
            return active.size();
        }
    }

    public void clear() {
        synchronized (active) {
            active.removeAllElements();
        }
    }

    public Iterator activeChannels() {
        return active.iterator();
    }

    public Object getValueAt(int r, int c) {
        synchronized (active) {
            ActiveChannelWrapper w = null;
            try {
                w = (ActiveChannelWrapper) active.elementAt(r);
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
            switch(c) {
                case 0:
                    if (w != null) {
                        return new Boolean(w.isSending());
                    } else {
                        return new Boolean(false);
                    }
                case 1:
                    if (w != null) {
                        return new Boolean(w.isReceiving());
                    } else {
                        return new Boolean(false);
                    }
                case 2:
                    if (w != null) {
                        return new String(w.getChannel().getName());
                    } else {
                        return new Long(-1);
                    }
                default:
                    if (w != null) {
                        StringBuffer sb = new StringBuffer(w.getChannel().getOriginatingHost());
                        sb.append(":");
                        sb.append(w.getChannel().getOriginatingPort());
                        return sb.toString();
                    } else {
                        return "Removed";
                    }
            }
        }
    }

    private int indexOf(ForwardingChannel channel) {
        synchronized (active) {
            int j = active.size();
            try {
                for (int i = 0; i < j; i++) {
                    ActiveChannelWrapper a = (ActiveChannelWrapper) active.elementAt(i);
                    if (a.getChannel() == channel) {
                        return i;
                    }
                }
                return -1;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return -1;
            }
        }
    }

    private ActiveChannelWrapper findWrapper(ForwardingChannel channel) {
        synchronized (active) {
            int j = active.size();
            for (int i = 0; i < j; i++) {
                try {
                    ActiveChannelWrapper a = (ActiveChannelWrapper) active.elementAt(i);
                    if (a.getChannel() == channel) {
                        return a;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    return null;
                }
            }
        }
        return null;
    }
}
