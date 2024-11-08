package atm.jcsp;

import atm.spec.*;
import jcsp.lang.*;
import java.util.HashMap;
import java.util.logging.Logger;

public class Bank implements BankSpec, CSProcess {

    private final AltingChannelInput ready;

    private final AltingChannelInput[] debit;

    private final Channel[] ident, auth;

    private final ChannelOutput setup;

    private final ChannelOutput[] result;

    private int currentChannel;

    private ATMSpec[] machines;

    private Boolean[] answers;

    private HashMap accountlist = new HashMap();

    public void ready() {
        Logger.global.fine("bank received ready");
    }

    public BankSpec ident(ATMSpec tid) {
        machines[currentChannel] = tid;
        Logger.global.fine("bank received ident from " + tid);
        return this;
    }

    public void setup(PARSpec param) {
        Logger.global.fine("bank is sending out setup ");
        setup.write(param);
        Logger.global.finer("bank sending out setup done");
    }

    /** 
     * This methods sends the result of a debit request back. The result
     * Boolean is atken from the resultQ and send via the channel, that is
     * connected to the caller ATM that has initiated the debit request.   
     */
    public void result(ATMSpec addr, Boolean ok) {
        int chan = getMachineChannel(addr);
        result[chan].write(ok);
        answers[chan] = null;
    }

    /** 
     * Authorization is successful if the bank has an account with
     * the given ID and the PIN is coorect for that acount. 
     */
    public Boolean auth(IDSpec id, PINSpec pin) {
        Account account = getAccount(id);
        if (account == null) {
            Logger.global.finest("Bank no account for " + id);
            return Boolean.FALSE;
        }
        Logger.global.finest("Bank checking pin for account: " + account);
        return new Boolean(account.isCorrectPin((PIN) pin));
    }

    /** 
     * Withdraw the amount from the account and store a success answer to the
     * queue. The stored answer is false if either the account does not exist or
     * the amount is not available. In the latter case no modification will apply 
     * the to the account.
     */
    public void debit(ATMSpec from, IDSpec id, SUMSpec amt) {
        int index = getMachineChannel(from);
        Account account = getAccount(id);
        if (account == null || !account.available((Amount) amt)) {
            answers[index] = Boolean.FALSE;
        } else {
            answers[index] = new Boolean(account.withdraw((Amount) amt));
        }
    }

    public void run() {
        Logger.global.finer("Bank initializing ");
        Guard[] guards = new Guard[ident.length + 1];
        java.lang.System.arraycopy(ident, 0, guards, 0, ident.length);
        guards[ident.length] = ready;
        Alternative alt = new Alternative(guards);
        int choose = alt.select();
        while (choose != ident.length) {
            currentChannel = choose;
            ident[choose].write(ident((ATMSpec) ident[choose].read()));
            Logger.global.finer("Bank choosing");
            choose = alt.select();
            Logger.global.finest("Bank has chosen");
        }
        Logger.global.fine("Bank avaiting ready");
        ready.read();
        ready();
        setup(new Data(new java.util.Date().toString()));
        upAndRunning();
    }

    private void upAndRunning() {
        Logger.global.finer("Bank entered upAndRunning ");
        for (int i = 0; i < machines.length; i++) Logger.global.finest(machines[i].toString());
        Guard[] guards = new Guard[auth.length + debit.length];
        java.lang.System.arraycopy(auth, 0, guards, 0, auth.length);
        java.lang.System.arraycopy(debit, 0, guards, auth.length, debit.length);
        Alternative alt = new Alternative(guards);
        while (true) {
            if (hasRequests()) {
                Logger.global.finest("Bank has requests");
                int chan = getRequest();
                ATMSpec addr = machines[chan];
                Boolean ok = answers[chan];
                Logger.global.finest("Bank sends result");
                result(addr, ok);
            } else {
                Logger.global.finest("Bank reads");
                int chan = alt.select();
                if (chan < auth.length) {
                    Logger.global.fine("Bank read auth " + chan);
                    getAccount(new AccountID(16));
                    auth[chan].write(auth((IDSpec) auth[chan].read(), (PINSpec) auth[chan].read()));
                } else {
                    chan -= auth.length;
                    Logger.global.fine("Bank read debit " + chan);
                    debit((ATMSpec) debit[chan].read(), (IDSpec) debit[chan].read(), (SUMSpec) debit[chan].read());
                }
            }
        }
    }

    private boolean hasRequests() {
        return (getRequest() >= 0);
    }

    /**
     * Get the index of a request. The index is also used to identify the ATM
     * and the channel to communicate with/over.
     */
    private int getRequest() {
        for (int i = 0; i < answers.length; i++) {
            if (answers[i] != null) {
                return i;
            }
        }
        return -1;
    }

    /** 
     * Get the channel index that is used for communication with the 
     * given ATM.
     */
    private int getMachineChannel(ATMSpec machine) {
        for (int i = 0; i < machines.length; i++) {
            if (machines[i] == machine) {
                return i;
            }
        }
        return -1;
    }

    /** 
     * Lookup an account by its ID.
     */
    private Account getAccount(IDSpec id) {
        Account a = (Account) accountlist.get(id);
        return (Account) accountlist.get(id);
    }

    private void addAccount(Account account) {
        if (accountlist == null) {
            accountlist = new HashMap();
        }
        accountlist.put(account.getAccountID(), account);
    }

    public Bank(AltingChannelInput ready, AltingChannelInput[] debit, Channel[] ident, Channel[] auth, ChannelOutput setup, ChannelOutput[] result) {
        if (debit.length != ident.length || debit.length != auth.length || debit.length != result.length) {
            Logger.global.severe("Inconsistant bank configuration");
            throw new RuntimeException("Inconsistant configuration");
        }
        this.ready = ready;
        this.debit = debit;
        this.ident = ident;
        this.auth = auth;
        this.setup = setup;
        this.result = result;
        machines = new ATMSpec[debit.length];
        answers = new Boolean[debit.length];
        addAccount(new Account(0010, 2000, "pin1"));
        addAccount(new Account(0020, 1000, "pin2"));
        addAccount(new Account(0030, 2500, "pin3"));
    }
}
