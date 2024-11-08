package com.ark.fix.model.fix;

import com.ark.fix.model.*;
import java.io.*;
import java.util.*;

public class Indication extends FIXMessage {

    public static final int TAG_IOIid = 23;

    public static final String FIELD_IOIid = "IOIid";

    public static final int TAG_IOITransType = 28;

    public static final String FIELD_IOITransType = "IOITransType";

    public static final int TAG_IOIRefID = 26;

    public static final String FIELD_IOIRefID = "IOIRefID";

    public static final int TAG_Symbol = 55;

    public static final String FIELD_Symbol = "Symbol";

    public static final int TAG_SymbolSfx = 65;

    public static final String FIELD_SymbolSfx = "SymbolSfx";

    public static final int TAG_SecurityID = 48;

    public static final String FIELD_SecurityID = "SecurityID";

    public static final int TAG_IDSource = 22;

    public static final String FIELD_IDSource = "IDSource";

    public static final int TAG_SecurityType = 167;

    public static final String FIELD_SecurityType = "SecurityType";

    public static final int TAG_MaturityMonthYear = 200;

    public static final String FIELD_MaturityMonthYear = "MaturityMonthYear";

    public static final int TAG_MaturityDay = 205;

    public static final String FIELD_MaturityDay = "MaturityDay";

    public static final int TAG_PutOrCall = 201;

    public static final String FIELD_PutOrCall = "PutOrCall";

    public static final int TAG_StrikePrice = 202;

    public static final String FIELD_StrikePrice = "StrikePrice";

    public static final int TAG_OptAttribute = 206;

    public static final String FIELD_OptAttribute = "OptAttribute";

    public static final int TAG_ContractMultiplier = 231;

    public static final String FIELD_ContractMultiplier = "ContractMultiplier";

    public static final int TAG_CouponRate = 223;

    public static final String FIELD_CouponRate = "CouponRate";

    public static final int TAG_SecurityExchange = 207;

    public static final String FIELD_SecurityExchange = "SecurityExchange";

    public static final int TAG_Issuer = 106;

    public static final String FIELD_Issuer = "Issuer";

    public static final int TAG_EncodedIssuerLen = 348;

    public static final String FIELD_EncodedIssuerLen = "EncodedIssuerLen";

    public static final int TAG_EncodedIssuer = 349;

    public static final String FIELD_EncodedIssuer = "EncodedIssuer";

    public static final int TAG_SecurityDesc = 107;

    public static final String FIELD_SecurityDesc = "SecurityDesc";

    public static final int TAG_EncodedSecurityDescLen = 350;

    public static final String FIELD_EncodedSecurityDescLen = "EncodedSecurityDescLen";

    public static final int TAG_EncodedSecurityDesc = 351;

    public static final String FIELD_EncodedSecurityDesc = "EncodedSecurityDesc";

    public static final int TAG_Side = 54;

    public static final String FIELD_Side = "Side";

    public static final int TAG_IOIShares = 27;

    public static final String FIELD_IOIShares = "IOIShares";

    public static final int TAG_Price = 44;

    public static final String FIELD_Price = "Price";

    public static final int TAG_Currency = 15;

    public static final String FIELD_Currency = "Currency";

    public static final int TAG_ValidUntilTime = 62;

    public static final String FIELD_ValidUntilTime = "ValidUntilTime";

    public static final int TAG_IOIQltyInd = 25;

    public static final String FIELD_IOIQltyInd = "IOIQltyInd";

    public static final int TAG_IOINaturalFlag = 130;

    public static final String FIELD_IOINaturalFlag = "IOINaturalFlag";

    public static final int TAG_NoIOIQualifiers = 199;

    public static final String FIELD_NoIOIQualifiers = "NoIOIQualifiers";

    public static final int TAG_IOIQualifierSeq = -1;

    public static final String FIELD_IOIQualifierSeq = "IOIQualifierSeq";

    public static final int TAG_Text = 58;

    public static final String FIELD_Text = "Text";

    public static final int TAG_EncodedTextLen = 354;

    public static final String FIELD_EncodedTextLen = "EncodedTextLen";

    public static final int TAG_EncodedText = 355;

    public static final String FIELD_EncodedText = "EncodedText";

    public static final int TAG_TransactTime = 60;

    public static final String FIELD_TransactTime = "TransactTime";

    public static final int TAG_URLLink = 149;

    public static final String FIELD_URLLink = "URLLink";

    public static final int TAG_NoRoutingIDs = 215;

    public static final String FIELD_NoRoutingIDs = "NoRoutingIDs";

    public static final int TAG_RoutingIDSeq = -1;

    public static final String FIELD_RoutingIDSeq = "RoutingIDSeq";

    public static final int TAG_SpreadToBenchmark = 218;

    public static final String FIELD_SpreadToBenchmark = "SpreadToBenchmark";

    public static final int TAG_Benchmark = 219;

    public static final String FIELD_Benchmark = "Benchmark";

    protected String _IOIid;

    protected String _IOITransType;

    protected String _IOIRefID;

    protected String _Symbol;

    protected String _SymbolSfx;

    protected String _SecurityID;

    protected String _IDSource;

    protected String _SecurityType;

    protected String _MaturityMonthYear;

    protected String _MaturityDay;

    protected String _PutOrCall;

    protected String _StrikePrice;

    protected String _OptAttribute;

    protected String _ContractMultiplier;

    protected String _CouponRate;

    protected String _SecurityExchange;

    protected String _Issuer;

    protected String _EncodedIssuerLen;

    protected String _EncodedIssuer;

    protected String _SecurityDesc;

    protected String _EncodedSecurityDescLen;

    protected String _EncodedSecurityDesc;

    protected String _Side;

    protected String _IOIShares;

    protected String _Price;

    protected String _Currency;

    protected String _ValidUntilTime;

    protected String _IOIQltyInd;

    protected String _IOINaturalFlag;

    protected String _NoIOIQualifiers;

    protected FIXObjSeq _IOIQualifierSeq;

    protected String _Text;

    protected String _EncodedTextLen;

    protected String _EncodedText;

    protected String _TransactTime;

    protected String _URLLink;

