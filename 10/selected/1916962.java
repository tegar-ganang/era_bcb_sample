package com.diyfiesta.pokimon.dataaccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.sql.DataSource;
import com.diyfiesta.pokimon.dataaccess.datasource.DataSourceFactory;
import com.diyfiesta.pokimon.dataaccess.datasource.DataSourceFactoryException;
import com.diyfiesta.pokimon.to.BasicStartingHandTransferObject;
import com.diyfiesta.pokimon.to.CalculateTransferObject;

/** DAO implementing generic JDBC access to the calculate tables.  
 */
public class GenericJDBCCalculateDAO implements CalculateHandDAO {

    private static final String INSERT_CALC_SQL = "INSERT INTO calculate (hands, board, dead) VALUES(?, ?, ?);";

    private static final String INSERT_HAND_SQL = "INSERT INTO calculatehands (id, hand, win, draw, lose) VALUES (LAST_INSERT_ID(), ?, ?, ?, ?);";

    private static final String SELECT_CALC_SQL = "SELECT COUNT(hands) AS count FROM calculate WHERE hands = ";

    private static final String SELECT_HANDS_SQL = "SELECT h.hand, h.win, h.lose, h.draw FROM calculate AS c JOIN calculatehands AS h ON c.id = h.id WHERE c.hands = ";

    private static final String SELECT_ID_SQL = "SELECT id FROM calculate WHERE hands = ";

    private static final String UPDATE_HANDS_SQL = "UPDATE calculatehands SET win=?, lose=?, draw=? WHERE id=? AND hand=?;";

    private static DataSource c_ds = null;

    /** Constructor.
	 */
    public GenericJDBCCalculateDAO() {
    }

    /** Get the data source. Uses the {@link DataSourceFactory} to get a specific
	 * data source. The data source itself is stored as a static within this class to
	 * avoid repeatidly looking it up.
	 * @throws CalculateDAOException if the {@link DataSource} couldn't be looked up 
	 * or created.
	 * @return the {@link DataSource}
	 */
    protected static DataSource getDataSource() throws CalculateDAOException {
        if (c_ds == null) {
            try {
                c_ds = DataSourceFactory.getDataSource();
            } catch (DataSourceFactoryException dsfe) {
                throw new CalculateDAOException(dsfe);
            }
        }
        return c_ds;
    }

