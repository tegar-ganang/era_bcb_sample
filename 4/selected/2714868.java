package tcg.syscontrol.cos;

/**
 * Generated from IDL interface "ICosProcessManager".
 *
 * @author JacORB IDL compiler V 2.3-beta-2, 14-Oct-2006
 * @version generated at 27-Nov-2009 18:09:27
 */
public abstract class ICosProcessManagerPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, tcg.syscontrol.cos.ICosProcessManagerOperations {

    private static final java.util.Hashtable m_opsHash = new java.util.Hashtable();

    static {
        m_opsHash.put("cosResetProcessNumberOfRestart", new java.lang.Integer(0));
        m_opsHash.put("cosSetProcessParams", new java.lang.Integer(1));
        m_opsHash.put("cosProcessStatusChanged", new java.lang.Integer(2));
        m_opsHash.put("cosUnregisterCorbaServer", new java.lang.Integer(3));
        m_opsHash.put("cosProcessGoingToControl", new java.lang.Integer(4));
        m_opsHash.put("cosGetCorbaServer", new java.lang.Integer(5));
        m_opsHash.put("cosGetPeerManager", new java.lang.Integer(6));
        m_opsHash.put("cosProcessTerminating", new java.lang.Integer(7));
        m_opsHash.put("cosSetProcessLogLevelDetail", new java.lang.Integer(8));
        m_opsHash.put("cosGetManagedProcess2", new java.lang.Integer(9));
        m_opsHash.put("cosSetProcessLogLevel", new java.lang.Integer(10));
        m_opsHash.put("cosGetProcessInfo", new java.lang.Integer(11));
        m_opsHash.put("cosGetProcessConfig2", new java.lang.Integer(12));
        m_opsHash.put("cosPoll", new java.lang.Integer(13));
        m_opsHash.put("cosGetActiveManagedProcess2", new java.lang.Integer(14));
        m_opsHash.put("cosUpdateCorbaServerOperationMode", new java.lang.Integer(15));
        m_opsHash.put("cosGetProcessInfo2", new java.lang.Integer(16));
        m_opsHash.put("cosRegisterCorbaServer", new java.lang.Integer(17));
        m_opsHash.put("cosGetParams", new java.lang.Integer(18));
        m_opsHash.put("cosSetLogLevelDetail", new java.lang.Integer(19));
        m_opsHash.put("cosUnregisterPeer", new java.lang.Integer(20));
        m_opsHash.put("cosGetProcessInfoAll", new java.lang.Integer(21));
        m_opsHash.put("cosTerminate", new java.lang.Integer(22));
        m_opsHash.put("cosProcessGoingToMonitor", new java.lang.Integer(23));
        m_opsHash.put("cosRegisterPeer", new java.lang.Integer(24));
        m_opsHash.put("cosKillProcess", new java.lang.Integer(25));
        m_opsHash.put("cosTerminateProcess", new java.lang.Integer(26));
        m_opsHash.put("cosGetActiveCorbaServer", new java.lang.Integer(27));
        m_opsHash.put("cosSynchronizeManagedProcess", new java.lang.Integer(28));
        m_opsHash.put("cosSetProcessOperationMode", new java.lang.Integer(29));
        m_opsHash.put("cosGetCorbaServerOperationMode", new java.lang.Integer(30));
        m_opsHash.put("cosGetActiveManagedProcess", new java.lang.Integer(31));
        m_opsHash.put("cosGetProcessStatusString2", new java.lang.Integer(32));
        m_opsHash.put("cosGetProcessManagerName", new java.lang.Integer(33));
        m_opsHash.put("cosSynchronizeCorbaServer", new java.lang.Integer(34));
        m_opsHash.put("cosGetNumberOfManagedProcesses", new java.lang.Integer(35));
        m_opsHash.put("cosRegisterManagedProcess", new java.lang.Integer(36));
        m_opsHash.put("cosStartProcess", new java.lang.Integer(37));
        m_opsHash.put("cosSetLogLevel", new java.lang.Integer(38));
        m_opsHash.put("cosGetProcessConfig", new java.lang.Integer(39));
        m_opsHash.put("cosGetManagedProcess", new java.lang.Integer(40));
        m_opsHash.put("cosGetProcessStatusString", new java.lang.Integer(41));
    }

    private String[] ids = { "IDL:tcg/syscontrol/cos/ICosProcessManager:1.0" };

    public tcg.syscontrol.cos.ICosProcessManager _this() {
        return tcg.syscontrol.cos.ICosProcessManagerHelper.narrow(_this_object());
    }

