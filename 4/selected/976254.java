package org.dbe.kb.server.proxyimpl;

import java.io.*;
import java.util.*;
import javax.jmi.reflect.*;
import org.dbe.kb.server.common.KBconnector;
import org.dbe.kb.mdrman.MDRmanager;

/**
 * <p>Service Description Proxy</p>
 * <p>TUC/MUSIC 2004</p>
 * @version 1.0
 */
public class Modelproxy extends KBconnector implements java.io.Serializable {

    RefPackage _refp = null;

    RefObject _refo = null;

    boolean _init = false;

    boolean _newmodel = true;

    int _kbcmd;

    String _packg, _modelObjName, _metamodelExt, _modelPackage, _modelExt;

    public Modelproxy(boolean inits, String serverAddr, String packg, String modelObjName, String metamodelExt, int kbcmd, String modelPackage, String modelExt) {
        _saddr = serverAddr;
        _init = inits;
        _packg = packg;
        _modelObjName = modelObjName;
        _metamodelExt = metamodelExt;
        _kbcmd = kbcmd;
        _modelPackage = modelPackage;
        _modelExt = modelExt;
    }

    public void init() {
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            MDRmanager.setCustomLookupProperty();
            MDRmanager.setMemoryImplProperty();
            MDRmanager.setMetamodelURL(_metamodelExt, _saddr + "?" + _kbcmd + "=true");
            MDRmanager.prepare(_modelPackage, _modelExt);
            if (!_init) MDRmanager.initRep();
            MDRmanager.createExtends();
            _refp = MDRmanager.getModelinst(_modelExt);
            _init = true;
            _MDRsupported = true;
            System.out.println("Metadata Repository Support Detected");
        } catch (java.lang.NoClassDefFoundError ex) {
            System.out.println("No Metadata Repository Support");
            System.out.println("msg:" + ex.getMessage());
            _buffer = new java.io.ByteArrayOutputStream();
            _MDRsupported = false;
            _refp = null;
        }
    }

    public RefObject newModel(Vector v) {
        if (_MDRsupported) {
            clearCache();
            if (_packg == null) _refo = _refp.refClass(_modelObjName).refCreateInstance(v); else _refo = _refp.refPackage(_packg).refClass(_modelObjName).refCreateInstance(v);
            _newmodel = false;
        }
        return _refo;
    }

    public void importModel(String filename) throws java.io.IOException, javax.jmi.xmi.MalformedXMIException {
        importModel(new java.io.FileInputStream(filename));
    }

    public void importModel(InputStream inStream) throws java.io.IOException, javax.jmi.xmi.MalformedXMIException {
        if (_MDRsupported) {
            clearCache();
            MDRmanager.loadModel(inStream, _refp);
            if (_packg == null) _refo = (RefObject) _refp.refClass(_modelObjName).refAllOfType().iterator().next(); else _refo = (RefObject) _refp.refPackage(_packg).refClass(_modelObjName).refAllOfType().iterator().next();
            _newmodel = false;
        } else {
            write(inStream, _buffer);
        }
    }

    public void exportModel(String filename) throws java.io.IOException {
        exportModel(new java.io.FileOutputStream(filename));
    }

    public void exportModel(OutputStream outStream) throws java.io.IOException {
        if (_MDRsupported) {
            MDRmanager.exportXMI(_refp, outStream);
        } else {
            _buffer.writeTo(outStream);
        }
    }

    public java.io.InputStream KBgetModel(String modelName, int cmd) throws IOException, javax.jmi.xmi.MalformedXMIException {
        if (_MDRsupported) {
            clearCache();
            InputStream in = getConnection(cmd, modelName);
            MDRmanager.loadModel(in, _refp);
            try {
                if (_packg == null) _refo = (RefObject) _refp.refClass(_modelObjName).refAllOfType().iterator().next(); else _refo = (RefObject) _refp.refPackage(_packg).refClass(_modelObjName).refAllOfType().iterator().next();
            } catch (Exception ex) {
            }
            ;
            _newmodel = false;
            return null;
        } else {
            _buffer.reset();
            InputStream ins = getConnection(cmd, modelName);
            int x;
            System.out.println("Retrieving...");
            System.out.flush();
            while ((x = ins.read()) != -1) _buffer.write(x);
            _buffer.flush();
            return new ByteArrayInputStream(_buffer.toByteArray());
        }
    }

    public void KBdeleteModel(String modelName, int cmd) throws IOException {
        InputStream in = getConnection(cmd, modelName);
        write(in, System.out);
    }

    public void KBstoreModel(int cmd) throws IOException {
        OutputStream out = postConnection(cmd);
        if (_MDRsupported) {
            MDRmanager.exportXMI(_refp, out);
        } else {
            _buffer.writeTo(out);
        }
        out.flush();
        out.close();
        InputStream in = getResponse();
        int x;
        while ((x = in.read()) != -1) System.out.write(x);
        System.out.flush();
    }

    public InputStream KBsubmitModel(int cmd) throws IOException {
        OutputStream out = postConnection(cmd);
        if (_MDRsupported) {
            MDRmanager.exportXMI(_refp, out);
        } else {
            _buffer.writeTo(out);
        }
        out.flush();
        out.close();
        InputStream in = getResponse();
        return in;
    }

    public java.util.Collection KBlistModels(int cmd) {
        java.util.Collection res = null;
        if (_MDRsupported) {
            clearCache();
            try {
                InputStream in = getConnection(cmd, "true");
                MDRmanager.loadModel(in, _refp);
                in.close();
                if (_packg == null) res = _refp.refClass(_modelObjName).refAllOfType(); else res = _refp.refPackage(_packg).refClass(_modelObjName).refAllOfType();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            _newmodel = false;
        } else {
            throw new java.lang.UnsupportedOperationException("No Metadata Repository Support");
        }
        return res;
    }

    public java.io.InputStream KBlistModelsXMI(int cmd) {
        InputStream in = getConnection(cmd, "true");
        return in;
    }

    public void addToModel(RefAssociation refa, RefObject refo) {
        refa.refAddLink(_refo, refo);
    }

    public void bundleModelElements(RefAssociation refa, RefClass genRefelem) {
        for (Iterator it = genRefelem.refAllOfType().iterator(); it.hasNext(); ) {
            RefObject elm = (RefObject) it.next();
            if (!elm.equals(_refo)) {
                refa.refAddLink(_refo, elm);
            }
        }
    }

    public void clearCache() {
        if (_MDRsupported) {
            if (!_newmodel) {
                _refp.refDelete();
                init();
            }
        } else {
            _buffer.reset();
        }
    }

    public RefPackage getModelPackage() {
        return _refp;
    }

    public void generateModelSpecificJMI(String modelExtName, String directory) {
        MDRmanager.generateJMIinterfaces(modelExtName, directory);
    }

    public void init(org.dbe.toolkit.proxyframework.Workspace wspace) {
    }
}
