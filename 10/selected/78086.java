package org.tom.weather;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public abstract class SqlHelper {

    protected Properties props;

    protected Connection con = null;

    protected Statement statement;

    protected TimeZone tz = null;

    public void setProps(Properties props) {
        this.props = props;
    }

    public TimeZone getTimeZone() {
        if (tz == null) {
            tz = TimeZone.getTimeZone(props.getProperty(Constants.TIMEZONE));
        }
        return tz;
    }

    public static SqlHelper getInstance(Properties props) {
        SqlHelper sqlHelper = null;
        try {
            sqlHelper = (SqlHelper) Class.forName(props.getProperty(Constants.SQL_CLASS)).newInstance();
            sqlHelper.setProps(props);
        } catch (Exception e) {
            System.out.println("Error: unable to instantiate: " + props.getProperty(Constants.SQL_CLASS));
            e.printStackTrace();
            System.exit(1);
        }
        return sqlHelper;
    }

    public static SqlHelper getGraphInstance(Properties props) {
        SqlHelper sqlHelper = null;
        try {
            sqlHelper = (SqlHelper) Class.forName(props.getProperty(Constants.GRAPH_SQL_CLASS)).newInstance();
            sqlHelper.setProps(props);
        } catch (Exception e) {
            System.out.println("Error: unable to instantiate: " + props.getProperty(Constants.GRAPH_SQL_CLASS));
            e.printStackTrace();
            System.exit(1);
        }
        return sqlHelper;
    }

    public void closeConnection() {
        try {
            if (con != null) {
                con.close();
                con = null;
            }
        } catch (Exception e) {
            con = null;
        }
    }

    public void insertArchiveEntries(ArchiveEntry entries[]) throws WeatherMonitorException {
        String sql = null;
        try {
            Connection con = getConnection();
            Statement stmt = con.createStatement();
            ResultSet rslt = null;
            con.setAutoCommit(false);
            for (int i = 0; i < entries.length; i++) {
                if (!sanityCheck(entries[i])) {
                } else {
                    sql = getSelectSql(entries[i]);
                    rslt = stmt.executeQuery(sql);
                    if (rslt.next()) {
                        if (rslt.getInt(1) == 0) {
                            sql = getInsertSql(entries[i]);
                            if (stmt.executeUpdate(sql) != 1) {
                                con.rollback();
                                System.out.println("rolling back sql");
                                throw new WeatherMonitorException("exception on insert");
                            }
                        }
                    }
                }
            }
            con.commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new WeatherMonitorException(e.getMessage());
        }
    }

    protected boolean sanityCheck(ArchiveEntry e) {
        boolean ok = true;
        if (e.getOutHumidity() < 0 || e.getOutHumidity() > 110) {
            ok = false;
        }
        if (e.getBarometer() < 27.0 || e.getBarometer() > 33.0) {
            ok = false;
        }
        return ok;
    }

    protected Statement getStatement() throws SQLException {
        if (statement == null) {
            statement = getConnection().createStatement();
        }
        return statement;
    }

    public abstract Connection getConnection();

    protected String getDateAsSql(java.util.Date d) {
        SimpleDateFormat formatter = new SimpleDateFormat(props.getProperty("weather.db.dateFormat"));
        formatter.setTimeZone(TimeZone.getTimeZone(props.getProperty("weather.db.timeZone")));
        String dateString = formatter.format(d);
        return dateString;
    }

    protected String getInsertSql(ArchiveEntry entry) {
        Date snapDate = entry.getDate(getTimeZone());
        String dateString = getDateAsSql(snapDate);
        String sql = "insert into " + props.getProperty(Constants.TABLE_NAME) + " ( ";
        sql = sql + " snapdate, ";
        sql = sql + " location, ";
        sql = sql + " avgInTemp, ";
        sql = sql + " avgOutTemp, ";
        sql = sql + " hiOutTemp, ";
        sql = sql + " lowOutTemp, ";
        sql = sql + " rain, ";
        sql = sql + " avgWindSpeed, ";
        sql = sql + " windDirection, ";
        sql = sql + " windGust, ";
        sql = sql + " barometer, ";
        sql = sql + " inHumidity, ";
        sql = sql + " dewpoint, ";
        sql = sql + " windchill, ";
        sql = sql + " outHumidity ) values ( '";
        sql = sql + dateString + "', ";
        sql = sql + "'" + props.getProperty(Constants.LOCATION) + "'" + ", ";
        sql = sql + (float) (entry.getAvgInTemp()) + ", ";
        sql = sql + (float) (entry.getAvgOutTemp()) + ", ";
        sql = sql + (float) (entry.getHiOutTemp()) + ", ";
        sql = sql + (float) (entry.getLowOutTemp()) + ", ";
        sql = sql + (float) (entry.getRain()) + ", ";
        sql = sql + entry.getAvgWindSpeed() + ", ";
        sql = sql + entry.getNativeWindDirection() + ", ";
        sql = sql + entry.getWindGust() + ", ";
        sql = sql + (float) (entry.getBarometer()) + ", ";
        sql = sql + entry.getInHumidity() + ", ";
        sql = sql + Converter.getDewpoint((float) entry.getOutHumidity(), (float) entry.getAvgOutTemp()) + ", ";
        sql = sql + Converter.getWindChill(entry.getWindGust(), (float) entry.getAvgOutTemp()) + ", ";
        sql = sql + entry.getOutHumidity() + ") ";
        return sql;
    }

    protected String getSelectSql(ArchiveEntry entry) {
        Date snapDate = entry.getDate(getTimeZone());
        String dateString = getDateAsSql(snapDate);
        String sql = "select count(*) from " + props.getProperty(Constants.TABLE_NAME) + " where snapDate = '" + dateString + "'";
        return sql;
    }

    protected java.sql.ResultSet executeQuery(String sql) throws java.sql.SQLException {
        return getStatement().executeQuery(sql);
    }

    protected int executeUpdate(String sql) throws java.sql.SQLException {
        return getStatement().executeUpdate(sql);
    }
}
