package com.kni.etl.dbutils;

import java.math.BigDecimal;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.kni.etl.EngineConstants;
import com.kni.etl.Metadata;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.util.XMLHelper;

/**
 * The Class SQLQuery.
 */
public class SQLQuery {

    private static final String THIS_GET_INCREMENTAL = "this.getIncremental(";

    private static final String THIS_GET_PARTITION_ID = "this.getPartitionID()";

    private static final String THIS_GET_PARTITIONS = "this.getPartitions()";

    private static final String THIS_GET_RANGE_PARTITION = "this.getRangePartition(";

    public static boolean containPartitionCode(String sql) {
        return (sql.contains(THIS_GET_PARTITIONS) && sql.contains(THIS_GET_PARTITION_ID)) || sql.contains(THIS_GET_RANGE_PARTITION);
    }

    public static void main(String[] args) throws ParseException {
        int p = 5;
        for (int i = 0; i < p; i++) System.out.println(replaceRangePartition("select  where this.getRangePartition(6,,45656565) dfgdf", p, i));
    }

    public static String replaceRangePartition(String sql, int partitions, int partitionID) throws ParseException {
        Pattern p = Pattern.compile("(" + THIS_GET_RANGE_PARTITION.replace(".", "\\.").replace("(", "\\(") + ").*\\)");
        Matcher m = p.matcher(sql);
        try {
            String code = sql;
            while (m.find()) {
                code = code.substring(m.start(), m.end());
                code = code.replace(THIS_GET_RANGE_PARTITION, "").trim();
                String[] params = code.substring(0, code.length() - 1).split(",");
                int startId = Integer.parseInt(params[0].trim());
                int endId = Integer.parseInt(params[1].trim());
                int pSize = (endId - startId) / partitions;
                int pStart, pEnd;
                if (partitionID == 0) {
                    pStart = startId;
                    pEnd = pSize + startId;
                } else if (partitionID == partitions - 1) {
                    pStart = (pSize * partitionID) + 1 + startId;
                    pEnd = endId;
                } else {
                    pStart = (pSize * partitionID) + startId + 1;
                    pEnd = (pSize * (partitionID + 1)) + startId;
                }
                code = pStart + " and " + pEnd;
                return m.replaceAll(code);
            }
            return code;
        } catch (Throwable e) {
            ParseException e1 = new ParseException("Invalid parameters for auto range partitioning syntax should be " + THIS_GET_RANGE_PARTITION + "start range, end range) - " + e.getMessage() + ", whilst parsing \"" + sql + "\"", m.start());
            e1.setStackTrace(e.getStackTrace());
            throw e1;
        }
    }

    /** The execute. */
    boolean execute;

    /** The parameter list. */
    int parameterList = -1;

    /** The sql. */
    String sql;

    private List<ParameterColumnMapping> incrementalParameters;

    private String nonIncSQL;

    private String previousValue;

    /**
	 * Instantiates a new SQL query.
	 * 
	 * @param sql
	 *            the sql
	 * @param parameterList
	 *            the parameter list
	 * @param pExecute
	 *            the execute
	 * @throws ParseException
	 */
    public SQLQuery(String sql, int parameterList, boolean pExecute) throws ParseException {
        super();
        this.sql = sql;
        this.execute = pExecute;
        this.parameterList = parameterList;
        this.sql = sql.replace(THIS_GET_PARTITIONS, Integer.toString(1)).replace(THIS_GET_PARTITION_ID, Integer.toString(0));
        this.sql = replaceRangePartition(sql, 1, 0);
    }

    public SQLQuery(String sql, int parameterList, boolean pExecute, int partitions, int partitionID) throws ParseException {
        this.execute = pExecute;
        this.parameterList = parameterList;
        this.sql = sql.replace(THIS_GET_PARTITIONS, Integer.toString(partitions)).replace(THIS_GET_PARTITION_ID, Integer.toString(partitionID));
        this.sql = replaceRangePartition(this.sql, partitions, partitionID);
    }

    /**
	 * Execute query.
	 * 
	 * @return true, if successful
	 */
    public boolean executeQuery() {
        return this.execute;
    }

    /**
	 * Gets the parameter list ID.
	 * 
	 * @return the parameter list ID
	 */
    public int getParameterListID() {
        return this.parameterList;
    }

    /**
	 * Gets the SQL.
	 * 
	 * @param partition
	 * @param partitions
	 * 
	 * @return the SQL
	 */
    public String getSQL() {
        return sql;
    }

    private enum IncrementalType {

        DATE, NUMBER, STRING, UNKNOWN
    }

    public Collection<ParameterColumnMapping> replaceIncremental(String parameterListName) throws ParseException {
        Pattern p = Pattern.compile("(" + THIS_GET_INCREMENTAL.replace(".", "\\.").replace("(", "\\(") + ").*\\)");
        Matcher m = p.matcher(sql);
        incrementalParameters = new ArrayList<ParameterColumnMapping>();
        try {
            String code = sql;
            while (m.find()) {
                String function = code.substring(m.start(), m.end());
                code = code.replace(function, "${INCPARAM}");
                function = function.replace(THIS_GET_INCREMENTAL, "");
                String param = function.substring(0, function.length() - 1).trim();
                String[] vals = param.split(",");
                IncrementalType type = null;
                if (vals.length == 4) this.offSet = Integer.parseInt(vals[3].trim());
                if (vals.length == 3) {
                    type = IncrementalType.valueOf(vals[2].trim());
                } else type = IncrementalType.UNKNOWN;
                incrementalParameters.add(new ParameterColumnMapping(parameterListName, vals[1].trim(), vals[0].trim(), type));
            }
            sql = code.replace("${INCPARAM}", "?");
            return incrementalParameters;
        } catch (Throwable e) {
            ParseException e1 = new ParseException("Invalid parameters for incremental syntax should be " + THIS_GET_INCREMENTAL + "column name,parameter name) - " + e.getMessage() + ", whilst parsing \"" + sql + "\"", m.start());
            e1.setStackTrace(e.getStackTrace());
            throw e1;
        }
    }

