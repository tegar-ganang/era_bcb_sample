package net.sf.mustang.db;

import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import net.sf.mustang.K;
import net.sf.mustang.Mustang;
import net.sf.mustang.bean.Bean;
import net.sf.mustang.bean.BeanDescriptor;
import net.sf.mustang.bean.PropertyDescriptor;
import net.sf.mustang.log.KLog;
import net.sf.mustang.service.Request;
import net.sf.mustang.wm.WMEvaluator;
import net.sf.mustang.wm.WMManager;

public final class Query {

    private static KLog log = Mustang.getLog(Query.class);

    public static final String SQL_TYPE = "SQL_TYPE";

    public static final String SQL_SIZE = "SQL_SIZE";

    public static QueryContext getQueryContext(String channel) throws Exception {
        return new QueryContext(WMManager.getInstance().getBroker(), channel);
    }

    public static void silentReleaseQueryContext(QueryContext queryContext) {
        try {
            queryContext.release();
        } catch (Exception e) {
        }
    }

    public static void releaseQueryContext(QueryContext queryContext) throws Exception {
        queryContext.release();
    }

    public static void commitQueryContext(QueryContext queryContext) throws Exception {
        queryContext.getConnection().commit();
        if (log.isNotice()) log.notice("committed con (" + queryContext.getConnection() + "): " + queryContext.getChannel());
    }

    public static void undoQueryContext(QueryContext queryContext) throws Exception {
        queryContext.getConnection().rollback();
        if (log.isNotice()) log.notice("rollbacked con (" + queryContext.getConnection() + "): " + queryContext.getChannel());
    }

    public static boolean executeFile(QueryContext queryContext, String template) throws Exception {
        return execute(null, queryContext, template, true);
    }

    public static boolean execute(QueryContext queryContext, String template) throws Exception {
        return execute(null, queryContext, template, false);
    }

    public static boolean execute(Request request, QueryContext queryContext, String template) throws Exception {
        return execute(request, queryContext, template, false);
    }

    public static boolean execute(Request request, QueryContext queryContext, String template, boolean isFile) throws Exception {
        boolean retVal = true;
        Statement statement = null;
        queryContext.getParameters().reset();
        String sql = null;
        if (isFile) sql = WMEvaluator.crunchFile(queryContext, template).trim(); else sql = WMEvaluator.crunch(request, queryContext, template).trim();
        if (queryContext.get(K.BREAK) == null) {
            if (sql.length() > 0) {
                statement = getStatement(queryContext, sql);
                if (log.isNotice()) log.notice("executing: " + sql);
                switch(queryContext.getParameters().getMode()) {
                    case Parameters.DIRECT_MODE:
                        retVal = statement.execute(sql);
                        break;
                    case Parameters.BATCH_MODE:
                        statement.addBatch(sql);
                        if (log.isNotice()) log.notice("added to batch channel '" + queryContext.getParameters().getRegBatchChannel() + "'");
                        break;
                    case Parameters.PREPARED_MODE:
                        retVal = ((PreparedStatement) statement).execute();
                        break;
                    case Parameters.PREP_BATCH_MODE:
                        ((PreparedStatement) statement).addBatch();
                        if (log.isNotice()) log.notice("added to batch channel '" + queryContext.getParameters().getRegBatchChannel() + "'");
                        break;
                    case Parameters.CALL_MODE:
                        retVal = ((CallableStatement) statement).execute();
                        break;
                }
                if (log.isNotice()) log.notice("executed!");
            }
            flushBatch(queryContext);
        }
        return retVal;
    }

