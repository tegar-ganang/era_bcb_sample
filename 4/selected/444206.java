package com.headissue.asterisk.jtapi.gjtapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.Iterator;
import javax.telephony.CallEvent;
import javax.telephony.InvalidArgumentException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.ProviderUnavailableException;
import net.sourceforge.gjtapi.*;
import net.sourceforge.gjtapi.capabilities.Capabilities;
import net.sourceforge.gjtapi.raw.BasicJtapiTpi;
import net.sf.asterisk.manager.*;
import net.sf.asterisk.manager.event.*;
import net.sf.asterisk.manager.response.*;
import net.sf.asterisk.manager.action.*;

/**
 * GJTAPI raw provider for the Asterisk PBX  
 * 
 * @author jw
 * @version $Id: AsteriskProvider.java,v 1.7 2006/08/09 07:01:14 jwilke Exp $
 */
public class AsteriskProvider implements BasicJtapiTpi {

    DefaultAsteriskManager asteriskManager;

    DefaultManagerConnection managerConnection;

    TelephonyListener listener = null;

    Map id2call = new Hashtable();

    ProviderConfig config;

    Map addresses;

    String[] addressStrings;

    Map terminals;

    Map terminal2context;

    public void addListener(TelephonyListener l) {
        if (listener != null) {
            System.err.println("asterisktpi: duplicate listener addition!");
        }
        listener = l;
    }

    public void removeListener(TelephonyListener ro) {
        if (listener == null) {
            System.err.println("asterisktpi: remove listener, but no listener there!");
        }
        listener = null;
    }

