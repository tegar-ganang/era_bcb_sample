package com.ark.fix.model.fix;

import com.ark.fix.model.*;
import java.io.*;
import java.util.*;

public class Email extends FIXMessage {

    public static final int TAG_EmailThreadID = 164;

    public static final String FIELD_EmailThreadID = "EmailThreadID";

    public static final int TAG_EmailType = 94;

    public static final String FIELD_EmailType = "EmailType";

    public static final int TAG_OrigTime = 42;

    public static final String FIELD_OrigTime = "OrigTime";

    public static final int TAG_Subject = 147;

    public static final String FIELD_Subject = "Subject";

    public static final int TAG_EncodedSubjectLen = 356;

    public static final String FIELD_EncodedSubjectLen = "EncodedSubjectLen";

    public static final int TAG_EncodedSubject = 357;

    public static final String FIELD_EncodedSubject = "EncodedSubject";

    public static final int TAG_NoRelatedSym = 146;

    public static final String FIELD_NoRelatedSym = "NoRelatedSym";

    public static final int TAG_RelatdSymSeq = -1;

    public static final String FIELD_RelatdSymSeq = "RelatdSymSeq";

    public static final int TAG_OrderID = 37;

    public static final String FIELD_OrderID = "OrderID";

    public static final int TAG_ClOrdID = 11;

    public static final String FIELD_ClOrdID = "ClOrdID";

    public static final int TAG_LinesOfText = 33;

    public static final String FIELD_LinesOfText = "LinesOfText";

    public static final int TAG_LinesOfTextSeq = -1;

    public static final String FIELD_LinesOfTextSeq = "LinesOfTextSeq";

    public static final int TAG_RawDataLength = 95;

    public static final String FIELD_RawDataLength = "RawDataLength";

    public static final int TAG_RawData = 96;

    public static final String FIELD_RawData = "RawData";

    protected String _EmailThreadID;

    protected String _EmailType;

    protected String _OrigTime;

    protected String _Subject;

    protected String _EncodedSubjectLen;

    protected String _EncodedSubject;

    protected String _NoRelatedSym;

    protected FIXObjSeq _RelatdSymSeq;

    protected String _OrderID;

    protected String _ClOrdID;

    protected String _LinesOfText;

    protected FIXObjSeq _LinesOfTextSeq;

    protected String _RawDataLength;

    protected String _RawData;

    public String getEmailThreadID() {
        return _EmailThreadID;
    }

    public void setEmailThreadID(String s) {
        _EmailThreadID = s;
    }

    public String getEmailThreadIDJ() throws ModelException {
        if (_EmailThreadID == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _EmailThreadID);
    }

