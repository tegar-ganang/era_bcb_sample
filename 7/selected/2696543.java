package info.gryb.xacml.pdp;

import java.util.Vector;
import info.gryb.schemas.xacml.common.*;
import info.gryb.schemas.xacml.wsdl.*;
import info.gryb.xacml.pdp.Constants;
import os.schema.context._0._2.xacml.tc.names.oasis.*;
import os.schema.policy._0._2.xacml.tc.names.oasis.*;
import java.util.Arrays;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import org.apache.axiom.om.util.Base64;
import org.apache.xalan.lib.ExsltDatetime;

public class XFunction {

    private boolean bVarArgs = false;

    private String sFunctionId = null;

    private Class cReturnType;

    private Class[] cArgTypes;

    private Class cVectorArgType;

    private boolean drillDown = true;

    private XFunction.Invoker invoker;

    public abstract static class Invoker {

        public abstract Object invoke(XFunction xf, Object[] args, RequestType req);
    }

    public XFunction(String id, Class retType, Class[] argTypes, XFunction.Invoker invoker) {
        this.sFunctionId = id;
        this.cReturnType = retType;
        this.cArgTypes = argTypes;
        this.invoker = invoker;
    }

    public XFunction(String id, boolean drill, Class retType, Class[] argTypes, XFunction.Invoker invoker) {
        this.sFunctionId = id;
        this.cReturnType = retType;
        this.cArgTypes = argTypes;
        this.invoker = invoker;
        this.drillDown = drill;
    }

    public XFunction(String id, Class retType, Class[] argTypes, Class vecArgType, XFunction.Invoker invoker) {
        this.sFunctionId = id;
        this.cReturnType = retType;
        this.cArgTypes = argTypes;
        this.invoker = invoker;
        this.cVectorArgType = vecArgType;
    }

    public XFunction(String id, Class retType, Class[] argTypes, boolean bVar, XFunction.Invoker invoker) {
        this.sFunctionId = id;
        this.cReturnType = retType;
        this.cArgTypes = argTypes;
        this.invoker = invoker;
        this.bVarArgs = bVar;
    }

    public void setFunctionId(String sFunctionId) {
        this.sFunctionId = sFunctionId;
    }

    public String getFunctionId() {
        return sFunctionId;
    }

    public void setReturnType(Class cReturnType) {
        this.cReturnType = cReturnType;
    }

    public Class getReturnType() {
        return cReturnType;
    }

    public void setArgTypes(Class[] cArgTypes) {
        this.cArgTypes = cArgTypes;
    }

    public Class[] getArgTypes() {
        return cArgTypes;
    }

    public void setInvoker(XFunction.Invoker invoker) {
        this.invoker = invoker;
    }

    public XFunction.Invoker getInvoker() {
        return invoker;
    }

    public int getLastArgInd(int ind) {
        return this.bVarArgs ? this.cArgTypes.length - 1 : ind;
    }

    public boolean validateParams(Object[] params, int ind, boolean[] disintegrate, boolean[] canHaveNulls) {
        boolean ret = true;
        if (Helper.hasNulls(params, ind, canHaveNulls)) return false;
        if (cArgTypes == null) return true;
        for (int i = ind; i < params.length && getLastArgInd(i - ind) < cArgTypes.length; i++) {
            if (cArgTypes[getLastArgInd(i - ind)] == null || params[i] == null) continue;
            if (Vector.class.isInstance(params[i]) && disintegrate[getLastArgInd(i - ind)]) {
                Vector v = (Vector) params[i];
                if (v.size() == 0 || v.elementAt(0) == null) {
                    if (!canHaveNulls[getLastArgInd(i - ind)]) {
                        ret = false;
                        break;
                    }
                    continue;
                }
                if (v.size() != 1) {
                    ret = false;
                    break;
                }
                if (!cArgTypes[getLastArgInd(i - ind)].isInstance(v.elementAt(0))) {
                    ret = false;
                    break;
                }
            } else {
                if (!cArgTypes[getLastArgInd(i - ind)].isInstance(params[i])) {
                    ret = false;
                    break;
                }
                if (this.cVectorArgType != null) {
                    Vector v = (Vector) params[i];
                    for (int j = 0; j < v.size(); j++) {
                        if (v.elementAt(j) != null && this.cVectorArgType.isInstance(v.elementAt(j))) continue;
                        ret = false;
                        break;
                    }
                    if (!ret) break;
                }
            }
        }
        return ret;
    }