    /**
   * Call answering is not supported by the asterisk tpi. In general this makes only
   * sense in first-party TAPI applications.
   */
    public void answerCall(CallId call, String address, String terminal) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("answering calls is not supported by asterisk");
    }

    public CallId createCall(CallId _callId, String _addr, String _terminal, String _destination) throws ResourceUnavailableException {
        AsteriskCall _call = (AsteriskCall) _callId;
        OriginateAction originateAction = new OriginateAction();
        originateAction.setChannel("local/" + _terminal + "@" + terminal2context.get(_terminal));
        originateAction.setContext(config.outgoingContext);
        originateAction.setAsync(Boolean.TRUE);
        originateAction.setCallerId(_addr);
        originateAction.setExten(_destination);
        originateAction.setPriority(new Integer(1));
        try {
            if (config.debug > 0) {
                System.out.println("* action: " + ToString.convert(originateAction));
            }
            ManagerResponse response = managerConnection.sendAction(originateAction);
            if (config.debug > 0) {
                System.out.println("* response: " + ToString.convert(response));
            }
            _call.uniqueId = response.getUniqueId();
            _call.terminal = _terminal;
            _call.address = _addr;
            _call.callerId = _destination;
            _call.originate = true;
            _call.calledAddress = _destination;
            id2call.put(_call.uniqueId, _call);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceUnavailableException(ResourceUnavailableException.UNKNOWN);
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new ResourceUnavailableException(ResourceUnavailableException.UNSPECIFIED_LIMIT_EXCEEDED);
        }
        return _callId;
    }

    public void release(String _address, CallId _callId) throws ResourceUnavailableException {
        AsteriskCall _call = (AsteriskCall) _callId;
        if (id2call.containsValue(_call)) {
            HangupAction ha = new HangupAction();
            ha.setChannel(_call.channel);
            try {
                managerConnection.sendAction(ha);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ResourceUnavailableException(ResourceUnavailableException.UNKNOWN);
            } catch (TimeoutException e) {
                e.printStackTrace();
                throw new ResourceUnavailableException(ResourceUnavailableException.UNSPECIFIED_LIMIT_EXCEEDED);
            }
        }
    }

    public Properties getCapabilities() {
        Properties capabilities = new Properties();
        capabilities.put(Capabilities.THROTTLE, "f");
        capabilities.put(Capabilities.MEDIA, "f");
        capabilities.put(Capabilities.ALL_MEDIA_TERMINALS, "f");
        capabilities.put(Capabilities.ALLOCATE_MEDIA, "f");
        return capabilities;
    }

    public void releaseCallId(CallId _callId) {
        AsteriskCall _call = (AsteriskCall) _callId;
        if (_call.uniqueId != null) {
            if (config.debug > 0) {
                System.err.println("releaseCallId called, uniqueId: " + _call.uniqueId);
            }
            id2call.remove(_call.uniqueId);
            if (_call.leg2 != null) {
                id2call.remove(_call.leg2.uniqueId);
            }
        }
    }

    public CallId reserveCallId(String address) {
        System.err.println("reserveCallId called");
        return new AsteriskCall();
    }

    public void shutdown() {
        try {
            managerConnection.logoff();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    void parseDialplan() throws ProviderUnavailableException {
        String dpClass = config.getString("dialplanBuilder", "com.headissue.asterisk.jtapi.gjtapi.DialplanBuilderExtensionsImpl");
        DialplanBuilder db = null;
        try {
            db = (DialplanBuilder) Class.forName(dpClass).newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new ProviderUnavailableException(e + "");
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new ProviderUnavailableException(e + "");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new ProviderUnavailableException(e + "");
        }
        db.setConfig(config);
        db.setManagerConnection(managerConnection);
        db.build();
        terminals = db.getTerminal2Addresses();
        addresses = db.getAddress2Terminals();
        terminal2context = db.getTerminal2Context();
        Set s = addresses.keySet();
        addressStrings = new String[s.size()];
        s.toArray(addressStrings);
    }

    public String[] getAddresses() throws ResourceUnavailableException {
        return addressStrings;
    }

    public String[] getAddresses(String _terminal) {
        ArrayList al = (ArrayList) terminals.get(_terminal);
        String[] sa = new String[al.size()];
        al.toArray(sa);
        return sa;
    }

    TermData[] terminalDataArray(Collection c) {
        TermData[] tda = new TermData[c.size()];
        int i = 0;
        Iterator it = c.iterator();
        while (it.hasNext()) {
            tda[i] = new TermData((String) it.next(), false);
            i++;
        }
        return tda;
    }

    public TermData[] getTerminals() throws ResourceUnavailableException {
        Set s = terminals.keySet();
        return terminalDataArray(s);
    }

    public TermData[] getTerminals(String _address) throws InvalidArgumentException {
        ArrayList al = (ArrayList) addresses.get(_address);
        if (al != null) {
            return terminalDataArray(al);
        }
        throw new InvalidArgumentException();
    }

    public void initialize(Map props) throws ProviderUnavailableException {
        asteriskManager = new DefaultAsteriskManager();
        managerConnection = new DefaultManagerConnection();
        asteriskManager.setManagerConnection(managerConnection);
        try {
            config = new ProviderConfig(props);
            managerConnection.setHostname(config.getString("Server"));
            managerConnection.setPort(config.getInt("Port"));
            managerConnection.setUsername(config.getString("Login"));
            managerConnection.setPassword(config.getString("Password"));
            asteriskManager.initialize();
        } catch (IOException e) {
            throw new ProviderUnavailableException(e.toString());
        } catch (AuthenticationFailedException e) {
            throw new ProviderUnavailableException(e.toString());
        } catch (TimeoutException e) {
            throw new ProviderUnavailableException(e.toString());
        }
        parseDialplan();
        managerConnection.addEventHandler(new MyManagerEventHandler());
    }

    AsteriskCall getCall(String _uniqueId) {
        AsteriskCall _call = (AsteriskCall) id2call.get(_uniqueId);
        if (_call == null) {
            _call = new AsteriskCall();
            _call.uniqueId = _uniqueId;
            id2call.put(_uniqueId, _call);
        }
        return _call;
    }

    class MyManagerEventHandler implements ManagerEventHandler {

        public void handleEvent(ManagerEvent _event) {
            if (config.debug > 0) {
                System.out.println("* event: " + ToString.convert(_event));
            }
            if (_event instanceof OriginateSuccessEvent) {
                OriginateSuccessEvent ev = (OriginateSuccessEvent) _event;
                AsteriskCall _call = (AsteriskCall) id2call.get(ev.getUniqueId());
                if (_call != null && !_call.originate && config.outgoingContext.equals(ev.getContext())) {
                    String s = ev.getChannel();
                    if (s.startsWith("local/")) {
                        s = s.substring("local/".length());
                        int i = s.indexOf('@');
                        if (i > 0) {
                            String _terminal = s.substring(0, i);
                            String _context = s.substring(i + 1);
                            _call.originate = true;
                            _call.calledAddress = ev.getExten();
                            _call.leg2.address = ev.getExten();
                            _call.address = _call.firstCallerId;
                            _call.terminal = _terminal;
                            listener.connectionConnected(_call, _call.address, CallEvent.CAUSE_NEW_CALL);
                            listener.terminalConnectionTalking(_call, _call.address, _call.terminal, CallEvent.CAUSE_NEW_CALL);
                            listener.connectionInProgress(_call, _call.calledAddress, CallEvent.CAUSE_NEW_CALL);
                        }
                    }
                }
                return;
            }
            if (_event instanceof NewStateEvent) {
                NewStateEvent ev = (NewStateEvent) _event;
                AsteriskCall _call = (AsteriskCall) id2call.get(ev.getUniqueId());
                if (_call == null || _call.address == null) {
                    return;
                }
                if (ev.getState().equals("Ringing")) {
                    listener.connectionAlerting(_call.realCall, _call.address, CallEvent.CAUSE_NORMAL);
                    if (_call.terminal != null) {
                        listener.terminalConnectionRinging(_call.realCall, _call.address, _call.terminal, CallEvent.CAUSE_NORMAL);
                    }
                } else if (ev.getState().equals("Up")) {
                    listener.connectionConnected(_call.realCall, _call.address, CallEvent.CALL_ACTIVE);
                    if (_call.terminal != null) {
                        listener.terminalConnectionTalking(_call.realCall, _call.address, _call.terminal, CallEvent.CAUSE_NORMAL);
                    }
                }
                return;
            }
            if (_event instanceof NewCallerIdEvent) {
                NewCallerIdEvent ev = (NewCallerIdEvent) _event;
                AsteriskCall _call = getCall(ev.getUniqueId());
                if (_call.firstCallerId == null) {
                    _call.firstCallerId = ev.getCallerId();
                }
                _call.callerId = ev.getCallerId();
                return;
            }
            if (_event instanceof NewExtenEvent) {
                NewExtenEvent nee = (NewExtenEvent) _event;
                if (config.incomingContext.contains(nee.getContext())) {
                    AsteriskCall _call = getCall(nee.getUniqueId());
                    if (_call.address == null) {
                        _call.address = nee.getExtension();
                        if (_call.callerId != null) {
                            listener.connectionConnected(_call, _call.callerId, CallEvent.CAUSE_NEW_CALL);
                        }
                        listener.connectionInProgress(_call, _call.address, CallEvent.CAUSE_NEW_CALL);
                    }
                }
                if (config.terminalContext.contains(nee.getContext())) {
                    AsteriskCall _call = (AsteriskCall) id2call.get(nee.getUniqueId());
                    if (_call != null) {
                        _call.terminal = nee.getExtension();
                    }
                }
                return;
            }
            if (_event instanceof DialEvent) {
                DialEvent de = (DialEvent) _event;
                AsteriskCall _call = (AsteriskCall) id2call.get(de.getSrcUniqueId());
                if (_call != null) {
                    AsteriskCall c2 = getCall(de.getDestUniqueId());
                    c2.realCall = _call;
                    c2.address = _call.calledAddress;
                    _call.leg2 = c2;
                }
            }
            if (_event instanceof ChannelEvent) {
                ChannelEvent ce = (ChannelEvent) _event;
                AsteriskCall _call = (AsteriskCall) id2call.get(ce.getUniqueId());
                if (_call == null) {
                    return;
                }
                if (ce instanceof NewChannelEvent) {
                    String _state = ce.getState();
                    if ("Ring".equals(_state)) {
                        _call.callerId = ce.getCallerId();
                        return;
                    }
                }
                if (_event instanceof HangupEvent) {
                    if (_call.callerId != null && _call.leg2 == null) {
                        listener.connectionDisconnected(_call, _call.callerId, CallEvent.CAUSE_NORMAL);
                    }
                    if (_call.terminal != null) {
                        listener.terminalConnectionDropped(_call.realCall, _call.address, _call.terminal, CallEvent.CAUSE_NORMAL);
                    }
                    listener.connectionDisconnected(_call.realCall, _call.address, CallEvent.CAUSE_NORMAL);
                    id2call.remove(ce.getUniqueId());
                }
            }
        }
    }
}