    protected String _NoRoutingIDs;

    protected FIXObjSeq _RoutingIDSeq;

    protected String _SpreadToBenchmark;

    protected String _Benchmark;

    public String getIOIid() {
        return _IOIid;
    }

    public void setIOIid(String s) {
        _IOIid = s;
    }

    public String getIOIidJ() throws ModelException {
        if (_IOIid == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _IOIid);
    }

    public void setIOIidJ(String obj) throws ModelException {
        if (obj == null) {
            _IOIid = null;
            return;
        }
        _IOIid = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getIOITransType() {
        return _IOITransType;
    }

    public void setIOITransType(String s) {
        _IOITransType = s;
    }

    public Character getIOITransTypeJ() throws ModelException {
        if (_IOITransType == null) return null;
        return (Character) FIXDataConverter.getNativeJavaData(FIXDataConverter.CHAR, _IOITransType);
    }

    public void setIOITransTypeJ(Character obj) throws ModelException {
        if (obj == null) {
            _IOITransType = null;
            return;
        }
        _IOITransType = FIXDataConverter.getNativeFIXString(FIXDataConverter.CHAR, obj);
    }

    public String getIOIRefID() {
        return _IOIRefID;
    }

    public void setIOIRefID(String s) {
        _IOIRefID = s;
    }

    public String getIOIRefIDJ() throws ModelException {
        if (_IOIRefID == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _IOIRefID);
    }

    public void setIOIRefIDJ(String obj) throws ModelException {
        if (obj == null) {
            _IOIRefID = null;
            return;
        }
        _IOIRefID = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getSymbol() {
        return _Symbol;
    }

    public void setSymbol(String s) {
        _Symbol = s;
    }

    public String getSymbolJ() throws ModelException {
        if (_Symbol == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _Symbol);
    }

    public void setSymbolJ(String obj) throws ModelException {
        if (obj == null) {
            _Symbol = null;
            return;
        }
        _Symbol = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getSymbolSfx() {
        return _SymbolSfx;
    }

    public void setSymbolSfx(String s) {
        _SymbolSfx = s;
    }

    public String getSymbolSfxJ() throws ModelException {
        if (_SymbolSfx == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _SymbolSfx);
    }

    public void setSymbolSfxJ(String obj) throws ModelException {
        if (obj == null) {
            _SymbolSfx = null;
            return;
        }
        _SymbolSfx = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getSecurityID() {
        return _SecurityID;
    }

    public void setSecurityID(String s) {
        _SecurityID = s;
    }

    public String getSecurityIDJ() throws ModelException {
        if (_SecurityID == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _SecurityID);
    }

    public void setSecurityIDJ(String obj) throws ModelException {
        if (obj == null) {
            _SecurityID = null;
            return;
        }
        _SecurityID = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getIDSource() {
        return _IDSource;
    }

    public void setIDSource(String s) {
        _IDSource = s;
    }

    public String getIDSourceJ() throws ModelException {
        if (_IDSource == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _IDSource);
    }

    public void setIDSourceJ(String obj) throws ModelException {
        if (obj == null) {
            _IDSource = null;
            return;
        }
        _IDSource = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getSecurityType() {
        return _SecurityType;
    }

    public void setSecurityType(String s) {
        _SecurityType = s;
    }

    public String getSecurityTypeJ() throws ModelException {
        if (_SecurityType == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _SecurityType);
    }

    public void setSecurityTypeJ(String obj) throws ModelException {
        if (obj == null) {
            _SecurityType = null;
            return;
        }
        _SecurityType = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getMaturityMonthYear() {
        return _MaturityMonthYear;
    }

    public void setMaturityMonthYear(String s) {
        _MaturityMonthYear = s;
    }

    public Date getMaturityMonthYearJ() throws ModelException {
        if (_MaturityMonthYear == null) return null;
        return (Date) FIXDataConverter.getNativeJavaData(FIXDataConverter.MONTHYEAR, _MaturityMonthYear);
    }

    public void setMaturityMonthYearJ(Date obj) throws ModelException {
        if (obj == null) {
            _MaturityMonthYear = null;
            return;
        }
        _MaturityMonthYear = FIXDataConverter.getNativeFIXString(FIXDataConverter.MONTHYEAR, obj);
    }

    public String getMaturityDay() {
        return _MaturityDay;
    }

    public void setMaturityDay(String s) {
        _MaturityDay = s;
    }

    public Date getMaturityDayJ() throws ModelException {
        if (_MaturityDay == null) return null;
        return (Date) FIXDataConverter.getNativeJavaData(FIXDataConverter.DAYOFMONTH, _MaturityDay);
    }

    public void setMaturityDayJ(Date obj) throws ModelException {
        if (obj == null) {
            _MaturityDay = null;
            return;
        }
        _MaturityDay = FIXDataConverter.getNativeFIXString(FIXDataConverter.DAYOFMONTH, obj);
    }

    public String getPutOrCall() {
        return _PutOrCall;
    }

    public void setPutOrCall(String s) {
        _PutOrCall = s;
    }

    public Long getPutOrCallJ() throws ModelException {
        if (_PutOrCall == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _PutOrCall);
    }

    public void setPutOrCallJ(Long obj) throws ModelException {
        if (obj == null) {
            _PutOrCall = null;
            return;
        }
        _PutOrCall = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public String getStrikePrice() {
        return _StrikePrice;
    }

    public void setStrikePrice(String s) {
        _StrikePrice = s;
    }

    public Double getStrikePriceJ() throws ModelException {
        if (_StrikePrice == null) return null;
        return (Double) FIXDataConverter.getNativeJavaData(FIXDataConverter.PRICE, _StrikePrice);
    }

    public void setStrikePriceJ(Double obj) throws ModelException {
        if (obj == null) {
            _StrikePrice = null;
            return;
        }
        _StrikePrice = FIXDataConverter.getNativeFIXString(FIXDataConverter.PRICE, obj);
    }

    public String getOptAttribute() {
        return _OptAttribute;
    }

    public void setOptAttribute(String s) {
        _OptAttribute = s;
    }

    public Character getOptAttributeJ() throws ModelException {
        if (_OptAttribute == null) return null;
        return (Character) FIXDataConverter.getNativeJavaData(FIXDataConverter.CHAR, _OptAttribute);
    }

    public void setOptAttributeJ(Character obj) throws ModelException {
        if (obj == null) {
            _OptAttribute = null;
            return;
        }
        _OptAttribute = FIXDataConverter.getNativeFIXString(FIXDataConverter.CHAR, obj);
    }

    public String getContractMultiplier() {
        return _ContractMultiplier;
    }

    public void setContractMultiplier(String s) {
        _ContractMultiplier = s;
    }

    public Double getContractMultiplierJ() throws ModelException {
        if (_ContractMultiplier == null) return null;
        return (Double) FIXDataConverter.getNativeJavaData(FIXDataConverter.FLOAT, _ContractMultiplier);
    }

    public void setContractMultiplierJ(Double obj) throws ModelException {
        if (obj == null) {
            _ContractMultiplier = null;
            return;
        }
        _ContractMultiplier = FIXDataConverter.getNativeFIXString(FIXDataConverter.FLOAT, obj);
    }

    public String getCouponRate() {
        return _CouponRate;
    }

    public void setCouponRate(String s) {
        _CouponRate = s;
    }

    public Double getCouponRateJ() throws ModelException {
        if (_CouponRate == null) return null;
        return (Double) FIXDataConverter.getNativeJavaData(FIXDataConverter.FLOAT, _CouponRate);
    }

    public void setCouponRateJ(Double obj) throws ModelException {
        if (obj == null) {
            _CouponRate = null;
            return;
        }
        _CouponRate = FIXDataConverter.getNativeFIXString(FIXDataConverter.FLOAT, obj);
    }

    public String getSecurityExchange() {
        return _SecurityExchange;
    }

    public void setSecurityExchange(String s) {
        _SecurityExchange = s;
    }

    public String getSecurityExchangeJ() throws ModelException {
        if (_SecurityExchange == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.EXCHANGE, _SecurityExchange);
    }

    public void setSecurityExchangeJ(String obj) throws ModelException {
        if (obj == null) {
            _SecurityExchange = null;
            return;
        }
        _SecurityExchange = FIXDataConverter.getNativeFIXString(FIXDataConverter.EXCHANGE, obj);
    }

    public String getIssuer() {
        return _Issuer;
    }

    public void setIssuer(String s) {
        _Issuer = s;
    }

    public String getIssuerJ() throws ModelException {
        if (_Issuer == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _Issuer);
    }

    public void setIssuerJ(String obj) throws ModelException {
        if (obj == null) {
            _Issuer = null;
            return;
        }
        _Issuer = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getEncodedIssuerLen() {
        return _EncodedIssuerLen;
    }

    public void setEncodedIssuerLen(String s) {
        _EncodedIssuerLen = s;
    }

    public Long getEncodedIssuerLenJ() throws ModelException {
        if (_EncodedIssuerLen == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _EncodedIssuerLen);
    }

    public void setEncodedIssuerLenJ(Long obj) throws ModelException {
        if (obj == null) {
            _EncodedIssuerLen = null;
            return;
        }
        _EncodedIssuerLen = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public String getEncodedIssuer() {
        return _EncodedIssuer;
    }

    public void setEncodedIssuer(String s) {
        _EncodedIssuer = s;
    }

    public byte[] getEncodedIssuerJ() throws ModelException {
        if (_EncodedIssuer == null) return null;
        return (byte[]) FIXDataConverter.getNativeJavaData(FIXDataConverter.DATA, _EncodedIssuer);
    }

    public void setEncodedIssuerJ(byte[] obj) throws ModelException {
        if (obj == null) {
            _EncodedIssuer = null;
            return;
        }
        _EncodedIssuer = FIXDataConverter.getNativeFIXString(FIXDataConverter.DATA, obj);
    }

    public String getSecurityDesc() {
        return _SecurityDesc;
    }

    public void setSecurityDesc(String s) {
        _SecurityDesc = s;
    }

    public String getSecurityDescJ() throws ModelException {
        if (_SecurityDesc == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _SecurityDesc);
    }

    public void setSecurityDescJ(String obj) throws ModelException {
        if (obj == null) {
            _SecurityDesc = null;
            return;
        }
        _SecurityDesc = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getEncodedSecurityDescLen() {
        return _EncodedSecurityDescLen;
    }

    public void setEncodedSecurityDescLen(String s) {
        _EncodedSecurityDescLen = s;
    }

    public Long getEncodedSecurityDescLenJ() throws ModelException {
        if (_EncodedSecurityDescLen == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _EncodedSecurityDescLen);
    }

    public void setEncodedSecurityDescLenJ(Long obj) throws ModelException {
        if (obj == null) {
            _EncodedSecurityDescLen = null;
            return;
        }
        _EncodedSecurityDescLen = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public String getEncodedSecurityDesc() {
        return _EncodedSecurityDesc;
    }

    public void setEncodedSecurityDesc(String s) {
        _EncodedSecurityDesc = s;
    }

    public byte[] getEncodedSecurityDescJ() throws ModelException {
        if (_EncodedSecurityDesc == null) return null;
        return (byte[]) FIXDataConverter.getNativeJavaData(FIXDataConverter.DATA, _EncodedSecurityDesc);
    }

    public void setEncodedSecurityDescJ(byte[] obj) throws ModelException {
        if (obj == null) {
            _EncodedSecurityDesc = null;
            return;
        }
        _EncodedSecurityDesc = FIXDataConverter.getNativeFIXString(FIXDataConverter.DATA, obj);
    }

    public String getSide() {
        return _Side;
    }

    public void setSide(String s) {
        _Side = s;
    }

    public Character getSideJ() throws ModelException {
        if (_Side == null) return null;
        return (Character) FIXDataConverter.getNativeJavaData(FIXDataConverter.CHAR, _Side);
    }

    public void setSideJ(Character obj) throws ModelException {
        if (obj == null) {
            _Side = null;
            return;
        }
        _Side = FIXDataConverter.getNativeFIXString(FIXDataConverter.CHAR, obj);
    }

    public String getIOIShares() {
        return _IOIShares;
    }

    public void setIOIShares(String s) {
        _IOIShares = s;
    }

    public String getIOISharesJ() throws ModelException {
        if (_IOIShares == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _IOIShares);
    }

    public void setIOISharesJ(String obj) throws ModelException {
        if (obj == null) {
            _IOIShares = null;
            return;
        }
        _IOIShares = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getPrice() {
        return _Price;
    }

    public void setPrice(String s) {
        _Price = s;
    }

    public Double getPriceJ() throws ModelException {
        if (_Price == null) return null;
        return (Double) FIXDataConverter.getNativeJavaData(FIXDataConverter.PRICE, _Price);
    }

    public void setPriceJ(Double obj) throws ModelException {
        if (obj == null) {
            _Price = null;
            return;
        }
        _Price = FIXDataConverter.getNativeFIXString(FIXDataConverter.PRICE, obj);
    }

    public String getCurrency() {
        return _Currency;
    }

    public void setCurrency(String s) {
        _Currency = s;
    }

    public String getCurrencyJ() throws ModelException {
        if (_Currency == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.CURRENCY, _Currency);
    }

    public void setCurrencyJ(String obj) throws ModelException {
        if (obj == null) {
            _Currency = null;
            return;
        }
        _Currency = FIXDataConverter.getNativeFIXString(FIXDataConverter.CURRENCY, obj);
    }

    public String getValidUntilTime() {
        return _ValidUntilTime;
    }

    public void setValidUntilTime(String s) {
        _ValidUntilTime = s;
    }

    public Date getValidUntilTimeJ() throws ModelException {
        if (_ValidUntilTime == null) return null;
        return (Date) FIXDataConverter.getNativeJavaData(FIXDataConverter.UTCTIMESTAMP, _ValidUntilTime);
    }

    public void setValidUntilTimeJ(Date obj) throws ModelException {
        if (obj == null) {
            _ValidUntilTime = null;
            return;
        }
        _ValidUntilTime = FIXDataConverter.getNativeFIXString(FIXDataConverter.UTCTIMESTAMP, obj);
    }

    public String getIOIQltyInd() {
        return _IOIQltyInd;
    }

    public void setIOIQltyInd(String s) {
        _IOIQltyInd = s;
    }

    public Character getIOIQltyIndJ() throws ModelException {
        if (_IOIQltyInd == null) return null;
        return (Character) FIXDataConverter.getNativeJavaData(FIXDataConverter.CHAR, _IOIQltyInd);
    }

    public void setIOIQltyIndJ(Character obj) throws ModelException {
        if (obj == null) {
            _IOIQltyInd = null;
            return;
        }
        _IOIQltyInd = FIXDataConverter.getNativeFIXString(FIXDataConverter.CHAR, obj);
    }

    public String getIOINaturalFlag() {
        return _IOINaturalFlag;
    }

    public void setIOINaturalFlag(String s) {
        _IOINaturalFlag = s;
    }

    public Boolean getIOINaturalFlagJ() throws ModelException {
        if (_IOINaturalFlag == null) return null;
        return (Boolean) FIXDataConverter.getNativeJavaData(FIXDataConverter.BOOLEAN, _IOINaturalFlag);
    }

    public void setIOINaturalFlagJ(Boolean obj) throws ModelException {
        if (obj == null) {
            _IOINaturalFlag = null;
            return;
        }
        _IOINaturalFlag = FIXDataConverter.getNativeFIXString(FIXDataConverter.BOOLEAN, obj);
    }

    public String getNoIOIQualifiers() {
        return _NoIOIQualifiers;
    }

    public void setNoIOIQualifiers(String s) {
        _NoIOIQualifiers = s;
    }

    public Long getNoIOIQualifiersJ() throws ModelException {
        if (_NoIOIQualifiers == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _NoIOIQualifiers);
    }

    public void setNoIOIQualifiersJ(Long obj) throws ModelException {
        if (obj == null) {
            _NoIOIQualifiers = null;
            return;
        }
        _NoIOIQualifiers = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public FIXObjSeq getIOIQualifierSeq() {
        return _IOIQualifierSeq;
    }

    public void setIOIQualifierSeq(FIXObjSeq aggregates) {
        _IOIQualifierSeq = aggregates;
    }

    public String getText() {
        return _Text;
    }

    public void setText(String s) {
        _Text = s;
    }

    public String getTextJ() throws ModelException {
        if (_Text == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _Text);
    }

    public void setTextJ(String obj) throws ModelException {
        if (obj == null) {
            _Text = null;
            return;
        }
        _Text = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getEncodedTextLen() {
        return _EncodedTextLen;
    }

    public void setEncodedTextLen(String s) {
        _EncodedTextLen = s;
    }

    public Long getEncodedTextLenJ() throws ModelException {
        if (_EncodedTextLen == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _EncodedTextLen);
    }

    public void setEncodedTextLenJ(Long obj) throws ModelException {
        if (obj == null) {
            _EncodedTextLen = null;
            return;
        }
        _EncodedTextLen = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public String getEncodedText() {
        return _EncodedText;
    }

    public void setEncodedText(String s) {
        _EncodedText = s;
    }

    public byte[] getEncodedTextJ() throws ModelException {
        if (_EncodedText == null) return null;
        return (byte[]) FIXDataConverter.getNativeJavaData(FIXDataConverter.DATA, _EncodedText);
    }

    public void setEncodedTextJ(byte[] obj) throws ModelException {
        if (obj == null) {
            _EncodedText = null;
            return;
        }
        _EncodedText = FIXDataConverter.getNativeFIXString(FIXDataConverter.DATA, obj);
    }

    public String getTransactTime() {
        return _TransactTime;
    }

    public void setTransactTime(String s) {
        _TransactTime = s;
    }

    public Date getTransactTimeJ() throws ModelException {
        if (_TransactTime == null) return null;
        return (Date) FIXDataConverter.getNativeJavaData(FIXDataConverter.UTCTIMESTAMP, _TransactTime);
    }

    public void setTransactTimeJ(Date obj) throws ModelException {
        if (obj == null) {
            _TransactTime = null;
            return;
        }
        _TransactTime = FIXDataConverter.getNativeFIXString(FIXDataConverter.UTCTIMESTAMP, obj);
    }

    public String getURLLink() {
        return _URLLink;
    }

    public void setURLLink(String s) {
        _URLLink = s;
    }

    public String getURLLinkJ() throws ModelException {
        if (_URLLink == null) return null;
        return (String) FIXDataConverter.getNativeJavaData(FIXDataConverter.STRING, _URLLink);
    }

    public void setURLLinkJ(String obj) throws ModelException {
        if (obj == null) {
            _URLLink = null;
            return;
        }
        _URLLink = FIXDataConverter.getNativeFIXString(FIXDataConverter.STRING, obj);
    }

    public String getNoRoutingIDs() {
        return _NoRoutingIDs;
    }

    public void setNoRoutingIDs(String s) {
        _NoRoutingIDs = s;
    }

    public Long getNoRoutingIDsJ() throws ModelException {
        if (_NoRoutingIDs == null) return null;
        return (Long) FIXDataConverter.getNativeJavaData(FIXDataConverter.INT, _NoRoutingIDs);
    }

    public void setNoRoutingIDsJ(Long obj) throws ModelException {
        if (obj == null) {
            _NoRoutingIDs = null;
            return;
        }
        _NoRoutingIDs = FIXDataConverter.getNativeFIXString(FIXDataConverter.INT, obj);
    }

    public FIXObjSeq getRoutingIDSeq() {
        return _RoutingIDSeq;
    }

    public void setRoutingIDSeq(FIXObjSeq aggregates) {
        _RoutingIDSeq = aggregates;
    }

    public String getSpreadToBenchmark() {
        return _SpreadToBenchmark;
    }

    public void setSpreadToBenchmark(String s) {
        _SpreadToBenchmark = s;
    }

    public Double getSpreadToBenchmarkJ() throws ModelException {
        if (_SpreadToBenchmark == null) return null;
        return (Double) FIXDataConverter.getNativeJavaData(FIXDataConverter.PRICEOFFSET, _SpreadToBenchmark);
    }

    public void setSpreadToBenchmarkJ(Double obj) throws ModelException {
        if (obj == null) {
            _SpreadToBenchmark = null;
            return;
        }
        _SpreadToBenchmark = FIXDataConverter.getNativeFIXString(FIXDataConverter.PRICEOFFSET, obj);
    }

    public String getBenchmark() {
        return _Benchmark;
    }

    public void setBenchmark(String s) {
        _Benchmark = s;
    }

    public Character getBenchmarkJ() throws ModelException {
        if (_Benchmark == null) return null;
        return (Character) FIXDataConverter.getNativeJavaData(FIXDataConverter.CHAR, _Benchmark);
    }

    public void setBenchmarkJ(Character obj) throws ModelException {
        if (obj == null) {
            _Benchmark = null;
            return;
        }
        _Benchmark = FIXDataConverter.getNativeFIXString(FIXDataConverter.CHAR, obj);
    }

    public String[] getProperties() {
        String[] properties = { "IOIid", "IOITransType", "IOIRefID", "Symbol", "SymbolSfx", "SecurityID", "IDSource", "SecurityType", "MaturityMonthYear", "MaturityDay", "PutOrCall", "StrikePrice", "OptAttribute", "ContractMultiplier", "CouponRate", "SecurityExchange", "Issuer", "EncodedIssuerLen", "EncodedIssuer", "SecurityDesc", "EncodedSecurityDescLen", "EncodedSecurityDesc", "Side", "IOIShares", "Price", "Currency", "ValidUntilTime", "IOIQltyInd", "IOINaturalFlag", "NoIOIQualifiers", "IOIQualifierSeq", "Text", "EncodedTextLen", "EncodedText", "TransactTime", "URLLink", "NoRoutingIDs", "RoutingIDSeq", "SpreadToBenchmark", "Benchmark" };
        return properties;
    }

    public String[] getRequiredProperties() {
        String[] properties = { "IOIid", "IOITransType", "Symbol", "Side", "IOIShares" };
        return properties;
    }

    public String getMessageType() {
        return "6";
    }

    public boolean isValid() {
        if (_IOIid == null) return false;
        if (!FIXDataValidator.isValidSTRING(_IOIid)) return false;
        if (_IOITransType == null) return false;
        if (!FIXDataValidator.isValidCHAR(_IOITransType)) return false;
        if (_IOITransType.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_28.indexOf(_IOITransType) < 0) return false;
        if (!FIXDataValidator.isValidSTRING(_IOIRefID)) return false;
        if (_Symbol == null) return false;
        if (!FIXDataValidator.isValidSTRING(_Symbol)) return false;
        if (!FIXDataValidator.isValidSTRING(_SymbolSfx)) return false;
        if (!FIXDataValidator.isValidSTRING(_SecurityID)) return false;
        if (!FIXDataValidator.isValidSTRING(_IDSource)) return false;
        if (_IDSource.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_22.indexOf(_IDSource) < 0) return false;
        if (!FIXDataValidator.isValidSTRING(_SecurityType)) return false;
        if (_SecurityType.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_167.indexOf(_SecurityType) < 0) return false;
        if (!FIXDataValidator.isValidMONTHYEAR(_MaturityMonthYear)) return false;
        if (!FIXDataValidator.isValidDAYOFMONTH(_MaturityDay)) return false;
        if (_MaturityDay.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_205.indexOf(_MaturityDay) < 0) return false;
        if (!FIXDataValidator.isValidINT(_PutOrCall)) return false;
        if (_PutOrCall.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_201.indexOf(_PutOrCall) < 0) return false;
        if (!FIXDataValidator.isValidPRICE(_StrikePrice)) return false;
        if (!FIXDataValidator.isValidCHAR(_OptAttribute)) return false;
        if (!FIXDataValidator.isValidFLOAT(_ContractMultiplier)) return false;
        if (!FIXDataValidator.isValidFLOAT(_CouponRate)) return false;
        if (!FIXDataValidator.isValidEXCHANGE(_SecurityExchange)) return false;
        if (!FIXDataValidator.isValidSTRING(_Issuer)) return false;
        if (!FIXDataValidator.isValidINT(_EncodedIssuerLen)) return false;
        if (!FIXDataValidator.isValidDATA(_EncodedIssuer)) return false;
        if (!FIXDataValidator.isValidSTRING(_SecurityDesc)) return false;
        if (!FIXDataValidator.isValidINT(_EncodedSecurityDescLen)) return false;
        if (!FIXDataValidator.isValidDATA(_EncodedSecurityDesc)) return false;
        if (_Side == null) return false;
        if (!FIXDataValidator.isValidCHAR(_Side)) return false;
        if (_Side.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_54.indexOf(_Side) < 0) return false;
        if (_IOIShares == null) return false;
        if (!FIXDataValidator.isValidSTRING(_IOIShares)) return false;
        if (!FIXDataValidator.isValidPRICE(_Price)) return false;
        if (!FIXDataValidator.isValidCURRENCY(_Currency)) return false;
        if (!FIXDataValidator.isValidUTCTIMESTAMP(_ValidUntilTime)) return false;
        if (!FIXDataValidator.isValidCHAR(_IOIQltyInd)) return false;
        if (_IOIQltyInd.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_25.indexOf(_IOIQltyInd) < 0) return false;
        if (!FIXDataValidator.isValidBOOLEAN(_IOINaturalFlag)) return false;
        if (_IOINaturalFlag.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_130.indexOf(_IOINaturalFlag) < 0) return false;
        if (!FIXDataValidator.isValidINT(_NoIOIQualifiers)) return false;
        if (!_IOIQualifierSeq.isValid()) return false;
        if (!FIXDataValidator.isValidSTRING(_Text)) return false;
        if (!FIXDataValidator.isValidINT(_EncodedTextLen)) return false;
        if (!FIXDataValidator.isValidDATA(_EncodedText)) return false;
        if (!FIXDataValidator.isValidUTCTIMESTAMP(_TransactTime)) return false;
        if (!FIXDataValidator.isValidSTRING(_URLLink)) return false;
        if (!FIXDataValidator.isValidINT(_NoRoutingIDs)) return false;
        if (!_RoutingIDSeq.isValid()) return false;
        if (!FIXDataValidator.isValidPRICEOFFSET(_SpreadToBenchmark)) return false;
        if (!FIXDataValidator.isValidCHAR(_Benchmark)) return false;
        if (_Benchmark.indexOf(";") >= 0 || FIXDataTypeDictionary.ENUM_219.indexOf(_Benchmark) < 0) return false;
        return true;
    }

    public int setValue(int p_tag, byte[] v_value) throws ModelException {
        if (p_tag == TAG_IOIid) {
            _IOIid = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_IOITransType) {
            _IOITransType = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_IOIRefID) {
            _IOIRefID = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Symbol) {
            _Symbol = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_SymbolSfx) {
            _SymbolSfx = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_SecurityID) {
            _SecurityID = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_IDSource) {
            _IDSource = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_SecurityType) {
            _SecurityType = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_MaturityMonthYear) {
            _MaturityMonthYear = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_MaturityDay) {
            _MaturityDay = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_PutOrCall) {
            _PutOrCall = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_StrikePrice) {
            _StrikePrice = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_OptAttribute) {
            _OptAttribute = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_ContractMultiplier) {
            _ContractMultiplier = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_CouponRate) {
            _CouponRate = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_SecurityExchange) {
            _SecurityExchange = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Issuer) {
            _Issuer = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_EncodedIssuerLen) {
            _EncodedIssuerLen = new String(v_value);
            return DATA_TYPE;
        }
        if (p_tag == TAG_EncodedIssuer) {
            _EncodedIssuer = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_SecurityDesc) {
            _SecurityDesc = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_EncodedSecurityDescLen) {
            _EncodedSecurityDescLen = new String(v_value);
            return DATA_TYPE;
        }
        if (p_tag == TAG_EncodedSecurityDesc) {
            _EncodedSecurityDesc = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Side) {
            _Side = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_IOIShares) {
            _IOIShares = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Price) {
            _Price = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Currency) {
            _Currency = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_ValidUntilTime) {
            _ValidUntilTime = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_IOIQltyInd) {
            _IOIQltyInd = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_IOINaturalFlag) {
            _IOINaturalFlag = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_NoIOIQualifiers) {
            _NoIOIQualifiers = new String(v_value);
            return START_GROUP;
        }
        if (p_tag == TAG_Text) {
            _Text = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_EncodedTextLen) {
            _EncodedTextLen = new String(v_value);
            return DATA_TYPE;
        }
        if (p_tag == TAG_EncodedText) {
            _EncodedText = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_TransactTime) {
            _TransactTime = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_URLLink) {
            _URLLink = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_NoRoutingIDs) {
            _NoRoutingIDs = new String(v_value);
            return START_GROUP;
        }
        if (p_tag == TAG_SpreadToBenchmark) {
            _SpreadToBenchmark = new String(v_value);
            return NORMAL;
        }
        if (p_tag == TAG_Benchmark) {
            _Benchmark = new String(v_value);
            return NORMAL;
        }
        return NOT_MEMBER;
    }

    public Stack newGroup(int p_tag, int p_len) {
        if (p_tag == TAG_NoIOIQualifiers) {
            Stack stk = new Stack();
            _IOIQualifierSeq = new FIXObjSeq(Indication_IOIQualifier.class);
            for (int i = 0; i < p_len; i++) {
                Indication_IOIQualifier child = new Indication_IOIQualifier();
                _IOIQualifierSeq.add(child);
                stk.push(child);
            }
            return stk;
        }
        if (p_tag == TAG_NoRoutingIDs) {
            Stack stk = new Stack();
            _RoutingIDSeq = new FIXObjSeq(Indication_RoutingID.class);
            for (int i = 0; i < p_len; i++) {
                Indication_RoutingID child = new Indication_RoutingID();
                _RoutingIDSeq.add(child);
                stk.push(child);
            }
            return stk;
        }
        return null;
    }

    public String toFIXMessage() {
        StringBuffer sb = new StringBuffer();
        if (_IOIid != null) {
            sb.append(String.valueOf(TAG_IOIid) + ES + _IOIid + SOH);
        }
        if (_IOITransType != null) {
            sb.append(String.valueOf(TAG_IOITransType) + ES + _IOITransType + SOH);
        }
        if (_IOIRefID != null) {
            sb.append(String.valueOf(TAG_IOIRefID) + ES + _IOIRefID + SOH);
        }
        if (_Symbol != null) {
            sb.append(String.valueOf(TAG_Symbol) + ES + _Symbol + SOH);
        }
        if (_SymbolSfx != null) {
            sb.append(String.valueOf(TAG_SymbolSfx) + ES + _SymbolSfx + SOH);
        }
        if (_SecurityID != null) {
            sb.append(String.valueOf(TAG_SecurityID) + ES + _SecurityID + SOH);
        }
        if (_IDSource != null) {
            sb.append(String.valueOf(TAG_IDSource) + ES + _IDSource + SOH);
        }
        if (_SecurityType != null) {
            sb.append(String.valueOf(TAG_SecurityType) + ES + _SecurityType + SOH);
        }
        if (_MaturityMonthYear != null) {
            sb.append(String.valueOf(TAG_MaturityMonthYear) + ES + _MaturityMonthYear + SOH);
        }
        if (_MaturityDay != null) {
            sb.append(String.valueOf(TAG_MaturityDay) + ES + _MaturityDay + SOH);
        }
        if (_PutOrCall != null) {
            sb.append(String.valueOf(TAG_PutOrCall) + ES + _PutOrCall + SOH);
        }
        if (_StrikePrice != null) {
            sb.append(String.valueOf(TAG_StrikePrice) + ES + _StrikePrice + SOH);
        }
        if (_OptAttribute != null) {
            sb.append(String.valueOf(TAG_OptAttribute) + ES + _OptAttribute + SOH);
        }
        if (_ContractMultiplier != null) {
            sb.append(String.valueOf(TAG_ContractMultiplier) + ES + _ContractMultiplier + SOH);
        }
        if (_CouponRate != null) {
            sb.append(String.valueOf(TAG_CouponRate) + ES + _CouponRate + SOH);
        }
        if (_SecurityExchange != null) {
            sb.append(String.valueOf(TAG_SecurityExchange) + ES + _SecurityExchange + SOH);
        }
        if (_Issuer != null) {
            sb.append(String.valueOf(TAG_Issuer) + ES + _Issuer + SOH);
        }
        if (_EncodedIssuerLen != null) {
            sb.append(String.valueOf(TAG_EncodedIssuerLen) + ES + _EncodedIssuerLen + SOH);
        }
        if (_EncodedIssuer != null) {
            sb.append(String.valueOf(TAG_EncodedIssuer) + ES + _EncodedIssuer + SOH);
        }
        if (_SecurityDesc != null) {
            sb.append(String.valueOf(TAG_SecurityDesc) + ES + _SecurityDesc + SOH);
        }
        if (_EncodedSecurityDescLen != null) {
            sb.append(String.valueOf(TAG_EncodedSecurityDescLen) + ES + _EncodedSecurityDescLen + SOH);
        }
        if (_EncodedSecurityDesc != null) {
            sb.append(String.valueOf(TAG_EncodedSecurityDesc) + ES + _EncodedSecurityDesc + SOH);
        }
        if (_Side != null) {
            sb.append(String.valueOf(TAG_Side) + ES + _Side + SOH);
        }
        if (_IOIShares != null) {
            sb.append(String.valueOf(TAG_IOIShares) + ES + _IOIShares + SOH);
        }
        if (_Price != null) {
            sb.append(String.valueOf(TAG_Price) + ES + _Price + SOH);
        }
        if (_Currency != null) {
            sb.append(String.valueOf(TAG_Currency) + ES + _Currency + SOH);
        }
        if (_ValidUntilTime != null) {
            sb.append(String.valueOf(TAG_ValidUntilTime) + ES + _ValidUntilTime + SOH);
        }
        if (_IOIQltyInd != null) {
            sb.append(String.valueOf(TAG_IOIQltyInd) + ES + _IOIQltyInd + SOH);
        }
        if (_IOINaturalFlag != null) {
            sb.append(String.valueOf(TAG_IOINaturalFlag) + ES + _IOINaturalFlag + SOH);
        }
        if (_NoIOIQualifiers != null) {
            sb.append(String.valueOf(TAG_NoIOIQualifiers) + ES + _NoIOIQualifiers + SOH);
        }
        if (_IOIQualifierSeq != null) {
            sb.append(_IOIQualifierSeq.toFIXMessage());
        }
        if (_Text != null) {
            sb.append(String.valueOf(TAG_Text) + ES + _Text + SOH);
        }
        if (_EncodedTextLen != null) {
            sb.append(String.valueOf(TAG_EncodedTextLen) + ES + _EncodedTextLen + SOH);
        }
        if (_EncodedText != null) {
            sb.append(String.valueOf(TAG_EncodedText) + ES + _EncodedText + SOH);
        }
        if (_TransactTime != null) {
            sb.append(String.valueOf(TAG_TransactTime) + ES + _TransactTime + SOH);
        }
        if (_URLLink != null) {
            sb.append(String.valueOf(TAG_URLLink) + ES + _URLLink + SOH);
        }
        if (_NoRoutingIDs != null) {
            sb.append(String.valueOf(TAG_NoRoutingIDs) + ES + _NoRoutingIDs + SOH);
        }
        if (_RoutingIDSeq != null) {
            sb.append(_RoutingIDSeq.toFIXMessage());
        }
        if (_SpreadToBenchmark != null) {
            sb.append(String.valueOf(TAG_SpreadToBenchmark) + ES + _SpreadToBenchmark + SOH);
        }
        if (_Benchmark != null) {
            sb.append(String.valueOf(TAG_Benchmark) + ES + _Benchmark + SOH);
        }
        return sb.toString();
    }

    public byte[] toFIXBytes() {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            if (_IOIid != null) {
                bs.write((String.valueOf(TAG_IOIid) + ES + _IOIid + SOH).getBytes());
            }
            if (_IOITransType != null) {
                bs.write((String.valueOf(TAG_IOITransType) + ES + _IOITransType + SOH).getBytes());
            }
            if (_IOIRefID != null) {
                bs.write((String.valueOf(TAG_IOIRefID) + ES + _IOIRefID + SOH).getBytes());
            }
            if (_Symbol != null) {
                bs.write((String.valueOf(TAG_Symbol) + ES + _Symbol + SOH).getBytes());
            }
            if (_SymbolSfx != null) {
                bs.write((String.valueOf(TAG_SymbolSfx) + ES + _SymbolSfx + SOH).getBytes());
            }
            if (_SecurityID != null) {
                bs.write((String.valueOf(TAG_SecurityID) + ES + _SecurityID + SOH).getBytes());
            }
            if (_IDSource != null) {
                bs.write((String.valueOf(TAG_IDSource) + ES + _IDSource + SOH).getBytes());
            }
            if (_SecurityType != null) {
                bs.write((String.valueOf(TAG_SecurityType) + ES + _SecurityType + SOH).getBytes());
            }
            if (_MaturityMonthYear != null) {
                bs.write((String.valueOf(TAG_MaturityMonthYear) + ES + _MaturityMonthYear + SOH).getBytes());
            }
            if (_MaturityDay != null) {
                bs.write((String.valueOf(TAG_MaturityDay) + ES + _MaturityDay + SOH).getBytes());
            }
            if (_PutOrCall != null) {
                bs.write((String.valueOf(TAG_PutOrCall) + ES + _PutOrCall + SOH).getBytes());
            }
            if (_StrikePrice != null) {
                bs.write((String.valueOf(TAG_StrikePrice) + ES + _StrikePrice + SOH).getBytes());
            }
            if (_OptAttribute != null) {
                bs.write((String.valueOf(TAG_OptAttribute) + ES + _OptAttribute + SOH).getBytes());
            }
            if (_ContractMultiplier != null) {
                bs.write((String.valueOf(TAG_ContractMultiplier) + ES + _ContractMultiplier + SOH).getBytes());
            }
            if (_CouponRate != null) {
                bs.write((String.valueOf(TAG_CouponRate) + ES + _CouponRate + SOH).getBytes());
            }
            if (_SecurityExchange != null) {
                bs.write((String.valueOf(TAG_SecurityExchange) + ES + _SecurityExchange + SOH).getBytes());
            }
            if (_Issuer != null) {
                bs.write((String.valueOf(TAG_Issuer) + ES + _Issuer + SOH).getBytes());
            }
            if (_EncodedIssuerLen != null) {
                bs.write((String.valueOf(TAG_EncodedIssuerLen) + ES + _EncodedIssuerLen + SOH).getBytes());
            }
            if (_EncodedIssuer != null) {
                bs.write((String.valueOf(TAG_EncodedIssuer) + ES + _EncodedIssuer + SOH).getBytes());
            }
            if (_SecurityDesc != null) {
                bs.write((String.valueOf(TAG_SecurityDesc) + ES + _SecurityDesc + SOH).getBytes());
            }
            if (_EncodedSecurityDescLen != null) {
                bs.write((String.valueOf(TAG_EncodedSecurityDescLen) + ES + _EncodedSecurityDescLen + SOH).getBytes());
            }
            if (_EncodedSecurityDesc != null) {
                bs.write((String.valueOf(TAG_EncodedSecurityDesc) + ES + _EncodedSecurityDesc + SOH).getBytes());
            }
            if (_Side != null) {
                bs.write((String.valueOf(TAG_Side) + ES + _Side + SOH).getBytes());
            }
            if (_IOIShares != null) {
                bs.write((String.valueOf(TAG_IOIShares) + ES + _IOIShares + SOH).getBytes());
            }
            if (_Price != null) {
                bs.write((String.valueOf(TAG_Price) + ES + _Price + SOH).getBytes());
            }
            if (_Currency != null) {
                bs.write((String.valueOf(TAG_Currency) + ES + _Currency + SOH).getBytes());
            }
            if (_ValidUntilTime != null) {
                bs.write((String.valueOf(TAG_ValidUntilTime) + ES + _ValidUntilTime + SOH).getBytes());
            }
            if (_IOIQltyInd != null) {
                bs.write((String.valueOf(TAG_IOIQltyInd) + ES + _IOIQltyInd + SOH).getBytes());
            }
            if (_IOINaturalFlag != null) {
                bs.write((String.valueOf(TAG_IOINaturalFlag) + ES + _IOINaturalFlag + SOH).getBytes());
            }
            if (_NoIOIQualifiers != null) {
                bs.write((String.valueOf(TAG_NoIOIQualifiers) + ES + _NoIOIQualifiers + SOH).getBytes());
            }
            if (_IOIQualifierSeq != null) {
                bs.write(_IOIQualifierSeq.toFIXBytes());
            }
            if (_Text != null) {
                bs.write((String.valueOf(TAG_Text) + ES + _Text + SOH).getBytes());
            }
            if (_EncodedTextLen != null) {
                bs.write((String.valueOf(TAG_EncodedTextLen) + ES + _EncodedTextLen + SOH).getBytes());
            }
            if (_EncodedText != null) {
                bs.write((String.valueOf(TAG_EncodedText) + ES + _EncodedText + SOH).getBytes());
            }
            if (_TransactTime != null) {
                bs.write((String.valueOf(TAG_TransactTime) + ES + _TransactTime + SOH).getBytes());
            }
            if (_URLLink != null) {
                bs.write((String.valueOf(TAG_URLLink) + ES + _URLLink + SOH).getBytes());
            }
            if (_NoRoutingIDs != null) {
                bs.write((String.valueOf(TAG_NoRoutingIDs) + ES + _NoRoutingIDs + SOH).getBytes());
            }
            if (_RoutingIDSeq != null) {
                bs.write(_RoutingIDSeq.toFIXBytes());
            }
            if (_SpreadToBenchmark != null) {
                bs.write((String.valueOf(TAG_SpreadToBenchmark) + ES + _SpreadToBenchmark + SOH).getBytes());
            }
            if (_Benchmark != null) {
                bs.write((String.valueOf(TAG_Benchmark) + ES + _Benchmark + SOH).getBytes());
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