    public Object[] getParams(Object[] params, int ind, boolean disintegrate, boolean canHaveNulls) {
        int nPars = params.length - ind;
        if (nPars <= 0) return null;
        boolean[] scalars = new boolean[nPars];
        boolean[] canHaveNullss = new boolean[nPars];
        for (int i = 0; i < nPars; i++) {
            scalars[i] = disintegrate;
            canHaveNullss[i] = canHaveNulls;
        }
        return getParams(params, ind, scalars, canHaveNullss);
    }

    public Object[] getParams(Object[] params, int ind, boolean disintegrate) {
        return getParams(params, ind, disintegrate, false);
    }

    public Object[] getParams(Object[] params, int ind, boolean[] disintegrate, boolean[] canHaveNulls) {
        if (!validateParams(params, ind, disintegrate, canHaveNulls)) return null;
        Vector v = new Vector();
        for (int i = ind; i < params.length && getLastArgInd(i - ind) < cArgTypes.length; i++) {
            if (disintegrate[getLastArgInd(i - ind)]) {
                if (params[i] != null && Vector.class.isInstance(params[i])) {
                    Vector cv = (Vector) params[i];
                    if (cv.size() > 0) v.add(cv.elementAt(0)); else v.add(null);
                } else {
                    v.add(params[i]);
                }
            } else v.add(params[i]);
        }
        return v.size() > 0 ? v.toArray() : new Object[0];
    }

    public Object[] getBooleanFuncParams(Object[] params, int nScalarInd) {
        if (Helper.hasNulls(params, 1)) return null;
        if (!XFunction.class.isInstance(params[1])) return null;
        XFunction func = (XFunction) params[1];
        if (!func.getReturnType().equals(Boolean.class)) return null;
        Object[] ret = new Object[params.length - 1];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = params[i + 1];
        }
        if (nScalarInd != -1 && nScalarInd != 0 && Vector.class.isInstance(params[nScalarInd])) {
            Vector cv = (Vector) params[nScalarInd];
            if (cv.size() == 0 || cv.elementAt(0) == null) return null;
            ret[nScalarInd - 1] = cv.elementAt(0);
        }
        return ret;
    }

    public Object[] getScalarFuncParams(Object[] params) {
        if (Helper.hasNulls(params, 1)) return null;
        if (!XFunction.class.isInstance(params[1])) return null;
        XFunction func = (XFunction) params[1];
        Class classRet = func.getReturnType();
        if (!isScalarType(classRet) || func.getArgTypes().length != 1) {
            return null;
        }
        Object[] ret = new Object[params.length - 1];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = params[i + 1];
        }
        return ret;
    }

    boolean isScalarType(Class cl) {
        Class[] scClasses = { String.class, Long.class, ComparableBytes.class, Boolean.class, Double.class, java.util.Date.class, Duration.class, java.net.URI.class };
        for (int i = 0; i < scClasses.length; i++) {
            if (cl.equals(scClasses[i])) return true;
        }
        return false;
    }

    public boolean isVarArgs() {
        return bVarArgs;
    }

    public void setVarArgs(boolean varArgs) {
        bVarArgs = varArgs;
    }

    public Class getVectorArgType() {
        return cVectorArgType;
    }

    public void setVectorArgType(Class vectorArgType) {
        cVectorArgType = vectorArgType;
    }

    public boolean isDrillDown() {
        return drillDown;
    }

    public void setDrillDown(boolean drillDown) {
        this.drillDown = drillDown;
    }
}