    public static void flushBatch(QueryContext queryContext) throws SQLException {
        Statement statement;
        String batchChannel;
        Vector<Statement> batchList;
        batchChannel = queryContext.getParameters().getFlushBatchChannel();
        if (batchChannel != null) {
            if (log.isNotice()) log.notice("flushing to batch channel '" + batchChannel + "'");
            batchList = queryContext.getBatches().get(batchChannel);
            if (batchList != null) {
                int n = batchList.size();
                for (int i = 0; i < n; i++) {
                    statement = batchList.remove(0);
                    statement.executeBatch();
                    statement.clearBatch();
                }
            }
            if (log.isNotice()) log.notice("flushed!");
        }
    }

    public static Vector<Bean> executeQueryFile(QueryContext queryContext, String template) throws Exception {
        return executeQuery(null, queryContext, template, true);
    }

    public static Vector<Bean> executeQuery(QueryContext queryContext, String template) throws Exception {
        return executeQuery(null, queryContext, template, false);
    }

    public static Vector<Bean> executeQuery(Request request, QueryContext queryContext, String template) throws Exception {
        return executeQuery(request, queryContext, template, false);
    }

    public static Vector<Bean> executeQuery(Request request, QueryContext queryContext, String template, boolean isFile) throws Exception {
        Vector retVal = new Vector();
        Statement statement = null;
        ResultSet resultSet = null;
        Object result = null;
        Vector values = null;
        try {
            queryContext.getParameters().reset();
            String sql = null;
            if (isFile) sql = WMEvaluator.crunchFile(queryContext, template).trim(); else sql = WMEvaluator.crunch(request, queryContext, template).trim();
            if (queryContext.get(K.BREAK) == null) {
                flushBatch(queryContext);
                if (sql.length() > 0) {
                    statement = getStatement(queryContext, sql);
                    if (log.isNotice()) log.notice("executing: " + sql);
                    switch(queryContext.getParameters().getMode()) {
                        case Parameters.DIRECT_MODE:
                        case Parameters.BATCH_MODE:
                            resultSet = statement.executeQuery(sql);
                            break;
                        case Parameters.PREPARED_MODE:
                        case Parameters.PREP_BATCH_MODE:
                            resultSet = ((PreparedStatement) statement).executeQuery();
                            break;
                        case Parameters.CALL_MODE:
                            ((CallableStatement) statement).execute();
                            result = ((CallableStatement) statement).getObject(queryContext.getParameters().getOutPosition() + 1);
                            if (result instanceof ResultSet) resultSet = (ResultSet) result;
                            break;
                    }
                    PropertyDescriptor[] propDescriptors = null;
                    BeanDescriptor beanDescriptor = null;
                    String temp = null;
                    if (resultSet != null) {
                        resultSet.setFetchSize(256);
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int colNum = metaData.getColumnCount();
                        propDescriptors = new PropertyDescriptor[colNum];
                        for (int i = 0; i < colNum; i++) {
                            propDescriptors[i] = new PropertyDescriptor();
                            propDescriptors[i].setKey(metaData.getColumnName(i + 1).toLowerCase());
                            propDescriptors[i].setAttribute(SQL_TYPE, metaData.getColumnTypeName(i + 1) + K.COMMA + metaData.getColumnType(i + 1));
                            propDescriptors[i].setAttribute(SQL_SIZE, K.EMPTY + metaData.getColumnDisplaySize(i + 1));
                            if (log.isInfo()) log.info("created property: " + propDescriptors[i].getKey());
                        }
                        beanDescriptor = new BeanDescriptor(propDescriptors);
                        while (resultSet.next()) {
                            values = new Vector();
                            for (int i = 1; i <= colNum; i++) {
                                temp = resultSet.getString(i);
                                if (temp != null) temp = temp.trim();
                                values.add(temp);
                                if (log.isInfo()) log.info("added: " + temp + " as property: " + propDescriptors[i - 1].getKey());
                            }
                            retVal.add(new Bean(beanDescriptor, values));
                        }
                    } else if (result != null) {
                        propDescriptors = new PropertyDescriptor[1];
                        propDescriptors[0] = new PropertyDescriptor();
                        propDescriptors[0].setKey("result");
                        if (log.isInfo()) log.info("creating property: result");
                        beanDescriptor = new BeanDescriptor(propDescriptors);
                        values = new Vector();
                        temp = result.toString();
                        if (log.isInfo()) log.info("adding: " + temp + " as property: result");
                        values.addElement(result.toString().trim());
                        retVal.addElement(new Bean(beanDescriptor, values));
                    }
                    if (log.isNotice()) log.notice("executed!");
                }
            }
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }
        return retVal;
    }

