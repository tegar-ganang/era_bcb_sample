package xregistry.doc;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import org.xml.sax.InputSource;
import xregistry.SQLConstants;
import xregistry.XregistryConstants;
import xregistry.XregistryException;
import xregistry.XregistryConstants.DocType;
import xregistry.XregistryConstants.SqlParmType;
import xregistry.auth.UserAuthorizer;
import xregistry.context.GlobalContext;
import xregistry.context.SqlParam;
import xregistry.utils.Utils;
import xsul.MLogger;
import edu.indiana.extreme.namespaces.x2004.x01.gFac.ApplicationDescriptionType;
import edu.indiana.extreme.namespaces.x2004.x01.gFac.ServiceMapType;

public class DocumentRegistryImpl implements SQLConstants, DocumentRegistry {

    protected static MLogger log = MLogger.getLogger(XregistryConstants.LOGGER_NAME);

    private final GlobalContext globalContext;

    public DocumentRegistryImpl(final GlobalContext globalContext) {
        super();
        this.globalContext = globalContext;
    }

    public String registerResource(String user, String resourceID, String sql, SqlParam[] sqlParams) throws XregistryException {
        try {
            Connection connection = globalContext.createConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(ADD_RESOURCE_SQL);
                statement.setString(1, resourceID);
                statement.setString(2, user);
                statement.executeUpdate();
                statement = connection.prepareStatement(sql);
                for (int i = 0; i < sqlParams.length; i++) {
                    SqlParam param = sqlParams[i];
                    switch(param.getType()) {
                        case Int:
                            statement.setInt(i + 1, Integer.parseInt(param.getValue()));
                            break;
                        case String:
                            statement.setString(i + 1, param.getValue());
                            break;
                        case Long:
                            statement.setLong(i + 1, Long.parseLong(param.getValue()));
                            break;
                        default:
                            throw new XregistryException("Unknown SQL param type " + param.getType());
                    }
                }
                statement.executeUpdate();
                log.info("Execuate SQL " + statement);
                connection.commit();
                return resourceID;
            } catch (Throwable e) {
                connection.rollback();
                throw new XregistryException(e);
            } finally {
                try {
                    statement.close();
                    connection.setAutoCommit(true);
                    globalContext.closeConnection(connection);
                } catch (SQLException e) {
                    throw new XregistryException(e);
                }
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        }
    }

