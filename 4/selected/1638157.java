package net.sourceforge.jcoupling2.dao;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import net.sourceforge.jcoupling.peer.property.ChooseClause;
import net.sourceforge.jcoupling2.dao.obsolete.Attribute;
import net.sourceforge.jcoupling2.dao.obsolete.ChannelException;
import net.sourceforge.jcoupling2.dao.obsolete.DataAccessObject;
import net.sourceforge.jcoupling2.dao.obsolete.FilterModelException;
import net.sourceforge.jcoupling2.dao.obsolete.Operation;

public class FilterModel extends DataAccessObject {

    public static TreeSet<Operation> supportedOperations = null;

    public static TreeSet<Attribute> supportedAttributes = null;

    private List<Channel> referencedChannels = null;

    private String _name;

    private String whereClause;

    private ChooseClause _chooseClause;

    public static String ADD = null;

    /**
	 * @param channel
	 *          the channel that the filter model is associated with
	 * @throws FilterModelException
	 *           if ...
	 * @throws ChannelException
	 *           if the specified channel does not exist TODO Filter model must have a list of channels. This is because
	 * 
	 */
    public FilterModel(String name, List<Channel> channels, String whereClause) throws FilterModelException, ChannelException {
        if (channels == null || channels.size() == 0) {
            throw new IllegalArgumentException("Needs channels.");
        }
        if (whereClause != null) {
            if (whereClause.indexOf('?') == -1) {
                throw new IllegalArgumentException("Non - null where clause needs value insert points ('?').");
            }
        }
        this._name = name;
        this.referencedChannels = channels;
        this.whereClause = whereClause;
        setChooseClause(new ChooseClause());
    }

    public String getWhereClause() {
        return whereClause;
    }

    public String getName() {
        return _name;
    }

    public Collection<Channel> getChannels() {
        return referencedChannels;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.getClass().getSimpleName());
        buffer.append(" channel=");
        buffer.append(referencedChannels.toString());
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
