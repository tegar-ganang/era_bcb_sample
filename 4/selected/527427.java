package shu.cms.measure.meterapi.ca210api;

import com.jacob.com.*;

public class IMemory extends Dispatch {

    public static final String componentName = "CA200SRVR.IMemory";

    public IMemory() {
        super(componentName);
    }

    /**
   * This constructor is used instead of a case operation to
   * turn a Dispatch object into a wider object - it must exist
   * in every wrapper class whose instances may be returned from
   * method calls wrapped in VT_DISPATCH Variants.
   */
    public IMemory(Dispatch d) {
        m_pDispatch = d.m_pDispatch;
        d.m_pDispatch = 0;
    }

    public IMemory(String compName) {
        super(compName);
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @return the result is of type int
   */
    public int getChannelNO() {
        return Dispatch.get(this, "ChannelNO").toInt();
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param lastParam an input-parameter of type int
   */
    public void setChannelNO(int lastParam) {
        Dispatch.call(this, "ChannelNO", new Variant(lastParam));
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @return the result is of type String
   */
    public String getChannelID() {
        return Dispatch.get(this, "ChannelID").toString();
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param probeIDVal an input-parameter of type String
   * @param xVal an input-parameter of type float
   * @param yVal an input-parameter of type float
   * @param lastParam an input-parameter of type float
   */
    public void getReferenceColor(String probeIDVal, float xVal, float yVal, float lastParam) {
        Dispatch.call(this, "GetReferenceColor", probeIDVal, new Variant(xVal), new Variant(yVal), new Variant(lastParam));
    }

    /**
   * Wrapper for calling the ActiveX-Method and receiving the output-parameter(s).
   * @param probeIDVal an input-parameter of type String
   * @param xVal is an one-element array which sends the input-parameter
   *             to the ActiveX-Component and receives the output-parameter
   * @param yVal is an one-element array which sends the input-parameter
   *             to the ActiveX-Component and receives the output-parameter
   * @param lastParam is an one-element array which sends the input-parameter
   *                  to the ActiveX-Component and receives the output-parameter
   */
    public void getReferenceColor(String probeIDVal, float[] xVal, float[] yVal, float[] lastParam) {
        Variant vnt_xVal = new Variant();
        if (xVal == null || xVal.length == 0) {
            vnt_xVal.noParam();
        } else {
            vnt_xVal.putFloatRef(xVal[0]);
        }
        Variant vnt_yVal = new Variant();
        if (yVal == null || yVal.length == 0) {
            vnt_yVal.noParam();
        } else {
            vnt_yVal.putFloatRef(yVal[0]);
        }
        Variant vnt_lastParam = new Variant();
        if (lastParam == null || lastParam.length == 0) {
            vnt_lastParam.noParam();
        } else {
            vnt_lastParam.putFloatRef(lastParam[0]);
        }
        Dispatch.call(this, "GetReferenceColor", probeIDVal, vnt_xVal, vnt_yVal, vnt_lastParam);
        if (xVal != null && xVal.length > 0) {
            xVal[0] = vnt_xVal.toFloat();
        }
        if (yVal != null && yVal.length > 0) {
            yVal[0] = vnt_yVal.toFloat();
        }
        if (lastParam != null && lastParam.length > 0) {
            lastParam[0] = vnt_lastParam.toFloat();
        }
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param lastParam an input-parameter of type String
   */
    public void setChannelID(String lastParam) {
        Dispatch.call(this, "SetChannelID", lastParam);
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param probeNOVal an input-parameter of type int
   * @param calProbeSNOVal an input-parameter of type int
   * @param refProbeSNOVal an input-parameter of type int
   * @param lastParam an input-parameter of type int
   */
    public void getMemoryStatus(int probeNOVal, int calProbeSNOVal, int refProbeSNOVal, int lastParam) {
        Dispatch.call(this, "GetMemoryStatus", new Variant(probeNOVal), new Variant(calProbeSNOVal), new Variant(refProbeSNOVal), new Variant(lastParam));
    }

    /**
   * Wrapper for calling the ActiveX-Method and receiving the output-parameter(s).
   * @param probeNOVal an input-parameter of type int
   * @param calProbeSNOVal is an one-element array which sends the input-parameter
   *                       to the ActiveX-Component and receives the output-parameter
   * @param refProbeSNOVal is an one-element array which sends the input-parameter
   *                       to the ActiveX-Component and receives the output-parameter
   * @param lastParam is an one-element array which sends the input-parameter
   *                  to the ActiveX-Component and receives the output-parameter
   */
    public void getMemoryStatus(int probeNOVal, int[] calProbeSNOVal, int[] refProbeSNOVal, int[] lastParam) {
        Variant vnt_calProbeSNOVal = new Variant();
        if (calProbeSNOVal == null || calProbeSNOVal.length == 0) {
            vnt_calProbeSNOVal.noParam();
        } else {
            vnt_calProbeSNOVal.putIntRef(calProbeSNOVal[0]);
        }
        Variant vnt_refProbeSNOVal = new Variant();
        if (refProbeSNOVal == null || refProbeSNOVal.length == 0) {
            vnt_refProbeSNOVal.noParam();
        } else {
            vnt_refProbeSNOVal.putIntRef(refProbeSNOVal[0]);
        }
        Variant vnt_lastParam = new Variant();
        if (lastParam == null || lastParam.length == 0) {
            vnt_lastParam.noParam();
        } else {
            vnt_lastParam.putIntRef(lastParam[0]);
        }
        Dispatch.call(this, "GetMemoryStatus", new Variant(probeNOVal), vnt_calProbeSNOVal, vnt_refProbeSNOVal, vnt_lastParam);
        if (calProbeSNOVal != null && calProbeSNOVal.length > 0) {
            calProbeSNOVal[0] = vnt_calProbeSNOVal.toInt();
        }
        if (refProbeSNOVal != null && refProbeSNOVal.length > 0) {
            refProbeSNOVal[0] = vnt_refProbeSNOVal.toInt();
        }
        if (lastParam != null && lastParam.length > 0) {
            lastParam[0] = vnt_lastParam.toInt();
        }
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param probeNOVal an input-parameter of type int
   * @param lastParam an input-parameter of type String
   * @return the result is of type int
   */
    public int checkCalData(int probeNOVal, String lastParam) {
        return Dispatch.call(this, "CheckCalData", new Variant(probeNOVal), lastParam).toInt();
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param probeNOVal an input-parameter of type int
   * @param lastParam an input-parameter of type String
   */
    public void copyToFile(int probeNOVal, String lastParam) {
        Dispatch.call(this, "CopyToFile", new Variant(probeNOVal), lastParam);
    }

    /**
   * Wrapper for calling the ActiveX-Method with input-parameter(s).
   * @param probeNOVal an input-parameter of type int
   * @param lastParam an input-parameter of type String
   */
    public void copyFromFile(int probeNOVal, String lastParam) {
        Dispatch.call(this, "CopyFromFile", new Variant(probeNOVal), lastParam);
    }
}