    public String getResource(String sql, String[] docKeys, String returnedRow) throws XregistryException {
        Connection connection = globalContext.createConnection();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(sql);
            for (int i = 0; i < docKeys.length; i++) {
                statement.setString(i + 1, docKeys[i]);
            }
            log.info("Execuate SQL " + statement);
            results = statement.executeQuery();
            String docToReturn = null;
            if (results.next()) {
                docToReturn = results.getString(returnedRow);
            }
            return docToReturn;
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            try {
                results.close();
                statement.close();
                connection.setAutoCommit(true);
                globalContext.closeConnection(connection);
            } catch (SQLException e) {
                throw new XregistryException(e);
            }
        }
    }

    public String getWithTypeResource(String sql, String[] docKeys, String returnedRow) throws XregistryException {
        Connection connection = globalContext.createConnection();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(sql);
            for (int i = 0; i < docKeys.length; i++) {
                if (i == 0) {
                    statement.setString(i + 1, "%" + docKeys[i] + "%");
                } else {
                    statement.setString(i + 1, docKeys[i]);
                }
            }
            log.info("Execuate SQL " + statement);
            results = statement.executeQuery();
            String docToReturn = null;
            if (results.next()) {
                docToReturn = results.getString(returnedRow);
            }
            return docToReturn;
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            try {
                results.close();
                statement.close();
                connection.setAutoCommit(true);
                globalContext.closeConnection(connection);
            } catch (SQLException e) {
                throw new XregistryException(e);
            }
        }
    }

    public String getResource(String sql, String[] docKeys, String[] returnedRows) throws XregistryException {
        Connection connection = globalContext.createConnection();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(sql);
            for (int i = 0; i < docKeys.length; i++) {
                statement.setString(i + 1, docKeys[i]);
            }
            log.info("Execuate SQL " + statement);
            results = statement.executeQuery();
            String docToReturn = null;
            List<String[]> list = new ArrayList<String[]>();
            while (results.next()) {
                String[] returnValues = new String[returnedRows.length];
                for (int i = 0; i < returnedRows.length; i++) {
                    String rawName = returnedRows[i];
                    returnValues[i] = results.getString(rawName);
                }
                list.add(returnValues);
            }
            return docToReturn;
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            try {
                results.close();
                statement.close();
                connection.setAutoCommit(true);
                globalContext.closeConnection(connection);
            } catch (SQLException e) {
                throw new XregistryException(e);
            }
        }
    }

    public void removeResource(String resourceID, String sql, String[] keys) throws XregistryException {
        try {
            Connection connection = globalContext.createConnection();
            connection.setAutoCommit(false);
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(sql);
                for (int i = 0; i < keys.length; i++) {
                    statement.setString(i + 1, keys[i]);
                }
                statement.executeUpdate();
                statement = connection.prepareStatement(DELETE_RESOURCE_SQL);
                statement.setString(1, resourceID);
                statement.executeUpdate();
                log.info("Execuate SQL " + statement);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new XregistryException(e);
            } finally {
                try {
                    statement.close();
                    connection.setAutoCommit(true);
                    globalContext.closeConnection(connection);
                } catch (SQLException e) {
                    throw new XregistryException(e);
                }
            }
        } catch (SQLException e) {
            throw new XregistryException(e);
        }
    }

    public List<String[]> findResource(String sql, String key, String... returnedRows) throws XregistryException {
        Connection connection = globalContext.createConnection();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, "%" + key + "%");
            log.info("Execuate SQL " + statement);
            results = statement.executeQuery();
            List<String[]> list = new ArrayList<String[]>();
            while (results.next()) {
                String[] returnValues = new String[returnedRows.length];
                for (int i = 0; i < returnedRows.length; i++) {
                    String rawName = returnedRows[i];
                    returnValues[i] = results.getString(rawName);
                }
                list.add(returnValues);
            }
            return list;
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            try {
                results.close();
                statement.close();
                globalContext.closeConnection(connection);
            } catch (SQLException e) {
                throw new XregistryException(e);
            }
        }
    }

    public List<String[]> findResource(String sql, String[] key, String... returnedRows) throws XregistryException {
        Connection connection = globalContext.createConnection();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, "%" + key[0] + "%");
            statement.setString(2, "%" + key[1] + "%");
            if (key.length >= 3) {
                statement.setString(3, key[2]);
            }
            if (key.length == 4) {
                statement.setString(4, key[3]);
            }
            log.info("Execuate SQL " + statement);
            results = statement.executeQuery();
            List<String[]> list = new ArrayList<String[]>();
            while (results.next()) {
                String[] returnValues = new String[returnedRows.length];
                for (int i = 0; i < returnedRows.length; i++) {
                    String rawName = returnedRows[i];
                    returnValues[i] = results.getString(rawName);
                }
                list.add(returnValues);
            }
            return list;
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            try {
                results.close();
                statement.close();
                globalContext.closeConnection(connection);
            } catch (SQLException e) {
                throw new XregistryException(e);
            }
        }
    }

    public String registerHostDesc(String user, String hostDescAsStr) throws XregistryException {
        String hostName = DocParser.parseHostDesc(hostDescAsStr);
        String resourceID = ResourceUtils.getResourceID(DocType.HostDesc, hostName);
        return registerResource(user, resourceID, ADD_HOST_DESC_SQL, new SqlParam[] { new SqlParam(resourceID), new SqlParam(hostName), new SqlParam(hostDescAsStr) });
    }

    public String registerServiceDesc(String user, String serviceDescAsStr, String awsdlAsStr) throws XregistryException {
        ServiceMapType serviceMapType = DocParser.parseServiceDesc(serviceDescAsStr);
        String serviceName = DocParser.getServiceName(serviceMapType.getService().getServiceName()).toString();
        String resourceID = ResourceUtils.getResourceID(DocType.ServiceDesc, serviceName);
        return registerResource(user, resourceID, ADD_SERVICE_DESC_SQL, new SqlParam[] { new SqlParam(resourceID), new SqlParam(serviceName), new SqlParam(serviceDescAsStr), new SqlParam(awsdlAsStr) });
    }

    public String registerAppDesc(String user, String appDescAsStr) throws XregistryException {
        ApplicationDescriptionType appDesc = DocParser.parseAppeDesc(appDescAsStr);
        String appName = DocParser.getAppName(appDesc.getApplicationName()).toString();
        String hostName = appDesc.getDeploymentDescription().getHostName();
        String resourceID = ResourceUtils.getResourceID(DocType.AppDesc, appName, hostName);
        return registerResource(user, resourceID, ADD_APP_DESC_SQL, new SqlParam[] { new SqlParam(resourceID), new SqlParam(appName), new SqlParam(hostName), new SqlParam(appDescAsStr) });
    }

    public void removeServiceDesc(String serviceName) throws XregistryException {
        String resourceID = ResourceUtils.getResourceID(DocType.ServiceDesc, serviceName);
        removeResource(resourceID, DELETE_SERVICE_DESC_SQL, new String[] { serviceName });
    }

    public void removeAppDesc(String appName, String hostName) throws XregistryException {
        String resourceID = ResourceUtils.getResourceID(DocType.AppDesc, appName, hostName);
        removeResource(resourceID, DELETE_APP_DESC_SQL, new String[] { appName, hostName });
    }

    public void removeHostDesc(String hostName) throws XregistryException {
        String resourceID = ResourceUtils.getResourceID(DocType.HostDesc, hostName);
        removeResource(resourceID, DELETE_HOST_DESC_SQL, new String[] { hostName });
    }

    public String getServiceDesc(String serviceName) throws XregistryException {
        return getResource(GET_SERVICE_DESC_SQL, new String[] { serviceName }, SERVICE_DESC_STR);
    }

    public String getAppDesc(String appName, String hostName) throws XregistryException {
        return getResource(GET_APP_DESC_SQL, new String[] { appName, hostName }, APP_DESC_STR);
    }

    public String getHostDesc(String hostName) throws XregistryException {
        return getResource(GET_HOST_DESC_SQL, new String[] { hostName }, HOST_DESC_STR);
    }

    public String registerConcreteWsdl(String user, String wsdlAsStr, int lifetimeAsSeconds) throws XregistryException {
        try {
            if (lifetimeAsSeconds < 60 * 15) {
                lifetimeAsSeconds = 60 * 15;
            }
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
            Definition definition = reader.readWSDL(null, new InputSource(new StringReader(wsdlAsStr)));
            if (definition.getServices() == null) {
                throw new XregistryException("WSDL must be a concreate WSDL, it must have service defined");
            }
            Iterator it = definition.getServices().values().iterator();
            if (it.hasNext()) {
                Service service = (Service) it.next();
                QName wsdlQname = service.getQName();
                QName portTypeName;
                Iterator ports = service.getPorts().values().iterator();
                if (ports.hasNext()) {
                    Port port = (Port) ports.next();
                    portTypeName = port.getBinding().getPortType().getQName();
                } else {
                    throw new XregistryException("WSDL must be a concreate WSDL, it must have port defined");
                }
                String resourceID = ResourceUtils.getResourceID(DocType.CWsdl, wsdlQname.toString());
                removeResource(resourceID, DELETE_CWSDL_SQL, new String[] { wsdlQname.toString() });
                return registerResource(user, resourceID, ADD_CWSDL_SQL, new SqlParam[] { new SqlParam(resourceID), new SqlParam(wsdlQname.toString()), new SqlParam(wsdlAsStr), new SqlParam(String.valueOf(System.currentTimeMillis()), SqlParmType.Long), new SqlParam(String.valueOf(lifetimeAsSeconds * 1000), SqlParmType.Int), new SqlParam(portTypeName.toString()) });
            } else {
                throw new XregistryException("WSDL must be a concreate WSDL, it must have service defined");
            }
        } catch (WSDLException e) {
            throw new XregistryException(e);
        }
    }

    public String getConcreateWsdl(String wsdlQName) throws XregistryException {
        return getResource(GET_CWSDL_SQL, new String[] { wsdlQName + "%", wsdlQName }, CWSDL_STR);
    }

    public void removeConcreteWsdl(String wsdlQName) throws XregistryException {
        String resourceID = ResourceUtils.getResourceID(DocType.CWsdl, wsdlQName);
        removeResource(resourceID, DELETE_CWSDL_SQL, new String[] { wsdlQName });
    }

    public String getAbstractWsdl(String wsdlQName) throws XregistryException {
        return getResource(GET_AWSDL_SQL, new String[] { wsdlQName }, AWSDL_STR);
    }

    public List<DocData> findServiceInstance(String user, String serviceName) throws XregistryException {
        List<String[]> results = findResource(FIND_CWSDL_SQL, serviceName, QNAME, OWNER, RESOURCE_ID);
        List<DocData> returnValues = new ArrayList<DocData>();
        StringBuffer buf = new StringBuffer();
        for (String[] result : results) {
            DocData data = new DocData(QName.valueOf(result[0]), result[1]);
            data.allowedAction = XregistryConstants.Action.Read.toString();
            data.resourceID = new QName(result[2]);
            buf.append(data.name).append(" ");
            returnValues.add(data);
        }
        log.info(new StringBuffer().append("Return Cwsdl:").append(user).append(":").append(serviceName).append("->").append(buf.toString()).toString());
        return returnValues;
    }

    public List<DocData> findServiceDesc(String user, String serviceName) throws XregistryException {
        List<String[]> results = findResource(FIND_SERVICE_DESC_SQL, serviceName, QNAME, OWNER, RESOURCE_ID);
        List<DocData> returnValues = new ArrayList<DocData>();
        for (String[] result : results) {
            String resourceID = ResourceUtils.getResourceID(DocType.ServiceDesc, result[0]);
            String allowedAction = isAccessible(resourceID, result[1], user);
            if (allowedAction != null) {
                DocData data = new DocData(QName.valueOf(result[0]), result[1]);
                data.allowedAction = allowedAction;
                data.resourceID = new QName(result[2]);
                data.resourcename = result[2];
                returnValues.add(data);
            }
        }
        return returnValues;
    }

    public List<AppData> findAppDesc(String user, String query) throws XregistryException {
        List<String[]> results = findResource(FIND_APP_DESC_SQL, query, QNAME, OWNER, HOST_NAME, RESOURCE_ID);
        List<AppData> returnValues = new ArrayList<AppData>();
        for (String[] result : results) {
            String resourceID = ResourceUtils.getResourceID(DocType.AppDesc, result[0], result[2]);
            String allowedAction = isAccessible(resourceID, result[1], user);
            if (allowedAction != null) {
                AppData data = new AppData(QName.valueOf(result[0]), result[1], result[2]);
                data.allowedAction = allowedAction;
                data.resourceID = new QName(result[3]);
                data.resourcename = result[3];
                returnValues.add(data);
            }
        }
        return returnValues;
    }

    public List<DocData> findHosts(String user, String hostName) throws XregistryException {
        List<String[]> results = findResource(FIND_HOST_DESC_SQL, hostName, HOST_NAME, OWNER, RESOURCE_ID);
        List<DocData> returnValues = new ArrayList<DocData>();
        for (String[] result : results) {
            String resourceID = ResourceUtils.getResourceID(DocType.HostDesc, result[0]);
            String allowedAction = isAccessible(resourceID, result[1], user);
            if (allowedAction != null) {
                DocData data = new DocData(QName.valueOf(result[0]), result[1]);
                data.allowedAction = allowedAction;
                data.resourceID = new QName(result[2]);
                data.resourcename = result[2];
                returnValues.add(data);
            }
        }
        return returnValues;
    }

    public String[] app2Hosts(String appName) throws XregistryException {
        Connection connection = globalContext.createConnection();
        PreparedStatement statement = null;
        ResultSet results = null;
        try {
            statement = connection.prepareStatement(GIVEN_APP_FIND_HOSTS_SQL);
            statement.setString(1, appName);
            log.info("Execuate SQL " + statement);
            results = statement.executeQuery();
            ArrayList<String> list = new ArrayList<String>();
            while (results.next()) {
                list.add(results.getString(HOST_NAME));
            }
            return Utils.toStrListToArray(list);
        } catch (SQLException e) {
            throw new XregistryException(e);
        } finally {
            try {
                results.close();
                statement.close();
                globalContext.closeConnection(connection);
            } catch (SQLException e) {
                throw new XregistryException(e);
            }
        }
    }

    private String isAccessible(String resourceID, String owner, String currentUser) {
        if (Utils.isSameDN(owner, currentUser)) {
            return XregistryConstants.Action.All.toString();
        } else {
            UserAuthorizer userAuthorizer = globalContext.getAuthorizer().getAuthorizerForUser(currentUser);
            return userAuthorizer.isAuthorized(resourceID);
        }
    }

    public void registerDocument(String user, QName resourceID, String document) throws XregistryException {
        registerResource(user, resourceID.toString(), ADD_DOC_SQL, new SqlParam[] { new SqlParam(resourceID.toString()), new SqlParam(document) });
    }

    public List<DocData> findDocument(String user, String query) throws XregistryException {
        List<String[]> results = findResource(FIND_DOC_SQL, query, RESOURCE_ID, OWNER);
        List<DocData> returnValues = new ArrayList<DocData>();
        for (String[] result : results) {
            String resourceID = result[0];
            String owner = result[1];
            String allowedAction = isAccessible(resourceID, owner, user);
            if (allowedAction != null) {
                DocData data = new DocData(QName.valueOf(resourceID), owner);
                data.allowedAction = allowedAction;
                data.resourceID = new QName(resourceID);
                data.resourcename = resourceID;
                returnValues.add(data);
            }
        }
        return returnValues;
    }

    public void removeDocument(String user, QName resourceID) throws XregistryException {
        removeResource(resourceID.toString(), DELETE_DOC_SQL, new String[] { resourceID.toString() });
    }

    public String getDocument(String user, QName docName) throws XregistryException {
        return getResource(GET_DOC_SQL, new String[] { docName.toString() }, DOC_STR);
    }

    public void registerOGCEResource(String user, QName resourceID, String resourceName, String resourceType, String resourceDesc, String resoureDocument, String resoureParentTypedID) throws XregistryException {
        String resourceIdString = ResourceUtils.getOGCEResourceID(resourceType, new String[] { resourceID.toString() });
        registerResource(user, resourceIdString, ADD_OGCE_RESOURCE_SQL, new SqlParam[] { new SqlParam(resourceIdString), new SqlParam(resourceName), new SqlParam(resourceType), new SqlParam(resourceDesc), new SqlParam(resoureDocument), new SqlParam(resoureParentTypedID) });
    }

    /**
	 * 
	 */
    public String getOGCEResource(String user, QName resourceID, String resourceType, String resoureParentTypedID) throws XregistryException {
        if (resourceType == null || resourceType == "") {
            return getWithTypeResource(GET_OGCE_RESOURCE_DESC_SQL_WITHOUTTYPE, new String[] { resourceID.getLocalPart() }, OGCE_RESOURCE);
        } else {
            String resourceIdString = ResourceUtils.getOGCEResourceID(resourceType, new String[] { resourceID.toString() });
            return getResource(GET_OGCE_RESOURCE_DESC_SQL, new String[] { resourceIdString, resourceType, resoureParentTypedID }, OGCE_RESOURCE);
        }
    }

    /**
     * 
     */
    public List<DocData> findOGCEResource(String user, String query, String resourceName, String resourceType, String resoureParentTypedID) throws XregistryException {
        String[] keys = null;
        List<String[]> results = null;
        if (resourceType == null || resourceType == "") {
            keys = new String[] { query, resourceName };
            results = findResource(FIND_OGCE_RESOURCE_DESC_SQL_WITHOUTTYPE, keys, RESOURCE_ID, OWNER, OGCE_RESOURCE_CREATED, OGCE_RESOURCE_DESC, OGCE_RESOURCE_NAME);
        } else {
            keys = new String[] { query, resourceName, resourceType, resoureParentTypedID };
            results = findResource(FIND_OGCE_RESOURCE_DESC_SQL, keys, RESOURCE_ID, OWNER, OGCE_RESOURCE_CREATED, OGCE_RESOURCE_DESC, OGCE_RESOURCE_NAME);
        }
        List<DocData> returnValues = new ArrayList<DocData>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
        for (String[] result : results) {
            String resourceID = result[0];
            String owner = result[1];
            String created = result[2];
            String resouceDesc = result[3];
            String resouceName = result[4];
            String allowedAction = isAccessible(resourceID, owner, user);
            if (allowedAction != null) {
                if (resourceID.startsWith("urn:")) {
                    String[] resourceIDArray = resourceID.split(":", 3);
                    if (resourceIDArray.length >= 3) {
                        resourceID = resourceIDArray[2];
                    }
                }
                DocData data = new DocData(QName.valueOf(resourceID), owner);
                data.allowedAction = allowedAction;
                data.resourceID = QName.valueOf(resourceID);
                data.resourcename = resouceName;
                Calendar calendar = Calendar.getInstance();
                Date date = null;
                try {
                    date = dateFormat.parse(created);
                } catch (ParseException e) {
                    log.severe(e.getMessage());
                }
                calendar.setTime(date);
                data.created = calendar;
                data.resourcetype = resourceType;
                data.resourcedesc = resouceDesc;
                returnValues.add(data);
            }
        }
        return returnValues;
    }

    public void removeOGCEResource(String user, QName resourceID, String resourceType) throws XregistryException {
        String resource = ResourceUtils.getOGCEResourceID(resourceType, new String[] { resourceID.toString() });
        removeResource(resource, DELETE_OGCE_RESOURCE_SQL, new String[] { resource, resourceType });
    }
}