    private static Statement getStatement(QueryContext queryContext, String sql) throws Exception {
        Statement retVal = null;
        String batchChannel;
        Vector<Statement> batchList;
        int mode = queryContext.getParameters().getMode();
        if (mode == Parameters.BATCH_MODE) {
            batchChannel = queryContext.getParameters().getRegBatchChannel();
            batchList = queryContext.getBatches().get(batchChannel);
            if (batchList == null) {
                batchList = new Vector();
                queryContext.getBatches().put(batchChannel, batchList);
            }
            for (int i = 0; i < batchList.size(); i++) if (!(batchList.get(i) instanceof PreparedStatement)) retVal = batchList.get(i);
            if (retVal == null) {
                retVal = queryContext.getConnection().createStatement();
                batchList.add(retVal);
            }
        } else {
            retVal = (Statement) queryContext.getStatements().get(sql);
            if (retVal == null) {
                switch(mode) {
                    case Parameters.DIRECT_MODE:
                        retVal = queryContext.getConnection().createStatement();
                        break;
                    case Parameters.PREP_BATCH_MODE:
                    case Parameters.PREPARED_MODE:
                        retVal = queryContext.getConnection().prepareStatement(sql);
                        break;
                    case Parameters.CALL_MODE:
                        retVal = queryContext.getConnection().prepareCall(sql);
                        break;
                }
                queryContext.getStatements().put(sql, retVal);
            }
            if (mode == Parameters.PREP_BATCH_MODE) {
                batchChannel = queryContext.getParameters().getRegBatchChannel();
                batchList = queryContext.getBatches().get(batchChannel);
                if (batchList == null) {
                    batchList = new Vector();
                    queryContext.getBatches().put(batchChannel, batchList);
                }
                if (!batchList.contains(retVal)) batchList.add(retVal);
            }
        }
        if (mode != Parameters.DIRECT_MODE && mode != Parameters.BATCH_MODE) {
            if (log.isNotice()) log.notice("applying params on statements: " + retVal);
            Vector<Parameter> l = queryContext.getParameters().getParams();
            Parameter parameter;
            for (int i = 0; i < l.size(); i++) {
                parameter = l.get(i);
                if (i == queryContext.getParameters().getOutPosition()) {
                    if (log.isNotice()) log.notice("registering out parameter: " + (i + 1) + " type: " + parameter.getType());
                    ((CallableStatement) retVal).registerOutParameter(i + 1, parameter.getType());
                } else {
                    if (parameter.getValue() != null) {
                        if (parameter.isStandard()) {
                            if (log.isNotice()) log.notice("applying param: " + (i + 1) + " value: " + parameter.getValue() + " type: " + parameter.getType());
                            ((PreparedStatement) retVal).setObject(i + 1, parameter.getValue(), parameter.getType());
                        } else {
                            if (parameter.getValue() instanceof InputStream) {
                                InputStream in = (InputStream) parameter.getValue();
                                if (log.isNotice()) log.notice("applying param: " + (i + 1) + " value: " + parameter.getValue() + " type: InputStream");
                                ((PreparedStatement) retVal).setAsciiStream(i + 1, in, in.available());
                            }
                        }
                    } else {
                        if (log.isNotice()) log.notice("applying param: " + (i + 1) + " value: null type: " + parameter.getType());
                        ((PreparedStatement) retVal).setNull(i + 1, parameter.getType());
                    }
                }
            }
        }
        return retVal;
    }
}