    public class ParameterColumnMapping {

        private IncrementalType columnType;

        public ParameterColumnMapping(String parameterListName, String parameter, String column, IncrementalType type) {
            super();
            this.parameterListName = parameterListName;
            this.parameter = parameter;
            this.column = column;
            this.columnType = type;
        }

        String parameter;

        String column;

        Class type;

        String value;

        private String parameterListName;

        private Comparable maxValue;

        private String newValue;

        public String getParameterName() {
            return this.parameter;
        }

        public void setValue(String parameterValue) {
            this.value = parameterValue;
        }

        public String getColumnName() {
            return this.column;
        }

        public void setClass(Class portClass) throws KETLThreadException {
            this.type = portClass;
            if (Comparable.class.isAssignableFrom(portClass) == false) {
                throw new KETLThreadException("The incremental column is non comparable and a new max cannot be calculated - " + portClass.getName(), Thread.currentThread());
            }
        }

        public void writeBackParameter() throws KETLThreadException {
            Metadata md = ResourcePool.getMetadata();
            if (md == null) {
                throw new KETLThreadException("Parameter writeback failed as metadata could not be connected to", this);
            }
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.newDocument();
                Element paramList = document.createElement(EngineConstants.PARAMETER_LIST);
                Element parameter = document.createElement(EngineConstants.PARAMETER);
                document.appendChild(paramList);
                paramList.appendChild(parameter);
                parameter.setAttribute(ETLStep.NAME_ATTRIB, this.parameter);
                if (maxValue == null) newValue = null; else if (Number.class.isAssignableFrom(type)) {
                    newValue = Long.toString(((Number) maxValue).longValue());
                } else if (CharSequence.class.isAssignableFrom(type)) {
                    newValue = maxValue.toString();
                } else if (java.util.Date.class.isAssignableFrom(type)) {
                    newValue = Long.toString(((Timestamp) maxValue).getTime());
                } else {
                    throw new SQLException("incremental does not support " + type);
                }
                parameter.setTextContent(newValue);
                paramList.setAttribute(ETLStep.NAME_ATTRIB, this.parameterListName);
                md.importParameterList(paramList);
                ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Updating incremental parameter " + this.getParameterName() + ", from " + this.getValue() + " to " + maxValue.toString());
            } catch (Exception e) {
                throw new KETLThreadException(e, this);
            }
        }

        public void setMaxValue(Comparable maxValue) {
            this.maxValue = maxValue;
        }

        public Object getNewValue() {
            return this.newValue;
        }

        public String getValue() {
            return this.value;
        }
    }

    /**
	 * Sets the SQL.
	 * 
	 * @param arg0
	 *            the new SQL
	 */
    public void setSQL(String arg0) {
        this.sql = arg0;
    }

    public boolean hasIncrementalParameters() {
        return this.incrementalParameters != null && this.incrementalParameters.size() > 0;
    }

    public String getFinalSQL(String sample) {
        return this.getSQL() + " " + sample;
    }

    private int offSet = 0;

    public void setIncrementalParameters(PreparedStatement pstmt) throws SQLException {
        for (int i = 0; i < this.incrementalParameters.size(); i++) {
            ParameterColumnMapping param = this.incrementalParameters.get(i);
            String paramValue = param.value;
            Class cls = param.type;
            if (Number.class.isAssignableFrom(cls)) {
                BigDecimal bd = paramValue == null ? new BigDecimal(Integer.MIN_VALUE) : new BigDecimal(paramValue).add(new BigDecimal(offSet));
                pstmt.setBigDecimal(i + 1, bd);
                previousValue = bd.toString();
            } else if (CharSequence.class.isAssignableFrom(cls)) {
                paramValue = paramValue == null ? new String(new byte[] { 0 }) : paramValue;
                pstmt.setString(i + 1, paramValue);
                previousValue = paramValue;
            } else if (java.util.Date.class.isAssignableFrom(cls)) {
                paramValue = paramValue == null ? "0" : paramValue;
                Timestamp dt = new java.sql.Timestamp(Long.parseLong(paramValue) + offSet);
                pstmt.setTimestamp(i + 1, dt);
                previousValue = dt.toString();
            } else {
                throw new SQLException("incremental does not support " + cls);
            }
            ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Using incremental value " + previousValue + " for column " + param.column);
        }
    }

    public List<ParameterColumnMapping> getIncrementalMappings() {
        return this.incrementalParameters;
    }

    public void setIncrementalBlankParameters(PreparedStatement stmt) throws SQLException {
        for (int i = 0; i < this.incrementalParameters.size(); i++) {
            ParameterColumnMapping param = this.incrementalParameters.get(i);
            switch(param.columnType) {
                case DATE:
                    stmt.setNull(i + 1, java.sql.Types.DATE);
                    break;
                case NUMBER:
                    stmt.setNull(i + 1, java.sql.Types.NUMERIC);
                    break;
                case STRING:
                default:
                    stmt.setNull(i + 1, java.sql.Types.VARCHAR);
                    break;
            }
        }
    }
}
