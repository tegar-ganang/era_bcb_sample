package de.schwarzrot.vdr.data.processing;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.schwarzrot.app.Client;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.app.errors.InvalidEpgDataException;
import de.schwarzrot.app.support.AbstractResponseHandler;
import de.schwarzrot.data.access.support.ConditionElement;
import de.schwarzrot.data.access.support.EqualConditionElement;
import de.schwarzrot.data.access.support.UpperBoundConditionElement;
import de.schwarzrot.data.transaction.TORead;
import de.schwarzrot.data.transaction.TORemove;
import de.schwarzrot.data.transaction.TOSave;
import de.schwarzrot.data.transaction.Transaction;
import de.schwarzrot.vdr.data.domain.ChannelInfo;
import de.schwarzrot.vdr.data.domain.EpgEvent;

public class VdrEpgImporter extends AbstractResponseHandler {

    private static final String AUDIO_STREAM = "2";

    private static final String CHANNEL_END = "c";

    private static final String CHANNEL_START = "C ";

    private static final String EVENT_END = "e";

    private static final String EVENT_START = "E ";

    private static final Map<Character, Pattern> pattern;

    private static final String VIDEO_STREAM = "1";

    public VdrEpgImporter(Client client) {
        super(client, "GEPG", null);
    }

    @Override
    public boolean skipCheck() {
        return true;
    }

    protected EpgEvent checkEvent(EpgEvent evt) {
        EpgEvent rv = evt;
        List<ConditionElement> args = new ArrayList<ConditionElement>();
        args.add(new EqualConditionElement("channel", evt.getChannel()));
        args.add(new EqualConditionElement("epgId", evt.getEpgId()));
        Transaction ta = getTransactionFactory().createTransaction();
        TORead<EpgEvent> tor = new TORead<EpgEvent>(EpgEvent.class, args);
        ta.add(tor);
        ta.setRollbackOnly();
        ta.execute();
        List<EpgEvent> tmp = tor.getResult();
        if (tmp != null && tmp.size() > 0) {
            rv = tmp.get(0);
            rv.setChannel(evt.getChannel());
        }
        return rv;
    }

    @Override
    protected void cleanup(long maxLifeTime) {
        Date expired = new Date(new Date().getTime() - maxLifeTime * 1000);
        Transaction ta = getTransactionFactory().createTransaction();
        ta.add(new TORemove<EpgEvent>(EpgEvent.class, new UpperBoundConditionElement("begin", expired)));
        ta.execute();
    }

    protected void processChannel(String line, BufferedReader br) {
        ChannelInfo channel = null;
        ChannelInfo ci = ChannelInfo.valueOf(line);
        Transaction ta = getTransactionFactory().createTransaction();
        TORead<ChannelInfo> tor = new TORead<ChannelInfo>(ChannelInfo.class);
        tor.addCondition(new EqualConditionElement("source", ci.getSource()));
        tor.addCondition(new EqualConditionElement("netId", ci.getNetId()));
        tor.addCondition(new EqualConditionElement("tsId", ci.getTsId()));
        tor.addCondition(new EqualConditionElement("serviceId", ci.getServiceId()));
        tor.addCondition(new EqualConditionElement("radioId", ci.getRadioId()));
        ta.add(tor);
        ta.setRollbackOnly();
        ta.execute();
        List<ChannelInfo> tmp = tor.getResult();
        if (tmp != null && tmp.size() > 0) channel = tmp.get(0);
        if (channel != null) {
            try {
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(EVENT_START)) {
                        try {
                            processEvent(line, br, channel);
                        } catch (Throwable t) {
                            getLogger().fatal("failed to process event", t);
                        }
                    } else if (line.startsWith(CHANNEL_END)) break; else if (line.equals(ENDSIG)) break;
                }
            } catch (Throwable t) {
                getLogger().fatal("failed to read line", t);
            }
        }
    }

    protected void processEvent(String line, BufferedReader br, ChannelInfo channel) {
        try {
            EpgEvent evt = EpgEvent.valueOf(line);
            Matcher m = null;
            evt.setChannel(channel);
            evt = checkEvent(evt);
            while ((line = br.readLine()) != null) {
                if (line.startsWith(EVENT_END)) {
                    if (evt.getTitle() != null) {
                        try {
                            Transaction ta = getTransactionFactory().createTransaction();
                            ta.add(new TOSave<EpgEvent>(evt));
                            ta.execute();
                        } catch (Throwable t) {
                            getLogger().fatal("failed to save EPG event: " + evt.toString(), t);
                        }
                    } else {
                        getLogger().fatal("skip saving of invalid event: " + evt.toString());
                    }
                    break;
                }
                Character key = line.charAt(0);
                Pattern p = pattern.get(key);
                if (p == null) throw new ApplicationException("unknown key of EPG-event: [" + key + "]");
                m = p.matcher(line);
                if (m.matches()) {
                    switch(key) {
                        case 'T':
                            evt.setTitle(validateText(m.group(1)));
                            break;
                        case 'S':
                            evt.setSubTitle(validateText(m.group(1)));
                            break;
                        case 'D':
                            evt.setDescription(validateText(m.group(1)));
                            break;
                        case 'G':
                            evt.setGenre(validateText(m.group(1)));
                            break;
                        case 'R':
                            evt.setMinAge(Integer.parseInt(m.group(1)));
                            break;
                        case 'V':
                            evt.setVpsBegin(new Date(Long.valueOf(m.group(1)).longValue() * 1000));
                            break;
                        case 'X':
                            if (m.group(1).equals(VIDEO_STREAM)) evt.setVideoMode(m.group(4)); else if (m.group(1).equals(AUDIO_STREAM)) {
                                if (m.group(4).equals("Dolby")) evt.setWithAC3(true);
                            }
                            break;
                        default:
                            throw new InvalidEpgDataException("unknown line in epg file: " + line);
                    }
                }
            }
        } catch (Throwable t) {
            getLogger().fatal("failed to process EPG data", t);
            throw new ApplicationException("failed to process EPG with ", t);
        }
    }

    @Override
    protected void processLine(String line, BufferedReader br) {
        if (line.startsWith(CHANNEL_START)) processChannel(line, br);
    }

    protected String validateText(String rawText) {
        StringBuilder work = new StringBuilder(rawText);
        for (int i = 0; i < work.length(); i++) {
            switch(work.charAt(i)) {
                case '|':
                    work.setCharAt(i, '\n');
                    break;
            }
        }
        return work.toString();
    }

    static {
        pattern = new HashMap<Character, Pattern>(15);
        pattern.put('C', Pattern.compile("^C\\s+(\\S+)\\s+(.+)$"));
        pattern.put('E', Pattern.compile("^E\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)(?:\\s+(\\S+))?$"));
        pattern.put('T', Pattern.compile("^T\\s+(.+)$"));
        pattern.put('G', Pattern.compile("^G\\s+(.+)$"));
        pattern.put('R', Pattern.compile("^G\\s+(\\d+)$"));
        pattern.put('S', Pattern.compile("^S\\s+(.+)$"));
        pattern.put('D', Pattern.compile("^D\\s+(.+)$"));
        pattern.put('X', Pattern.compile("^X\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)"));
        pattern.put('V', Pattern.compile("^V\\s+(\\d+)$"));
    }
}