    public tcg.syscontrol.cos.ICosProcessManager _this(org.omg.CORBA.ORB orb) {
        return tcg.syscontrol.cos.ICosProcessManagerHelper.narrow(_this_object(orb));
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String method, org.omg.CORBA.portable.InputStream _input, org.omg.CORBA.portable.ResponseHandler handler) throws org.omg.CORBA.SystemException {
        org.omg.CORBA.portable.OutputStream _out = null;
        java.lang.Integer opsIndex = (java.lang.Integer) m_opsHash.get(method);
        if (null == opsIndex) throw new org.omg.CORBA.BAD_OPERATION(method + " not found");
        switch(opsIndex.intValue()) {
            case 0:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    cosResetProcessNumberOfRestart(_arg0);
                    break;
                }
            case 1:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosRunParamStruct[] _arg1 = tcg.syscontrol.cos.CosRunParamSeqHelper.read(_input);
                    _out = handler.createReply();
                    cosSetProcessParams(_arg0, _arg1);
                    break;
                }
            case 2:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosProcessStatusEnum _arg1 = tcg.syscontrol.cos.CosProcessStatusEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosProcessStatusChanged(_arg0, _arg1);
                    break;
                }
            case 3:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    cosUnregisterCorbaServer(_arg0);
                    break;
                }
            case 4:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    _out.write_boolean(cosProcessGoingToControl(_arg0));
                    break;
                }
            case 5:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosMonitoredThreadHelper.write(_out, cosGetCorbaServer(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 6:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    tcg.syscontrol.cos.ICosProcessManagerHelper.write(_out, cosGetPeerManager(_arg0));
                    break;
                }
            case 7:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosTerminationCodeEnum _arg1 = tcg.syscontrol.cos.CosTerminationCodeEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosProcessTerminating(_arg0, _arg1);
                    break;
                }
            case 8:
                {
                    java.lang.String _arg0 = _input.read_string();
                    java.lang.String _arg1 = _input.read_string();
                    tcg.syscontrol.cos.CosLogLevelEnum _arg2 = tcg.syscontrol.cos.CosLogLevelEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSetProcessLogLevelDetail(_arg0, _arg1, _arg2);
                    break;
                }
            case 9:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosManagedProcessHelper.write(_out, cosGetManagedProcess2(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 10:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosLogLevelEnum _arg1 = tcg.syscontrol.cos.CosLogLevelEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSetProcessLogLevel(_arg0, _arg1);
                    break;
                }
            case 11:
                {
                    try {
                        short _arg0 = _input.read_ushort();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.CosProcessRuntimeDataStructHelper.write(_out, cosGetProcessInfo(_arg0));
                    } catch (tcg.syscontrol.cos.CosIndexOutOfBoundException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 12:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.CosProcessDataStructHelper.write(_out, cosGetProcessConfig2(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 13:
                {
                    _out = handler.createReply();
                    cosPoll();
                    break;
                }
            case 14:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosManagedProcessHelper.write(_out, cosGetActiveManagedProcess2(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 15:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosOperationModeEnum _arg1 = tcg.syscontrol.cos.CosOperationModeEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosUpdateCorbaServerOperationMode(_arg0, _arg1);
                    break;
                }
            case 16:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.CosProcessRuntimeDataStructHelper.write(_out, cosGetProcessInfo2(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 17:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        tcg.syscontrol.cos.ICosMonitoredThread _arg1 = tcg.syscontrol.cos.ICosMonitoredThreadHelper.read(_input);
                        _out = handler.createReply();
                        cosRegisterCorbaServer(_arg0, _arg1);
                    } catch (tcg.syscontrol.cos.CosFailedToRegisterException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosFailedToRegisterExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 18:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    tcg.syscontrol.cos.CosRunParamSeqHelper.write(_out, cosGetParams(_arg0));
                    break;
                }
            case 19:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosLogLevelEnum _arg1 = tcg.syscontrol.cos.CosLogLevelEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSetLogLevelDetail(_arg0, _arg1);
                    break;
                }
            case 20:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    cosUnregisterPeer(_arg0);
                    break;
                }
            case 21:
                {
                    _out = handler.createReply();
                    tcg.syscontrol.cos.CosProcessRuntimeDataSeqHelper.write(_out, cosGetProcessInfoAll());
                    break;
                }
            case 22:
                {
                    _out = handler.createReply();
                    cosTerminate();
                    break;
                }
            case 23:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    _out.write_boolean(cosProcessGoingToMonitor(_arg0));
                    break;
                }
            case 24:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        tcg.syscontrol.cos.ICosProcessManager _arg1 = tcg.syscontrol.cos.ICosProcessManagerHelper.read(_input);
                        _out = handler.createReply();
                        cosRegisterPeer(_arg0, _arg1);
                    } catch (tcg.syscontrol.cos.CosFailedToRegisterException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosFailedToRegisterExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 25:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    cosKillProcess(_arg0);
                    break;
                }
            case 26:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    cosTerminateProcess(_arg0);
                    break;
                }
            case 27:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosMonitoredThreadHelper.write(_out, cosGetActiveCorbaServer(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 28:
                {
                    java.lang.String _arg0 = _input.read_string();
                    java.lang.String _arg1 = _input.read_string();
                    short _arg2 = _input.read_short();
                    tcg.syscontrol.cos.CosProcessStatusEnum _arg3 = tcg.syscontrol.cos.CosProcessStatusEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSynchronizeManagedProcess(_arg0, _arg1, _arg2, _arg3);
                    break;
                }
            case 29:
                {
                    java.lang.String _arg0 = _input.read_string();
                    tcg.syscontrol.cos.CosOperationModeEnum _arg1 = tcg.syscontrol.cos.CosOperationModeEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSetProcessOperationMode(_arg0, _arg1);
                    break;
                }
            case 30:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        tcg.syscontrol.cos.CosOperationModeEnumHolder _arg1 = new tcg.syscontrol.cos.CosOperationModeEnumHolder();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosMonitoredThreadHelper.write(_out, cosGetCorbaServerOperationMode(_arg0, _arg1));
                        tcg.syscontrol.cos.CosOperationModeEnumHelper.write(_out, _arg1.value);
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 31:
                {
                    try {
                        short _arg0 = _input.read_ushort();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosManagedProcessHelper.write(_out, cosGetActiveManagedProcess(_arg0));
                    } catch (tcg.syscontrol.cos.CosIndexOutOfBoundException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 32:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        _out.write_string(cosGetProcessStatusString2(_arg0));
                    } catch (tcg.syscontrol.cos.CosUnknownProcessException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosUnknownProcessExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 33:
                {
                    _out = handler.createReply();
                    _out.write_string(cosGetProcessManagerName());
                    break;
                }
            case 34:
                {
                    java.lang.String _arg0 = _input.read_string();
                    java.lang.String _arg1 = _input.read_string();
                    tcg.syscontrol.cos.CosOperationModeEnum _arg2 = tcg.syscontrol.cos.CosOperationModeEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSynchronizeCorbaServer(_arg0, _arg1, _arg2);
                    break;
                }
            case 35:
                {
                    _out = handler.createReply();
                    _out.write_ushort(cosGetNumberOfManagedProcesses());
                    break;
                }
            case 36:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        tcg.syscontrol.cos.CosProcessTypeEnum _arg1 = tcg.syscontrol.cos.CosProcessTypeEnumHelper.read(_input);
                        tcg.syscontrol.cos.ICosManagedProcess _arg2 = tcg.syscontrol.cos.ICosManagedProcessHelper.read(_input);
                        long _arg3 = _input.read_longlong();
                        _out = handler.createReply();
                        cosRegisterManagedProcess(_arg0, _arg1, _arg2, _arg3);
                    } catch (tcg.syscontrol.cos.CosProcessRunningException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosProcessRunningExceptionHelper.write(_out, _ex0);
                    } catch (tcg.syscontrol.cos.CosFailedToRegisterException _ex1) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosFailedToRegisterExceptionHelper.write(_out, _ex1);
                    }
                    break;
                }
            case 37:
                {
                    java.lang.String _arg0 = _input.read_string();
                    _out = handler.createReply();
                    cosStartProcess(_arg0);
                    break;
                }
            case 38:
                {
                    tcg.syscontrol.cos.CosLogLevelEnum _arg0 = tcg.syscontrol.cos.CosLogLevelEnumHelper.read(_input);
                    _out = handler.createReply();
                    cosSetLogLevel(_arg0);
                    break;
                }
            case 39:
                {
                    try {
                        short _arg0 = _input.read_ushort();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.CosProcessDataStructHelper.write(_out, cosGetProcessConfig(_arg0));
                    } catch (tcg.syscontrol.cos.CosIndexOutOfBoundException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 40:
                {
                    try {
                        short _arg0 = _input.read_ushort();
                        _out = handler.createReply();
                        tcg.syscontrol.cos.ICosManagedProcessHelper.write(_out, cosGetManagedProcess(_arg0));
                    } catch (tcg.syscontrol.cos.CosIndexOutOfBoundException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 41:
                {
                    try {
                        short _arg0 = _input.read_ushort();
                        _out = handler.createReply();
                        _out.write_string(cosGetProcessStatusString(_arg0));
                    } catch (tcg.syscontrol.cos.CosIndexOutOfBoundException _ex0) {
                        _out = handler.createExceptionReply();
                        tcg.syscontrol.cos.CosIndexOutOfBoundExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
        }
        return _out;
    }

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] obj_id) {
        return ids;
    }
}
