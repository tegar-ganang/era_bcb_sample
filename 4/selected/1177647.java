package org.webgraphlab.algorithm;

/**
 *	Generated from IDL definition of interface "AlgorithmComponent"
 *	@author JacORB IDL compiler 
 */
public abstract class AlgorithmComponentPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, org.webgraphlab.algorithm.AlgorithmComponentOperations {

    private static final java.util.Hashtable m_opsHash = new java.util.Hashtable();

    static {
        m_opsHash.put("runNextIteration", new java.lang.Integer(0));
        m_opsHash.put("provide_facet", new java.lang.Integer(1));
        m_opsHash.put("subscribe", new java.lang.Integer(2));
        m_opsHash.put("get_named_publishers", new java.lang.Integer(3));
        m_opsHash.put("disconnect_consumer", new java.lang.Integer(4));
        m_opsHash.put("get_named_consumers", new java.lang.Integer(5));
        m_opsHash.put("get_all_receptacles", new java.lang.Integer(6));
        m_opsHash.put("get_all_emitters", new java.lang.Integer(7));
        m_opsHash.put("get_named_emitters", new java.lang.Integer(8));
        m_opsHash.put("get_primary_key", new java.lang.Integer(9));
        m_opsHash.put("get_named_receptacles", new java.lang.Integer(10));
        m_opsHash.put("loadGraph", new java.lang.Integer(11));
        m_opsHash.put("get_all_consumers", new java.lang.Integer(12));
        m_opsHash.put("get_all_ports", new java.lang.Integer(13));
        m_opsHash.put("get_component_def", new java.lang.Integer(14));
        m_opsHash.put("reset", new java.lang.Integer(15));
        m_opsHash.put("hasNextIteration", new java.lang.Integer(16));
        m_opsHash.put("same_component", new java.lang.Integer(17));
        m_opsHash.put("remove", new java.lang.Integer(18));
        m_opsHash.put("get_connections", new java.lang.Integer(19));
        m_opsHash.put("disconnect", new java.lang.Integer(20));
        m_opsHash.put("connect", new java.lang.Integer(21));
        m_opsHash.put("get_all_facets", new java.lang.Integer(22));
        m_opsHash.put("configuration_complete", new java.lang.Integer(23));
        m_opsHash.put("getParametersFormat", new java.lang.Integer(24));
        m_opsHash.put("get_all_publishers", new java.lang.Integer(25));
        m_opsHash.put("getResultFormat", new java.lang.Integer(26));
        m_opsHash.put("get_consumer", new java.lang.Integer(27));
        m_opsHash.put("getGraphFormat", new java.lang.Integer(28));
        m_opsHash.put("loadParameters", new java.lang.Integer(29));
        m_opsHash.put("get_ccm_home", new java.lang.Integer(30));
        m_opsHash.put("unsubscribe", new java.lang.Integer(31));
        m_opsHash.put("get_named_facets", new java.lang.Integer(32));
        m_opsHash.put("connect_consumer", new java.lang.Integer(33));
    }

    private String[] ids = { "IDL:org/webgraphlab/algorithm/AlgorithmComponent:1.0", "IDL:omg.org/Components/Events:1.0", "IDL:org/webgraphlab/algorithm/GraphAlgorithm:1.0", "IDL:omg.org/Components/Receptacles:1.0", "IDL:org/webgraphlab/algorithm/AlgorithmEssence:1.0", "IDL:omg.org/Components/Navigation:1.0", "IDL:omg.org/Components/CCMObject:1.0" };

    public org.webgraphlab.algorithm.AlgorithmComponent _this() {
        return org.webgraphlab.algorithm.AlgorithmComponentHelper.narrow(_this_object());
    }