    /**
	 * @see com.diyfiesta.pokimon.dataaccess.CalculateHandDAO#insertCalculatedHand(com.diyfiesta.pokimon.to.CalculateTransferObject)
	 */
    public boolean insertCalculatedHand(CalculateTransferObject to, BasicStartingHandTransferObject[] hands) throws CalculateDAOException {
        boolean result = false;
        Connection connection = null;
        PreparedStatement prep = null;
        try {
            connection = getDataSource().getConnection();
            connection.setAutoCommit(false);
            prep = connection.prepareStatement(INSERT_CALC_SQL);
            prep.setString(1, to.getHandsAsString());
            prep.setString(2, to.getBoardAsString());
            prep.setString(3, to.getDeadAsString());
            result = prep.execute();
        } catch (SQLException sqle) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.setNextException(sqle);
                throw new CalculateDAOException(e);
            }
            throw new CalculateDAOException(sqle);
        } finally {
            if (prep != null) {
                try {
                    prep.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
        }
        try {
            prep = connection.prepareStatement(INSERT_HAND_SQL);
            for (int i = 0; i < hands.length; i++) {
                prep.setString(1, hands[i].getHand());
                prep.setInt(2, hands[i].getWins());
                prep.setInt(3, hands[i].getDraws());
                prep.setInt(4, hands[i].getLoses());
                result = prep.execute();
            }
            connection.commit();
        } catch (SQLException sqle) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.setNextException(sqle);
                throw new CalculateDAOException(e);
            }
            throw new CalculateDAOException(sqle);
        } finally {
            if (prep != null) {
                try {
                    prep.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
        }
        return result;
    }

    /**
	 * @see com.diyfiesta.pokimon.dataaccess.CalculateHandDAO#deleteCalculatedHand(java.lang.String)
	 */
    public boolean deleteCalculatedHand(String query) throws CalculateDAOException {
        return false;
    }

    /** Update the record based on the object passed in.
	 * @see com.diyfiesta.pokimon.dataaccess.CalculateHandDAO#updateCalculatedHand(com.diyfiesta.pokimon.to.CalculateTransferObject)
	 */
    public boolean updateCalculatedHand(CalculateTransferObject query, BasicStartingHandTransferObject[] hands) throws CalculateDAOException {
        boolean retval = false;
        Connection connection = null;
        Statement statement = null;
        PreparedStatement prep = null;
        ResultSet result = null;
        StringBuffer sql = new StringBuffer(SELECT_ID_SQL);
        sql.append(appendQuery(query));
        try {
            connection = getDataSource().getConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            result = statement.executeQuery(sql.toString());
            if (result.first()) {
                String id = result.getString("id");
                prep = connection.prepareStatement(UPDATE_HANDS_SQL);
                for (int i = 0; i < hands.length; i++) {
                    prep.setInt(1, hands[i].getWins());
                    prep.setInt(2, hands[i].getLoses());
                    prep.setInt(3, hands[i].getDraws());
                    prep.setString(4, id);
                    prep.setString(5, hands[i].getHand());
                    if (prep.executeUpdate() != 1) {
                        throw new SQLException("updated too many records in calculatehands, " + id + "-" + hands[i].getHand());
                    }
                }
                connection.commit();
            }
        } catch (SQLException sqle) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.setNextException(sqle);
                throw new CalculateDAOException(e);
            }
            throw new CalculateDAOException(sqle);
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
            if (prep != null) {
                try {
                    prep.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
        }
        return retval;
    }

    /**
	 * @see com.diyfiesta.pokimon.dataaccess.CalculateHandDAO#hasCalculatedHand(java.lang.String)
	 */
    public boolean hasCalculatedHand(CalculateTransferObject query) throws CalculateDAOException {
        boolean retval = false;
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        StringBuffer sql = new StringBuffer(SELECT_CALC_SQL);
        sql.append(appendQuery(query));
        try {
            connection = getDataSource().getConnection();
            statement = connection.createStatement();
            result = statement.executeQuery(sql.toString());
            if (result.first()) {
                int count = result.getInt("count");
                if (count == 1) {
                    retval = true;
                }
            }
        } catch (SQLException sqle) {
            throw new CalculateDAOException(sqle);
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
        }
        return retval;
    }

    /**
	 * @see com.diyfiesta.pokimon.dataaccess.CalculateHandDAO#getCalculatedHand(java.lang.String)
	 */
    public BasicStartingHandTransferObject[] getCalculatedHand(CalculateTransferObject query) throws CalculateDAOException {
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        BasicStartingHandTransferObject[] retval = null;
        ArrayList<BasicStartingHandTransferObject> list = new ArrayList<BasicStartingHandTransferObject>();
        StringBuffer sql = new StringBuffer(SELECT_HANDS_SQL);
        sql.append(appendQuery(query));
        try {
            connection = getDataSource().getConnection();
            statement = connection.createStatement();
            result = statement.executeQuery(sql.toString());
            while (result.next()) {
                String hand = result.getString("hand");
                int win = result.getInt("win");
                int lose = result.getInt("lose");
                int draw = result.getInt("draw");
                BasicStartingHandTransferObject to = new BasicStartingHandTransferObject(hand);
                to.setWins(win);
                to.setLoses(lose);
                to.setDraws(draw);
                list.add(to);
            }
            retval = new BasicStartingHandTransferObject[list.size()];
            for (int i = 0; i < retval.length; i++) {
                retval[i] = (BasicStartingHandTransferObject) list.get(i);
            }
        } catch (SQLException sqle) {
            throw new CalculateDAOException(sqle);
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    throw new CalculateDAOException(e);
                }
            }
        }
        return retval;
    }

    /**
	 * @see com.diyfiesta.pokimon.dataaccess.CalculateHandDAO#getCalculatedHand(int)
	 */
    public CalculateTransferObject getCalculatedHand(int id) throws CalculateDAOException {
        return null;
    }

    /** Append the rest of the select query dependant on the contents of the board and dead
	 * cards. Always includes the hands. 
	 * @param query the transfer object containing the board and dead card lists
	 * @return the end part of the basic select SQL 
	 */
    private String appendQuery(CalculateTransferObject query) {
        StringBuffer sql = new StringBuffer();
        sql.append("'");
        sql.append(query.getHandsAsString());
        sql.append("'");
        sql.append(" AND board = '");
        sql.append(query.getBoardAsString());
        sql.append("'");
        sql.append(" AND dead = '");
        sql.append(query.getDeadAsString());
        sql.append("'");
        sql.append(";");
        return sql.toString();
    }
}
