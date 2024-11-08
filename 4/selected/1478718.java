package net.sourceforge.jcoupling2.persistence;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import net.sourceforge.jcoupling2.exception.JCouplingException;
import net.sourceforge.jcoupling2.peer.property.ChooseClause;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import org.apache.log4j.Logger;

public class FilterModel {

    private Logger logger = Logger.getLogger(FilterModel.class);

    private List<Channel> _referencedChannels = null;

    private String _name;

    private String _whereClause;

    private ChooseClause _chooseClause;

    public static String ADD = null;

    /**
	 * @param channel
	 *          the channel that the filter model is associated with
	 * @throws FilterModelException
	 *           if ...
	 * @throws ChannelException
	 *           if the specified channel does not exist, Filter model must have a list of channels. 
	 */
    public FilterModel(String name, List<Channel> channels, String whereClause) throws JCouplingException {
        if (channels == null || channels.size() == 0) {
            throw new IllegalArgumentException("Needs channels.");
        }
        if (whereClause != null) {
            if (whereClause.indexOf('?') == -1) {
                throw new IllegalArgumentException("Non - null where clause needs value insert points ('?').");
            }
        }
        this._name = name;
        this._referencedChannels = channels;
        this._whereClause = whereClause;
        setChooseClause(new ChooseClause());
    }

    public FilterModel(String name) throws JCouplingException {
        OracleConnection con = null;
        OracleCallableStatement callableStatement = null;
        String callString = null;
        try {
            con = (OracleConnection) ConnectionManager.getConnection();
            callString = new String("{? = call pkg_filtermodel.GET_WHERECLAUSE( ? )}");
            callableStatement = (OracleCallableStatement) con.prepareCall(callString);
            callableStatement.registerOutParameter(1, OracleTypes.VARCHAR);
            callableStatement.setString(2, name);
            logger.debug(DataMapper.logCallableStatement(callString, new Object[] { name }));
            callableStatement.executeQuery();
            this._name = name;
            this._whereClause = callableStatement.getString(1);
        } catch (SQLException e) {
            throw new JCouplingException("Error while setting the WhereClause for Filtermodel (" + name + "):", e);
        } finally {
            try {
                if (callableStatement != null) callableStatement.close();
            } catch (SQLException e) {
                logger.warn("ERROR while closing callableStatement", e);
            }
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                logger.warn("ERROR while closing Connection", e);
            }
        }
    }

    public String getWhereClause() {
        return _whereClause;
    }

    public String getName() {
        return _name;
    }

    public Collection<Channel> getChannels() {
        return _referencedChannels;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.getClass().getSimpleName());
        buffer.append(" channel=");
        buffer.append(_referencedChannels.toString());
        buffer.append(" [");
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("]");
        return buffer.toString();
    }

    public void setChooseClause(ChooseClause _chooseClause) {
        this._chooseClause = _chooseClause;
    }

    public ChooseClause getChooseClause() {
        return _chooseClause;
    }
}
