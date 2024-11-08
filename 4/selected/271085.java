package org.beepcore.beep.core.serialize;

/**
 *
 * @author Huston Franklin
 * @version $Revision: 1.1 $, $Date: 2006/02/25 18:02:49 $
 */
public class CloseElement extends ChannelIndication {

    private int channelNumber;

    private int code;

    private String xmlLang;

    private String diagnostic;

    public CloseElement(int channelNumber, int code, String xmlLang, String diagnostic) {
        super(ChannelIndication.CLOSE);
        this.channelNumber = channelNumber;
        this.code = code;
        this.xmlLang = xmlLang;
        this.diagnostic = diagnostic;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public int getCode() {
        return code;
    }

    public String getXmlLang() {
        return xmlLang;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setXmlLang(String xmlLang) {
        this.xmlLang = xmlLang;
    }

    public void setDiagnostic(String diagnostic) {
        this.diagnostic = diagnostic;
    }
}
