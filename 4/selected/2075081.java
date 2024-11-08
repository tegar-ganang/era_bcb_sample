package com.pbxworkbench.pbx.mock;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.command.AgiCommand;
import org.asteriskjava.fastagi.reply.AgiReply;
import com.pbxworkbench.pbx.IChannelAddress;

public class MockChannel implements AgiChannel {

    private IChannelAddress address;

    public MockChannel(IChannelAddress address) {
        this.address = address;
    }

    public AgiReply sendCommand(AgiCommand command) throws AgiException {
        return null;
    }

    public void answer() throws AgiException {
    }

    public void hangup() throws AgiException {
    }

    public void setAutoHangup(int time) throws AgiException {
    }

    public void setCallerId(String callerId) throws AgiException {
    }

    public void playMusicOnHold() throws AgiException {
    }

    public void playMusicOnHold(String musicOnHoldClass) throws AgiException {
    }

    public void stopMusicOnHold() throws AgiException {
    }

    public int getChannelStatus() throws AgiException {
        return 0;
    }

    public String getData(String file) throws AgiException {
        return null;
    }

    public String getData(String file, long timeout) throws AgiException {
        return null;
    }

    public String getData(String file, long timeout, int maxDigits) throws AgiException {
        return null;
    }

    public char getOption(String file, String escapeDigits) throws AgiException {
        return 0;
    }

    public char getOption(String file, String escapeDigits, int timeout) throws AgiException {
        return 0;
    }

    public int exec(String application) throws AgiException {
        return 0;
    }

    public int exec(String application, String options) throws AgiException {
        return 0;
    }

    public void setContext(String context) throws AgiException {
    }

    public void setExtension(String extension) throws AgiException {
    }

    public void setPriority(String priority) throws AgiException {
    }

    public void streamFile(String file) throws AgiException {
    }

    public char streamFile(String file, String escapeDigits) throws AgiException {
        return 0;
    }

    public void sayDigits(String digits) throws AgiException {
    }

    public char sayDigits(String digits, String escapeDigits) throws AgiException {
        return 0;
    }

    public void sayNumber(String number) throws AgiException {
    }

    public char sayNumber(String number, String escapeDigits) throws AgiException {
        return 0;
    }

    public void sayPhonetic(String text) throws AgiException {
    }

    public char sayPhonetic(String text, String escapeDigits) throws AgiException {
        return 0;
    }

    public void sayAlpha(String text) throws AgiException {
    }

    public char sayAlpha(String text, String escapeDigits) throws AgiException {
        return 0;
    }

    public void sayTime(long time) throws AgiException {
    }

    public char sayTime(long time, String escapeDigits) throws AgiException {
        return 0;
    }

    public String getVariable(String name) throws AgiException {
        return null;
    }

    public void setVariable(String name, String value) throws AgiException {
    }

    public char waitForDigit(int timeout) throws AgiException {
        return 0;
    }

    public String getFullVariable(String name) throws AgiException {
        return null;
    }

    public String getFullVariable(String name, String channel) throws AgiException {
        return null;
    }

    public void sayDateTime(long time) throws AgiException {
    }

    public char sayDateTime(long time, String escapeDigits) throws AgiException {
        return 0;
    }

    public char sayDateTime(long time, String escapeDigits, String format) throws AgiException {
        return 0;
    }

    public char sayDateTime(long time, String escapeDigits, String format, String timezone) throws AgiException {
        return 0;
    }

    public String databaseGet(String family, String key) throws AgiException {
        return null;
    }

    public void databasePut(String family, String key, String value) throws AgiException {
    }

    public void databaseDel(String family, String key) throws AgiException {
    }

    public void databaseDelTree(String family) throws AgiException {
    }

    public void databaseDelTree(String family, String keytree) throws AgiException {
    }

    public void verbose(String message, int level) throws AgiException {
    }

    public void recordFile(String file, String format, String escapeDigits, int timeout) throws AgiException {
    }

    public void recordFile(String file, String format, String escapeDigits, int timeout, int offset, boolean beep, int maxSilence) throws AgiException {
    }

    public void controlStreamFile(String file) throws AgiException {
    }

    public char controlStreamFile(String file, String escapeDigits) throws AgiException {
        return 0;
    }

    public char controlStreamFile(String file, String escapeDigits, int offset) throws AgiException {
        return 0;
    }

    public char controlStreamFile(String file, String escapeDigits, int offset, String forwardDigit, String rewindDigit, String pauseDigit) throws AgiException {
        return 0;
    }

    @Override
    public String toString() {
        return "MockChannel(" + address + ")";
    }
}