    public void setEmailThreadIDJ(String obj) throws ModelException {
        if (obj == null) {
            _EmailThreadID = null;
            return;
        }
        _EmailThreadID = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getEmailType() {
        return _EmailType;
    }

    public void setEmailType(String s) {
        _EmailType = s;
    }

    public Character getEmailTypeJ() throws ModelException {
        if (_EmailType == null) return null;
        return (Character) FIXDataConverter.getNativeJavaData(FIXDataConverter.CHAR, _EmailType);
    }

    public void setEmailTypeJ(Character obj) throws ModelException {
        if (obj == null) {
            _EmailType = null;
            return;
        }
        _EmailType = FIXDataConverter.getNativeFIXString(FIXDataConverter.CHAR, obj);
    }

    public String getOrigTime() {
        return _OrigTime;
    }

    public void setOrigTime(String s) {
        _OrigTime = s;
    }

    public Date getOrigTimeJ() throws ModelException {
        if (_OrigTime == null) return null;
        return (Date) FIXDataConverter.getNativeJavaData(FIXDataConverter.UTCTIMESTAMP, _OrigTime);
    }

    public void setOrigTimeJ(Date obj) throws ModelException {
        if (obj == null) {
            _OrigTime = null;
            return;
        }
        _OrigTime = FIXDataConverter.getNativeFIXString(FIXDataConverter.UTCTIMESTAMP, obj);
    }

    public String getSubject() {
        return _Subject;
    }

    public void setSubject(String s) {
        _Subject = s;
    }

    public String getSubjectJ() throws ModelException {
        if (_Subject == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _Subject);
    }

    public void setSubjectJ(String obj) throws ModelException {
        if (obj == null) {
            _Subject = null;
            return;
        }
        _Subject = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getEncodedSubjectLen() {
        return _EncodedSubjectLen;
    }

    public void setEncodedSubjectLen(String s) {
        _EncodedSubjectLen = s;
    }

    public Long getEncodedSubjectLenJ() throws ModelException {
        if (_EncodedSubjectLen == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _EncodedSubjectLen);
    }

    public void setEncodedSubjectLenJ(Long obj) throws ModelException {
        if (obj == null) {
            _EncodedSubjectLen = null;
            return;
        }
        _EncodedSubjectLen = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public String getEncodedSubject() {
        return _EncodedSubject;
    }

    public void setEncodedSubject(String s) {
        _EncodedSubject = s;
    }

    public byte[] getEncodedSubjectJ() throws ModelException {
        if (_EncodedSubject == null) return null;
        return (byte[]) FIXDataConverter.getNativeJavaData(FIXDataConverter.DATA, _EncodedSubject);
    }

    public void setEncodedSubjectJ(byte[] obj) throws ModelException {
        if (obj == null) {
            _EncodedSubject = null;
            return;
        }
        _EncodedSubject = FIXDataConverter.getNativeFIXString(FIXDataConverter.DATA, obj);
    }

    public String getNoRelatedSym() {
        return _NoRelatedSym;
    }

    public void setNoRelatedSym(String s) {
        _NoRelatedSym = s;
    }

    public Long getNoRelatedSymJ() throws ModelException {
        if (_NoRelatedSym == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _NoRelatedSym);
    }

    public void setNoRelatedSymJ(Long obj) throws ModelException {
        if (obj == null) {
            _NoRelatedSym = null;
            return;
        }
        _NoRelatedSym = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public FIXObjSeq getRelatdSymSeq() {
        return _RelatdSymSeq;
    }

    public void setRelatdSymSeq(FIXObjSeq aggregates) {
        _RelatdSymSeq = aggregates;
    }

    public String getOrderID() {
        return _OrderID;
    }

    public void setOrderID(String s) {
        _OrderID = s;
    }

    public String getOrderIDJ() throws ModelException {
        if (_OrderID == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _OrderID);
    }

    public void setOrderIDJ(String obj) throws ModelException {
        if (obj == null) {
            _OrderID = null;
            return;
        }
        _OrderID = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getClOrdID() {
        return _ClOrdID;
    }

    public void setClOrdID(String s) {
        _ClOrdID = s;
    }

    public String getClOrdIDJ() throws ModelException {
        if (_ClOrdID == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _ClOrdID);
    }

    public void setClOrdIDJ(String obj) throws ModelException {
        if (obj == null) {
            _ClOrdID = null;
            return;
        }
        _ClOrdID = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getLinesOfText() {
        return _LinesOfText;
    }

    public void setLinesOfText(String s) {
        _LinesOfText = s;
    }

    public Long getLinesOfTextJ() throws ModelException {
        if (_LinesOfText == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _LinesOfText);
    }

    public void setLinesOfTextJ(Long obj) throws ModelException {
        if (obj == null) {
            _LinesOfText = null;
            return;
        }
        _LinesOfText = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public FIXObjSeq getLinesOfTextSeq() {
        return _LinesOfTextSeq;
    }

    public void setLinesOfTextSeq(FIXObjSeq aggregates) {
        _LinesOfTextSeq = aggregates;
    }

    public String getRawDataLength() {
        return _RawDataLength;
    }

    public void setRawDataLength(String s) {
        _RawDataLength = s;
    }

    public Long getRawDataLengthJ() throws ModelException {
        if (_RawDataLength == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _RawDataLength);
    }

    public void setRawDataLengthJ(Long obj) throws ModelException {
        if (obj == null) {
            _RawDataLength = null;
            return;
        }
        _RawDataLength = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public String getRawData() {
        return _RawData;
    }

    public void setRawData(String s) {
        _RawData = s;
    }

    public byte[] getRawDataJ() throws ModelException {
        if (_RawData == null) return null;
        return (byte[]) FIXDataConverter.getNativeJavaData(FIXDataConverter.DATA, _RawData);
    }

    public void setRawDataJ(byte[] obj) throws ModelException {
        if (obj == null) {
            _RawData = null;
            return;
        }
        _RawData = FIXDataConverter.getNativeFIXString(FIXDataConverter.DATA, obj);
    }

    public String[] getProperties() {
        String[] properties = { "EmailThreadID", "EmailType", "OrigTime", "Subject", "EncodedSubjectLen", "EncodedSubject", "NoRelatedSym", "RelatdSymSeq", "OrderID", "ClOrdID", "LinesOfText", "LinesOfTextSeq", "RawDataLength", "RawData" };
        return properties;
    }

    public String[] getRequiredProperties() {
        String[] properties = { "EmailThreadID", "EmailType", "Subject", "LinesOfText" };
        return properties;
    }

    public String getMessageType() {
        return "C";
    }

    public boolean isValid() {
        if (_EmailThreadID == null) return false;
        if (!FIXDataValidator.isValidSTRING(_EmailThreadID)) return false;
        if (_EmailType == null) return false;
        if (!FIXDataValidator.isValidCHAR(_EmailType)) return false;
        if (_EmailType.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_94.indexOf(_EmailType) < 0) return false;
        if (!FIXDataValidator.isValidUTCTIMESTAMP(_OrigTime)) return false;
        if (_Subject == null) return false;
        if (!FIXDataValidator.isValidSTRING(_Subject)) return false;
        if (!FIXDataValidator.isValidINT(_EncodedSubjectLen)) return false;
        if (!FIXDataValidator.isValidDATA(_EncodedSubject)) return false;
        if (!FIXDataValidator.isValidINT(_NoRelatedSym)) return false;
        if (!_RelatdSymSeq.isValid()) return false;
        if (!FIXDataValidator.isValidSTRING(_OrderID)) return false;
        if (!FIXDataValidator.isValidSTRING(_ClOrdID)) return false;
        if (_LinesOfText == null) return false;
        if (!FIXDataValidator.isValidINT(_LinesOfText)) return false;
        if (!_LinesOfTextSeq.isValid()) return false;
        if (!FIXDataValidator.isValidINT(_RawDataLength)) return false;
        if (!FIXDataValidator.isValidDATA(_RawData)) return false;
        return true;
    }

    public int setValue(int p_tag, byte[] v_value) throws ModelException {
        if (p_tag == TAG_EmailThreadID) {
            _EmailThreadID = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_EmailType) {
            _EmailType = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_OrigTime) {
            _OrigTime = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Subject) {
            _Subject = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_EncodedSubjectLen) {
            _EncodedSubjectLen = new String(v_value);
            return DATA_TYPE;
        }
        if (p_tag == TAG_EncodedSubject) {
            _EncodedSubject = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_NoRelatedSym) {
            _NoRelatedSym = new String(v_value);
            return START_GROUP;
        }
        if (p_tag == TAG_OrderID) {
            _OrderID = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_ClOrdID) {
            _ClOrdID = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_LinesOfText) {
            _LinesOfText = new String(v_value);
            return START_GROUP;
        }
        if (p_tag == TAG_RawDataLength) {
            _RawDataLength = new String(v_value);
            return DATA_TYPE;
        }
        if (p_tag == TAG_RawData) {
            _RawData = new String(v_value);
            return NORMAL;
        }
        return NOT_MEMBER;
    }

    public Stack newGroup(int p_tag, int p_len) {
        if (p_tag == TAG_NoRelatedSym) {
            Stack stk = new Stack();
            _RelatdSymSeq = new FIXObjSeq(Email_RelatdSym.class);
            for (int i = 0; i < p_len; i++) {
                Email_RelatdSym child = new Email_RelatdSym();
                _RelatdSymSeq.add(child);
                stk.push(child);
            }
            return stk;
        }
        if (p_tag == TAG_LinesOfText) {
            Stack stk = new Stack();
            _LinesOfTextSeq = new FIXObjSeq(Email_LinesOfText.class);
            for (int i = 0; i < p_len; i++) {
                Email_LinesOfText child = new Email_LinesOfText();
                _LinesOfTextSeq.add(child);
                stk.push(child);
            }
            return stk;
        }
        return null;
    }

    public String toFIXMessage() {
        StringBuffer sb = new StringBuffer();
        if (_EmailThreadID != null) {
            sb.append(String.valueOf(TAG_EmailThreadID) + ES + _EmailThreadID + SOH);
        }
        if (_EmailType != null) {
            sb.append(String.valueOf(TAG_EmailType) + ES + _EmailType + SOH);
        }
        if (_OrigTime != null) {
            sb.append(String.valueOf(TAG_OrigTime) + ES + _OrigTime + SOH);
        }
        if (_Subject != null) {
            sb.append(String.valueOf(TAG_Subject) + ES + _Subject + SOH);
        }
        if (_EncodedSubjectLen != null) {
            sb.append(String.valueOf(TAG_EncodedSubjectLen) + ES + _EncodedSubjectLen + SOH);
        }
        if (_EncodedSubject != null) {
            sb.append(String.valueOf(TAG_EncodedSubject) + ES + _EncodedSubject + SOH);
        }
        if (_NoRelatedSym != null) {
            sb.append(String.valueOf(TAG_NoRelatedSym) + ES + _NoRelatedSym + SOH);
        }
        if (_RelatdSymSeq != null) {
            sb.append(_RelatdSymSeq.toFIXMessage());
        }
        if (_OrderID != null) {
            sb.append(String.valueOf(TAG_OrderID) + ES + _OrderID + SOH);
        }
        if (_ClOrdID != null) {
            sb.append(String.valueOf(TAG_ClOrdID) + ES + _ClOrdID + SOH);
        }
        if (_LinesOfText != null) {
            sb.append(String.valueOf(TAG_LinesOfText) + ES + _LinesOfText + SOH);
        }
        if (_LinesOfTextSeq != null) {
            sb.append(_LinesOfTextSeq.toFIXMessage());
        }
        if (_RawDataLength != null) {
            sb.append(String.valueOf(TAG_RawDataLength) + ES + _RawDataLength + SOH);
        }
        if (_RawData != null) {
            sb.append(String.valueOf(TAG_RawData) + ES + _RawData + SOH);
        }
        return sb.toString();
    }

    public byte[] toFIXBytes() {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            if (_EmailThreadID != null) {
                bs.write((String.valueOf(TAG_EmailThreadID) + ES + _EmailThreadID + SOH).getBytes());
            }
            if (_EmailType != null) {
                bs.write((String.valueOf(TAG_EmailType) + ES + _EmailType + SOH).getBytes());
            }
            if (_OrigTime != null) {
                bs.write((String.valueOf(TAG_OrigTime) + ES + _OrigTime + SOH).getBytes());
            }
            if (_Subject != null) {
                bs.write((String.valueOf(TAG_Subject) + ES + _Subject + SOH).getBytes());
            }
            if (_EncodedSubjectLen != null) {
                bs.write((String.valueOf(TAG_EncodedSubjectLen) + ES + _EncodedSubjectLen + SOH).getBytes());
            }
            if (_EncodedSubject != null) {
                bs.write((String.valueOf(TAG_EncodedSubject) + ES + _EncodedSubject + SOH).getBytes());
            }
            if (_NoRelatedSym != null) {
                bs.write((String.valueOf(TAG_NoRelatedSym) + ES + _NoRelatedSym + SOH).getBytes());
            }
            if (_RelatdSymSeq != null) {
                bs.write(_RelatdSymSeq.toFIXBytes());
            }
            if (_OrderID != null) {
                bs.write((String.valueOf(TAG_OrderID) + ES + _OrderID + SOH).getBytes());
            }
            if (_ClOrdID != null) {
                bs.write((String.valueOf(TAG_ClOrdID) + ES + _ClOrdID + SOH).getBytes());
            }
            if (_LinesOfText != null) {
                bs.write((String.valueOf(TAG_LinesOfText) + ES + _LinesOfText + SOH).getBytes());
            }
            if (_LinesOfTextSeq != null) {
                bs.write(_LinesOfTextSeq.toFIXBytes());
            }
            if (_RawDataLength != null) {
                bs.write((String.valueOf(TAG_RawDataLength) + ES + _RawDataLength + SOH).getBytes());
            }
            if (_RawData != null) {
                bs.write((String.valueOf(TAG_RawData) + ES + _RawData + SOH).getBytes());
            }
            byte[] t = bs.toByteArray();
            bs.close();
            return t;
        } catch (IOException ie) {
            ie.printStackTrace();
            return new byte[0];
        }
    }
}
