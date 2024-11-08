package com.quikj.server.framework;

import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class AceTimer extends AceThread implements AceCompareMessageInterface {

    private LinkedList timerQueue = new LinkedList();

    public boolean killThread = false;

    public int nextTimerId = 0;

    public static AceTimer instance = null;

    public AceTimer() {
        super("AceTimer", true);
        instance = this;
    }

    public static AceTimer Instance() {
        return instance;
    }

    private int addTimer(long abs_time, AceThread cthread, long user_parm) {
        Thread calling_thread;
        if (cthread != null) {
            calling_thread = cthread;
        } else {
            calling_thread = Thread.currentThread();
        }
        if ((calling_thread instanceof AceThread) == false) {
            writeErrorMessage("This method is not being called from an object which is a sub-class of type AceThread", null);
            return -1;
        }
        int timer_id = nextTimerId++;
        AceTimerMessage msg = new AceTimerMessage(abs_time, (AceThread) calling_thread, timer_id, user_parm);
        if (insertElementInTimerQueue(msg) == true) {
            this.interrupt();
        } else {
            timer_id = -1;
        }
        return timer_id;
    }

    public void cancelAllTimers() {
        cancelAllTimers(null);
    }

    public void cancelAllTimers(AceThread cthread) {
        if (cthread == null) {
            cthread = (AceThread) Thread.currentThread();
        }
        synchronized (timerQueue) {
            ListIterator iter = timerQueue.listIterator(0);
            while (iter.hasNext() == true) {
                AceTimerMessage msg = (AceTimerMessage) iter.next();
                if (msg.getRequestingThread() == cthread) {
                    iter.remove();
                }
            }
        }
    }

    public boolean cancelTimer(int timer_id) {
        return cancelTimer(timer_id, null);
    }

    public boolean cancelTimer(int timer_id, AceThread cthread) {
        boolean ret = false;
        synchronized (timerQueue) {
            ListIterator iter = timerQueue.listIterator(0);
            while (iter.hasNext() == true) {
                AceTimerMessage msg = (AceTimerMessage) iter.next();
                if (msg.getTimerId() == timer_id) {
                    ret = true;
                    iter.remove();
                    break;
                }
            }
            if (ret == false) {
                if (cthread == null) {
                    Thread cur_thr = Thread.currentThread();
                    if ((cur_thr instanceof AceThread) == false) {
                        writeErrorMessage("Element not found", null);
                        return ret;
                    }
                    cthread = (AceThread) cur_thr;
                }
                ret = cthread.removeMessage(new AceTimerMessage(0L, null, timer_id, 0L), this);
                if (ret == true) {
                    System.out.println("AceTimer.cancelTimer() -- Request to cancel timer id " + timer_id + " received - the timer message was already delivered to the requesting thread but not read from its queue, removed the message from queue");
                }
            }
        }
        if (ret == false) {
            writeErrorMessage("Element not found", null);
        }
        return ret;
    }

    public void dispose() {
        System.out.println(getName() + " - request received to dispose AceTimer.");
        killThread = true;
        this.interrupt();
        super.dispose();
    }

    private boolean insertElementInTimerQueue(AceTimerMessage msg) {
        long exp_time = msg.getExpiryTime();
        synchronized (timerQueue) {
            ListIterator iter = timerQueue.listIterator(0);
            try {
                while (true) {
                    AceTimerMessage queued_msg = (AceTimerMessage) iter.next();
                    if (exp_time <= queued_msg.getExpiryTime()) {
                        try {
                            Object prev = iter.previous();
                            iter.add(msg);
                            break;
                        } catch (NoSuchElementException ex1) {
                            timerQueue.addFirst(msg);
                            break;
                        }
                    }
                }
            } catch (NoSuchElementException ex2) {
                iter.add(msg);
            }
        }
        return true;
    }

    public void run() {
        while (true) {
            try {
                int interval = 0;
                if (timerQueue.size() == 0) {
                    interval = Integer.MAX_VALUE;
                } else {
                    interval = (int) (((AceTimerMessage) timerQueue.getFirst()).getExpiryTime() - (new Date()).getTime());
                    if (interval < 0) interval = 0;
                }
                if (killThread) {
                    break;
                }
                sleep(interval);
                if (killThread) {
                    break;
                }
                long cur_time = (new Date()).getTime();
                synchronized (timerQueue) {
                    ListIterator iter = timerQueue.listIterator(0);
                    while (iter.hasNext() == true) {
                        AceTimerMessage queued_msg = (AceTimerMessage) iter.next();
                        long exp_time = queued_msg.getExpiryTime();
                        if (exp_time <= cur_time) {
                            if (queued_msg.getRequestingThread().sendMessage(queued_msg) == false) {
                                System.err.println("AceTimer.run() -- could not send timer expiry message to thread " + queued_msg.getRequestingThread().getName() + ", timer id : " + queued_msg.getTimerId() + ", error : " + getErrorMessage());
                            }
                            iter.remove();
                        } else {
                            break;
                        }
                    }
                }
            } catch (InterruptedException ex) {
                if (killThread == false) {
                    continue;
                } else {
                    break;
                }
            }
        }
        System.out.println(getName() + " - AceTimer killed.");
    }

    public boolean same(AceMessageInterface obj1, AceMessageInterface obj2) {
        boolean ret = false;
        if (((obj1 instanceof AceTimerMessage) == true) && ((obj2 instanceof AceTimerMessage) == true)) {
            if (((AceTimerMessage) obj1).getTimerId() == ((AceTimerMessage) obj2).getTimerId()) {
                ret = true;
            }
        }
        return ret;
    }

    public int startTimer(Date abs_time, AceThread cthread, long user_parm) {
        return addTimer(abs_time.getTime(), cthread, user_parm);
    }

    public int startTimer(Date abs_time, long user_parm) {
        return addTimer(abs_time.getTime(), null, user_parm);
    }

    public int startTimer(long interval, AceThread cthread, long user_parm) {
        return addTimer((new Date()).getTime() + interval, cthread, user_parm);
    }

    public int startTimer(long interval, long user_parm) {
        return addTimer((new Date()).getTime() + interval, null, user_parm);
    }

    public AceMessageInterface waitTimer(int timerid) {
        Thread thr = Thread.currentThread();
        if ((thr instanceof AceThread) == false) {
            writeErrorMessage("This method is not being called from an object which is a sub-class of type AceThread", null);
            return null;
        }
        AceThread cthread = (AceThread) thr;
        while (true) {
            AceMessageInterface msg = cthread.waitMessage();
            if ((msg instanceof AceTimerMessage) == true) {
                if (((AceTimerMessage) msg).getTimerId() == timerid) {
                    return msg;
                }
            } else if ((msg instanceof AceSignalMessage) == true) {
                return msg;
            }
        }
    }
}