    public org.webgraphlab.algorithm.AlgorithmComponent _this(org.omg.CORBA.ORB orb) {
        return org.webgraphlab.algorithm.AlgorithmComponentHelper.narrow(_this_object(orb));
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String method, org.omg.CORBA.portable.InputStream _input, org.omg.CORBA.portable.ResponseHandler handler) throws org.omg.CORBA.SystemException {
        org.omg.CORBA.portable.OutputStream _out = null;
        java.lang.Integer opsIndex = (java.lang.Integer) m_opsHash.get(method);
        if (null == opsIndex) throw new org.omg.CORBA.BAD_OPERATION(method + " not found");
        switch(opsIndex.intValue()) {
            case 0:
                {
                    _out = handler.createReply();
                    _out.write_string(runNextIteration());
                    break;
                }
            case 1:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        _out.write_Object(provide_facet(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 2:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        org.omg.Components.EventConsumerBase _arg1 = org.omg.Components.EventConsumerBaseHelper.read(_input);
                        _out = handler.createReply();
                        ((org.omg.CORBA_2_3.portable.OutputStream) _out).write_value(subscribe(_arg0, _arg1));
                    } catch (org.omg.Components.InvalidConnection _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidConnectionHelper.write(_out, _ex0);
                    } catch (org.omg.Components.ExceededConnectionLimit _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.ExceededConnectionLimitHelper.write(_out, _ex1);
                    } catch (org.omg.Components.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex2);
                    }
                    break;
                }
            case 3:
                {
                    try {
                        java.lang.String[] _arg0 = org.omg.Components.NameListHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.Components.PublisherDescriptionsHelper.write(_out, get_named_publishers(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 4:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        org.omg.Components.EventConsumerBaseHelper.write(_out, disconnect_consumer(_arg0));
                    } catch (org.omg.Components.NoConnection _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.NoConnectionHelper.write(_out, _ex0);
                    } catch (org.omg.Components.InvalidName _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex1);
                    }
                    break;
                }
            case 5:
                {
                    try {
                        java.lang.String[] _arg0 = org.omg.Components.NameListHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.Components.ConsumerDescriptionsHelper.write(_out, get_named_consumers(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 6:
                {
                    _out = handler.createReply();
                    org.omg.Components.ReceptacleDescriptionsHelper.write(_out, get_all_receptacles());
                    break;
                }
            case 7:
                {
                    _out = handler.createReply();
                    org.omg.Components.EmitterDescriptionsHelper.write(_out, get_all_emitters());
                    break;
                }
            case 8:
                {
                    try {
                        java.lang.String[] _arg0 = org.omg.Components.NameListHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.Components.EmitterDescriptionsHelper.write(_out, get_named_emitters(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 9:
                {
                    try {
                        _out = handler.createReply();
                        ((org.omg.CORBA_2_3.portable.OutputStream) _out).write_value(get_primary_key());
                    } catch (org.omg.Components.NoKeyAvailable _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.NoKeyAvailableHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 10:
                {
                    try {
                        java.lang.String[] _arg0 = org.omg.Components.NameListHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.Components.ReceptacleDescriptionsHelper.write(_out, get_named_receptacles(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 11:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        loadGraph(_arg0);
                    } catch (org.webgraphlab.algorithm.GraphParseException _ex0) {
                        _out = handler.createExceptionReply();
                        org.webgraphlab.algorithm.GraphParseExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 12:
                {
                    _out = handler.createReply();
                    org.omg.Components.ConsumerDescriptionsHelper.write(_out, get_all_consumers());
                    break;
                }
            case 13:
                {
                    _out = handler.createReply();
                    ((org.omg.CORBA_2_3.portable.OutputStream) _out).write_value(get_all_ports());
                    break;
                }
            case 14:
                {
                    _out = handler.createReply();
                    org.omg.CORBA.IRObjectHelper.write(_out, get_component_def());
                    break;
                }
            case 15:
                {
                    _out = handler.createReply();
                    reset();
                    break;
                }
            case 16:
                {
                    _out = handler.createReply();
                    _out.write_boolean(hasNextIteration());
                    break;
                }
            case 17:
                {
                    org.omg.CORBA.Object _arg0 = _input.read_Object();
                    _out = handler.createReply();
                    _out.write_boolean(same_component(_arg0));
                    break;
                }
            case 18:
                {
                    try {
                        _out = handler.createReply();
                        remove();
                    } catch (org.omg.Components.RemoveFailure _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.RemoveFailureHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 19:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        org.omg.Components.ConnectionDescriptionsHelper.write(_out, get_connections(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 20:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        org.omg.Components.Cookie _arg1 = (org.omg.Components.Cookie) ((org.omg.CORBA_2_3.portable.InputStream) _input).read_value("IDL:omg.org/Components/Cookie:1.0");
                        _out = handler.createReply();
                        _out.write_Object(disconnect(_arg0, _arg1));
                    } catch (org.omg.Components.CookieRequired _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.CookieRequiredHelper.write(_out, _ex0);
                    } catch (org.omg.Components.NoConnection _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.NoConnectionHelper.write(_out, _ex1);
                    } catch (org.omg.Components.InvalidConnection _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidConnectionHelper.write(_out, _ex2);
                    } catch (org.omg.Components.InvalidName _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex3);
                    }
                    break;
                }
            case 21:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        org.omg.CORBA.Object _arg1 = _input.read_Object();
                        _out = handler.createReply();
                        ((org.omg.CORBA_2_3.portable.OutputStream) _out).write_value(connect(_arg0, _arg1));
                    } catch (org.omg.Components.AlreadyConnected _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.AlreadyConnectedHelper.write(_out, _ex0);
                    } catch (org.omg.Components.InvalidConnection _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidConnectionHelper.write(_out, _ex1);
                    } catch (org.omg.Components.ExceededConnectionLimit _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.ExceededConnectionLimitHelper.write(_out, _ex2);
                    } catch (org.omg.Components.InvalidName _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex3);
                    }
                    break;
                }
            case 22:
                {
                    _out = handler.createReply();
                    org.omg.Components.FacetDescriptionsHelper.write(_out, get_all_facets());
                    break;
                }
            case 23:
                {
                    try {
                        _out = handler.createReply();
                        configuration_complete();
                    } catch (org.omg.Components.InvalidConfiguration _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidConfigurationHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 24:
                {
                    _out = handler.createReply();
                    _out.write_string(getParametersFormat());
                    break;
                }
            case 25:
                {
                    _out = handler.createReply();
                    org.omg.Components.PublisherDescriptionsHelper.write(_out, get_all_publishers());
                    break;
                }
            case 26:
                {
                    _out = handler.createReply();
                    _out.write_string(getResultFormat());
                    break;
                }
            case 27:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        org.omg.Components.EventConsumerBaseHelper.write(_out, get_consumer(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 28:
                {
                    _out = handler.createReply();
                    _out.write_string(getGraphFormat());
                    break;
                }
            case 29:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        _out = handler.createReply();
                        loadParameters(_arg0);
                    } catch (org.webgraphlab.algorithm.ParametersParseException _ex0) {
                        _out = handler.createExceptionReply();
                        org.webgraphlab.algorithm.ParametersParseExceptionHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 30:
                {
                    _out = handler.createReply();
                    org.omg.Components.CCMHomeHelper.write(_out, get_ccm_home());
                    break;
                }
            case 31:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        org.omg.Components.Cookie _arg1 = (org.omg.Components.Cookie) ((org.omg.CORBA_2_3.portable.InputStream) _input).read_value("IDL:omg.org/Components/Cookie:1.0");
                        _out = handler.createReply();
                        org.omg.Components.EventConsumerBaseHelper.write(_out, unsubscribe(_arg0, _arg1));
                    } catch (org.omg.Components.InvalidConnection _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidConnectionHelper.write(_out, _ex0);
                    } catch (org.omg.Components.InvalidName _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex1);
                    }
                    break;
                }
            case 32:
                {
                    try {
                        java.lang.String[] _arg0 = org.omg.Components.NameListHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.Components.FacetDescriptionsHelper.write(_out, get_named_facets(_arg0));
                    } catch (org.omg.Components.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 33:
                {
                    try {
                        java.lang.String _arg0 = _input.read_string();
                        org.omg.Components.EventConsumerBase _arg1 = org.omg.Components.EventConsumerBaseHelper.read(_input);
                        _out = handler.createReply();
                        connect_consumer(_arg0, _arg1);
                    } catch (org.omg.Components.AlreadyConnected _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.AlreadyConnectedHelper.write(_out, _ex0);
                    } catch (org.omg.Components.InvalidConnection _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidConnectionHelper.write(_out, _ex1);
                    } catch (org.omg.Components.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.Components.InvalidNameHelper.write(_out, _ex2);
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
