package tcg.syscontrol.cos;

/**
 * Generated from IDL interface "ICosProcessManager".
 *
 * @author JacORB IDL compiler V 2.3-beta-2, 14-Oct-2006
 * @version generated at 27-Nov-2009 18:09:27
 */
public class _ICosProcessManagerStub extends org.omg.CORBA.portable.ObjectImpl implements tcg.syscontrol.cos.ICosProcessManager {

    private String[] ids = { "IDL:tcg/syscontrol/cos/ICosProcessManager:1.0" };

    public String[] _ids() {
        return ids;
    }

    public static final java.lang.Class _opsClass = tcg.syscontrol.cos.ICosProcessManagerOperations.class;

    public void cosResetProcessNumberOfRestart(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosResetProcessNumberOfRestart", false);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosResetProcessNumberOfRestart", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosResetProcessNumberOfRestart(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosSetProcessParams(java.lang.String entity, tcg.syscontrol.cos.CosRunParamStruct[] paramSeq) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSetProcessParams", false);
                    _os.write_string(entity);
                    tcg.syscontrol.cos.CosRunParamSeqHelper.write(_os, paramSeq);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSetProcessParams", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSetProcessParams(entity, paramSeq);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosProcessStatusChanged(java.lang.String entity, tcg.syscontrol.cos.CosProcessStatusEnum status) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosProcessStatusChanged", true);
                    _os.write_string(entity);
                    tcg.syscontrol.cos.CosProcessStatusEnumHelper.write(_os, status);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosProcessStatusChanged", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosProcessStatusChanged(entity, status);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosUnregisterCorbaServer(java.lang.String uniqueKey) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosUnregisterCorbaServer", true);
                    _os.write_string(uniqueKey);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosUnregisterCorbaServer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosUnregisterCorbaServer(uniqueKey);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public boolean cosProcessGoingToControl(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosProcessGoingToControl", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    boolean _result = _is.read_boolean();
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosProcessGoingToControl", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                boolean _result;
                try {
                    _result = _localServant.cosProcessGoingToControl(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public tcg.syscontrol.cos.ICosMonitoredThread cosGetCorbaServer(java.lang.String uniqueKey) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetCorbaServer", true);
                    _os.write_string(uniqueKey);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosMonitoredThread _result = tcg.syscontrol.cos.ICosMonitoredThreadHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetCorbaServer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosMonitoredThread _result;
                try {
                    _result = _localServant.cosGetCorbaServer(uniqueKey);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public tcg.syscontrol.cos.ICosProcessManager cosGetPeerManager(java.lang.String peerName) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetPeerManager", true);
                    _os.write_string(peerName);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosProcessManager _result = tcg.syscontrol.cos.ICosProcessManagerHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetPeerManager", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosProcessManager _result;
                try {
                    _result = _localServant.cosGetPeerManager(peerName);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosProcessTerminating(java.lang.String entity, tcg.syscontrol.cos.CosTerminationCodeEnum p_code) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosProcessTerminating", true);
                    _os.write_string(entity);
                    tcg.syscontrol.cos.CosTerminationCodeEnumHelper.write(_os, p_code);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosProcessTerminating", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosProcessTerminating(entity, p_code);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosSetProcessLogLevelDetail(java.lang.String entity, java.lang.String logger, tcg.syscontrol.cos.CosLogLevelEnum loglevel) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSetProcessLogLevelDetail", false);
                    _os.write_string(entity);
                    _os.write_string(logger);
                    tcg.syscontrol.cos.CosLogLevelEnumHelper.write(_os, loglevel);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSetProcessLogLevelDetail", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSetProcessLogLevelDetail(entity, logger, loglevel);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.ICosManagedProcess cosGetManagedProcess2(java.lang.String entity) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetManagedProcess2", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosManagedProcess _result = tcg.syscontrol.cos.ICosManagedProcessHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetManagedProcess2", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosManagedProcess _result;
                try {
                    _result = _localServant.cosGetManagedProcess2(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosSetProcessLogLevel(java.lang.String entity, tcg.syscontrol.cos.CosLogLevelEnum loglevel) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSetProcessLogLevel", false);
                    _os.write_string(entity);
                    tcg.syscontrol.cos.CosLogLevelEnumHelper.write(_os, loglevel);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSetProcessLogLevel", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSetProcessLogLevel(entity, loglevel);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.CosProcessRuntimeDataStruct cosGetProcessInfo(short index) throws tcg.syscontrol.cos.CosIndexOutOfBoundException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessInfo", true);
                    _os.write_ushort(index);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.CosProcessRuntimeDataStruct _result = tcg.syscontrol.cos.CosProcessRuntimeDataStructHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosIndexOutOfBoundException:1.0")) {
                        throw tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessInfo", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.CosProcessRuntimeDataStruct _result;
                try {
                    _result = _localServant.cosGetProcessInfo(index);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public tcg.syscontrol.cos.CosProcessDataStruct cosGetProcessConfig2(java.lang.String entity) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessConfig2", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.CosProcessDataStruct _result = tcg.syscontrol.cos.CosProcessDataStructHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessConfig2", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.CosProcessDataStruct _result;
                try {
                    _result = _localServant.cosGetProcessConfig2(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosPoll() {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosPoll", true);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosPoll", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosPoll();
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.ICosManagedProcess cosGetActiveManagedProcess2(java.lang.String entity) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetActiveManagedProcess2", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosManagedProcess _result = tcg.syscontrol.cos.ICosManagedProcessHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetActiveManagedProcess2", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosManagedProcess _result;
                try {
                    _result = _localServant.cosGetActiveManagedProcess2(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosUpdateCorbaServerOperationMode(java.lang.String uniqueKey, tcg.syscontrol.cos.CosOperationModeEnum operationMode) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosUpdateCorbaServerOperationMode", true);
                    _os.write_string(uniqueKey);
                    tcg.syscontrol.cos.CosOperationModeEnumHelper.write(_os, operationMode);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosUpdateCorbaServerOperationMode", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosUpdateCorbaServerOperationMode(uniqueKey, operationMode);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.CosProcessRuntimeDataStruct cosGetProcessInfo2(java.lang.String entity) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessInfo2", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.CosProcessRuntimeDataStruct _result = tcg.syscontrol.cos.CosProcessRuntimeDataStructHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessInfo2", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.CosProcessRuntimeDataStruct _result;
                try {
                    _result = _localServant.cosGetProcessInfo2(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosRegisterCorbaServer(java.lang.String uniqueKey, tcg.syscontrol.cos.ICosMonitoredThread monitoredThread) throws tcg.syscontrol.cos.CosFailedToRegisterException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosRegisterCorbaServer", true);
                    _os.write_string(uniqueKey);
                    tcg.syscontrol.cos.ICosMonitoredThreadHelper.write(_os, monitoredThread);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosFailedToRegisterException:1.0")) {
                        throw tcg.syscontrol.cos.CosFailedToRegisterExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosRegisterCorbaServer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosRegisterCorbaServer(uniqueKey, monitoredThread);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.CosRunParamStruct[] cosGetParams(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetParams", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.CosRunParamStruct[] _result = tcg.syscontrol.cos.CosRunParamSeqHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetParams", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.CosRunParamStruct[] _result;
                try {
                    _result = _localServant.cosGetParams(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosSetLogLevelDetail(java.lang.String logger, tcg.syscontrol.cos.CosLogLevelEnum loglevel) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSetLogLevelDetail", false);
                    _os.write_string(logger);
                    tcg.syscontrol.cos.CosLogLevelEnumHelper.write(_os, loglevel);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSetLogLevelDetail", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSetLogLevelDetail(logger, loglevel);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosUnregisterPeer(java.lang.String peerName) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosUnregisterPeer", true);
                    _os.write_string(peerName);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosUnregisterPeer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosUnregisterPeer(peerName);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.CosProcessRuntimeDataStruct[] cosGetProcessInfoAll() {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessInfoAll", true);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.CosProcessRuntimeDataStruct[] _result = tcg.syscontrol.cos.CosProcessRuntimeDataSeqHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessInfoAll", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.CosProcessRuntimeDataStruct[] _result;
                try {
                    _result = _localServant.cosGetProcessInfoAll();
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosTerminate() {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosTerminate", false);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosTerminate", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosTerminate();
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public boolean cosProcessGoingToMonitor(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosProcessGoingToMonitor", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    boolean _result = _is.read_boolean();
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosProcessGoingToMonitor", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                boolean _result;
                try {
                    _result = _localServant.cosProcessGoingToMonitor(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosRegisterPeer(java.lang.String peerName, tcg.syscontrol.cos.ICosProcessManager p_peerManager) throws tcg.syscontrol.cos.CosFailedToRegisterException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosRegisterPeer", true);
                    _os.write_string(peerName);
                    tcg.syscontrol.cos.ICosProcessManagerHelper.write(_os, p_peerManager);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosFailedToRegisterException:1.0")) {
                        throw tcg.syscontrol.cos.CosFailedToRegisterExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosRegisterPeer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosRegisterPeer(peerName, p_peerManager);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosKillProcess(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosKillProcess", false);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosKillProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosKillProcess(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosTerminateProcess(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosTerminateProcess", false);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosTerminateProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosTerminateProcess(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.ICosMonitoredThread cosGetActiveCorbaServer(java.lang.String uniqueKey) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetActiveCorbaServer", true);
                    _os.write_string(uniqueKey);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosMonitoredThread _result = tcg.syscontrol.cos.ICosMonitoredThreadHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetActiveCorbaServer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosMonitoredThread _result;
                try {
                    _result = _localServant.cosGetActiveCorbaServer(uniqueKey);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosSynchronizeManagedProcess(java.lang.String peerName, java.lang.String entity, short weightage, tcg.syscontrol.cos.CosProcessStatusEnum status) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSynchronizeManagedProcess", true);
                    _os.write_string(peerName);
                    _os.write_string(entity);
                    _os.write_short(weightage);
                    tcg.syscontrol.cos.CosProcessStatusEnumHelper.write(_os, status);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSynchronizeManagedProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSynchronizeManagedProcess(peerName, entity, weightage, status);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosSetProcessOperationMode(java.lang.String entity, tcg.syscontrol.cos.CosOperationModeEnum mode) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSetProcessOperationMode", false);
                    _os.write_string(entity);
                    tcg.syscontrol.cos.CosOperationModeEnumHelper.write(_os, mode);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSetProcessOperationMode", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSetProcessOperationMode(entity, mode);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.ICosMonitoredThread cosGetCorbaServerOperationMode(java.lang.String uniqueKey, tcg.syscontrol.cos.CosOperationModeEnumHolder operationMode) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetCorbaServerOperationMode", true);
                    _os.write_string(uniqueKey);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosMonitoredThread _result = tcg.syscontrol.cos.ICosMonitoredThreadHelper.read(_is);
                    operationMode.value = tcg.syscontrol.cos.CosOperationModeEnumHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetCorbaServerOperationMode", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosMonitoredThread _result;
                try {
                    _result = _localServant.cosGetCorbaServerOperationMode(uniqueKey, operationMode);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public tcg.syscontrol.cos.ICosManagedProcess cosGetActiveManagedProcess(short index) throws tcg.syscontrol.cos.CosIndexOutOfBoundException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetActiveManagedProcess", true);
                    _os.write_ushort(index);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosManagedProcess _result = tcg.syscontrol.cos.ICosManagedProcessHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosIndexOutOfBoundException:1.0")) {
                        throw tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetActiveManagedProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosManagedProcess _result;
                try {
                    _result = _localServant.cosGetActiveManagedProcess(index);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public java.lang.String cosGetProcessStatusString2(java.lang.String entity) throws tcg.syscontrol.cos.CosUnknownProcessException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessStatusString2", true);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    java.lang.String _result = _is.read_string();
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosUnknownProcessException:1.0")) {
                        throw tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessStatusString2", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                java.lang.String _result;
                try {
                    _result = _localServant.cosGetProcessStatusString2(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public java.lang.String cosGetProcessManagerName() {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessManagerName", true);
                    _is = _invoke(_os);
                    java.lang.String _result = _is.read_string();
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessManagerName", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                java.lang.String _result;
                try {
                    _result = _localServant.cosGetProcessManagerName();
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosSynchronizeCorbaServer(java.lang.String peerName, java.lang.String uniqueKey, tcg.syscontrol.cos.CosOperationModeEnum mode) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSynchronizeCorbaServer", true);
                    _os.write_string(peerName);
                    _os.write_string(uniqueKey);
                    tcg.syscontrol.cos.CosOperationModeEnumHelper.write(_os, mode);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSynchronizeCorbaServer", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSynchronizeCorbaServer(peerName, uniqueKey, mode);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public short cosGetNumberOfManagedProcesses() {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetNumberOfManagedProcesses", true);
                    _is = _invoke(_os);
                    short _result = _is.read_ushort();
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetNumberOfManagedProcesses", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                short _result;
                try {
                    _result = _localServant.cosGetNumberOfManagedProcesses();
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public void cosRegisterManagedProcess(java.lang.String entity, tcg.syscontrol.cos.CosProcessTypeEnum p_processType, tcg.syscontrol.cos.ICosManagedProcess p_managedProcess, long p_processId) throws tcg.syscontrol.cos.CosProcessRunningException, tcg.syscontrol.cos.CosFailedToRegisterException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosRegisterManagedProcess", true);
                    _os.write_string(entity);
                    tcg.syscontrol.cos.CosProcessTypeEnumHelper.write(_os, p_processType);
                    tcg.syscontrol.cos.ICosManagedProcessHelper.write(_os, p_managedProcess);
                    _os.write_longlong(p_processId);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosProcessRunningException:1.0")) {
                        throw tcg.syscontrol.cos.CosProcessRunningExceptionHelper.read(_ax.getInputStream());
                    } else if (_id.equals("IDL:tcg/syscontrol/cos/CosFailedToRegisterException:1.0")) {
                        throw tcg.syscontrol.cos.CosFailedToRegisterExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosRegisterManagedProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosRegisterManagedProcess(entity, p_processType, p_managedProcess, p_processId);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosStartProcess(java.lang.String entity) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosStartProcess", false);
                    _os.write_string(entity);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosStartProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosStartProcess(entity);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public void cosSetLogLevel(tcg.syscontrol.cos.CosLogLevelEnum loglevel) {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosSetLogLevel", false);
                    tcg.syscontrol.cos.CosLogLevelEnumHelper.write(_os, loglevel);
                    _is = _invoke(_os);
                    return;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosSetLogLevel", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                try {
                    _localServant.cosSetLogLevel(loglevel);
                } finally {
                    _servant_postinvoke(_so);
                }
                return;
            }
        }
    }

    public tcg.syscontrol.cos.CosProcessDataStruct cosGetProcessConfig(short index) throws tcg.syscontrol.cos.CosIndexOutOfBoundException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessConfig", true);
                    _os.write_ushort(index);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.CosProcessDataStruct _result = tcg.syscontrol.cos.CosProcessDataStructHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosIndexOutOfBoundException:1.0")) {
                        throw tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessConfig", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.CosProcessDataStruct _result;
                try {
                    _result = _localServant.cosGetProcessConfig(index);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public tcg.syscontrol.cos.ICosManagedProcess cosGetManagedProcess(short index) throws tcg.syscontrol.cos.CosIndexOutOfBoundException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetManagedProcess", true);
                    _os.write_ushort(index);
                    _is = _invoke(_os);
                    tcg.syscontrol.cos.ICosManagedProcess _result = tcg.syscontrol.cos.ICosManagedProcessHelper.read(_is);
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosIndexOutOfBoundException:1.0")) {
                        throw tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetManagedProcess", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                tcg.syscontrol.cos.ICosManagedProcess _result;
                try {
                    _result = _localServant.cosGetManagedProcess(index);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }

    public java.lang.String cosGetProcessStatusString(short index) throws tcg.syscontrol.cos.CosIndexOutOfBoundException {
        while (true) {
            if (!this._is_local()) {
                org.omg.CORBA.portable.InputStream _is = null;
                try {
                    org.omg.CORBA.portable.OutputStream _os = _request("cosGetProcessStatusString", true);
                    _os.write_ushort(index);
                    _is = _invoke(_os);
                    java.lang.String _result = _is.read_string();
                    return _result;
                } catch (org.omg.CORBA.portable.RemarshalException _rx) {
                } catch (org.omg.CORBA.portable.ApplicationException _ax) {
                    String _id = _ax.getId();
                    if (_id.equals("IDL:tcg/syscontrol/cos/CosIndexOutOfBoundException:1.0")) {
                        throw tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.read(_ax.getInputStream());
                    }
                    throw new RuntimeException("Unexpected exception " + _id);
                } finally {
                    this._releaseReply(_is);
                }
            } else {
                org.omg.CORBA.portable.ServantObject _so = _servant_preinvoke("cosGetProcessStatusString", _opsClass);
                if (_so == null) throw new org.omg.CORBA.UNKNOWN("local invocations not supported!");
                ICosProcessManagerOperations _localServant = (ICosProcessManagerOperations) _so.servant;
                java.lang.String _result;
                try {
                    _result = _localServant.cosGetProcessStatusString(index);
                } finally {
                    _servant_postinvoke(_so);
                }
                return _result;
            }
        }
    }
}
