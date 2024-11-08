import java.util.Hashtable;

public class PracticeMode implements UserListener {

    private int infinity = 1000000;

    private RowDispatcher rd;

    private MistakeDispatcher md;

    private CallDispatcher cd;

    private Method currentmethod;

    private int[] calls;

    private int callno = 0;

    private String[] methods;

    private int methodno = 0;

    private Hashtable methodtable = new Hashtable();

    private int nextcall;

    private int nextmethod = 0;

    private int ringer;

    private int lastbell;

    private int[] thischange;

    private boolean going = true;

    public PracticeMode(RowDispatcher rd, MistakeDispatcher md, CallDispatcher cd) throws Exception {
        this.rd = rd;
        this.md = md;
        this.cd = cd;
        thischange = new int[] { 1, 2, 3, 4, 5, 6 };
        ringer = 1;
        lastbell = ringer;
        Method plain = new Method("Plain", "&-16-16-16,12");
        plain.set(new int[] { 1, 4 }, new int[] { 1, 2, 3, 4 }, 12, 12);
        Method little = new Method("Little", "&-16-14,12");
        little.set(new int[] { 1, 4 }, new int[] { 1, 2, 3, 4 }, 8, 8);
        methodtable.put("P", plain);
        methodtable.put("L", little);
        methods = new String[] { "P", "L", "P", "P", "L", "P" };
        calls = new int[] { 0, 0, 2, 0, 0, 2 };
        change();
    }

    public PracticeMode(RowDispatcher rd, MistakeDispatcher md, CallDispatcher cd, int[] startchange, int ringer, Hashtable methodtable, String[] methods, int[] calls) {
        this.rd = rd;
        this.md = md;
        this.cd = cd;
        thischange = startchange;
        this.ringer = ringer;
        this.methodtable = methodtable;
        this.methods = methods;
        this.calls = calls;
        lastbell = ringer;
    }

    private void call() {
        int[] change = null;
        if (calls[callno] == 1) {
            change = currentmethod.getBob();
        }
        if (calls[callno] == 2) {
            change = currentmethod.getSingle();
        }
        int j = 0;
        for (int i = 0; i < thischange.length; ) {
            if (j < change.length && i == change[j] - 1) {
                i++;
                j++;
            } else {
                int temp = thischange[i];
                thischange[i] = thischange[i + 1];
                thischange[i + 1] = temp;
                i += 2;
                j += 1;
            }
        }
        callno++;
        nextmethod--;
        nextcall = currentmethod.callFreq();
    }

    private void beginMethod() {
        currentmethod = (Method) methodtable.get(methods[methodno]);
        nextmethod = currentmethod.length();
        nextcall = currentmethod.firstCall() - 1;
        methodno++;
    }

    private void change() {
        if (!going) return;
        if (methodno != methods.length && nextmethod == 0) {
            beginMethod();
        }
        if (methodno == methods.length && nextmethod == 0) {
            cd.displayCall("Stand");
            going = false;
            return;
        }
        boolean callexecuted = false;
        if (nextcall == 0) {
            if (callno != calls.length) {
                if (calls[callno] == 0) {
                    callno++;
                    nextcall = currentmethod.callFreq() + 1;
                } else {
                    call();
                    callexecuted = true;
                }
            }
        }
        if (nextmethod == 2) {
            Method a = null;
            try {
                a = (Method) methodtable.get(methods[methodno]);
                cd.displayCall(a.getName());
            } catch (ArrayIndexOutOfBoundsException e) {
                cd.displayCall("That's all");
            }
        }
        if (nextcall == 2) {
            try {
                if (calls[callno] == 1) {
                    cd.displayCall("Bob");
                } else if (calls[callno] == 2) {
                    cd.displayCall("Single");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        if (callexecuted) return;
        int[] change = currentmethod.changeAt(currentmethod.length() - nextmethod);
        int j = 0;
        int[] newchange = new int[thischange.length];
        for (int i = 0; i < change.length; i++) {
            newchange[i] = thischange[change[i]];
        }
        thischange = newchange;
        nextmethod--;
        nextcall--;
    }

    private boolean isRounds() {
        for (int i = 0; i < thischange.length; i++) {
            if (thischange[i] != i + 1) {
                return false;
            }
        }
        return true;
    }

    private int find(int bell) {
        for (int i = 0; i < thischange.length; i++) {
            if (thischange[i] == bell) {
                return i + 1;
            }
        }
        return -1;
    }

    public void up() {
        int thisbell = find(ringer);
        if (thisbell == lastbell + 1) {
            rd.displayRow(thischange);
            lastbell = thisbell;
            change();
        } else {
            md.displayMistake();
        }
    }

    public void down() {
        int thisbell = find(ringer);
        if (thisbell == lastbell - 1) {
            rd.displayRow(thischange);
            lastbell = thisbell;
            change();
        } else {
            md.displayMistake();
        }
    }

    public void place() {
        int thisbell = find(ringer);
        if (thisbell == lastbell) {
            rd.displayRow(thischange);
            lastbell = thisbell;
            change();
        } else {
            md.displayMistake();
        }
    }
}
