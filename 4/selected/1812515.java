package org.jage.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jage.address.IAgentAddress;
import org.jage.property.DuplicatePropertyNameException;
import org.jage.property.InvalidPropertyOperationException;
import org.jage.property.MetaProperty;
import org.jage.property.SimpleProperty;

/**
 * This class is the implementation of a result of performing a query.
 * 
 * @author KrzS
 */
public class QueryResult extends AbstractQueryResult {

    /**
	 * SerialVersionUID
	 */
    private static final long serialVersionUID = 1537397587734831731L;

    /**
	 * Logger
	 */
    private static final Logger _log = Logger.getLogger(QueryResult.class);

    /**
	 * Map of entries of this result of a query. <BR>
	 * Key: address (Address) <BR>
	 * Value: result's entry (IQueryResult.Entry) <BR>
	 */
    private final Map<IAgentAddress, IQueryResult.Entry> _entries;

    /**
	 * Constructor.
	 */
    public QueryResult() {
        this(false);
    }

    /**
	 * Constructor.
	 * 
	 * @param emptyQuery
	 *            <TT>true</TT> if this result is based on performing the
	 *            empty query, so it contains properties of all available
	 *            entities; <TT> false</TT>- otherwise
	 */
    public QueryResult(boolean emptyQuery) {
        super(emptyQuery);
        _entries = Collections.synchronizedMap(new HashMap<IAgentAddress, IQueryResult.Entry>());
    }

    /**
	 * Constructor creating result of a query based on another result.
	 * 
	 * @param result
	 *            the result of a query
	 * @param emptyQuery
	 *            <TT>true</TT> if this result is based on performing the
	 *            empty query, so it contains properties of all available
	 *            entities; <TT> false</TT>- otherwise
	 */
    public QueryResult(IQueryResult result, boolean emptyQuery) {
        super(result, emptyQuery);
        Map<IAgentAddress, IQueryResult.Entry> entries = new HashMap<IAgentAddress, IQueryResult.Entry>();
        for (IQueryResult.Entry entry : result.entries()) {
            entries.put(entry.getAddress(), entry);
        }
        _entries = Collections.synchronizedMap(entries);
    }

    /**
	 * @see org.jage.query.IQueryResult#addEntry(org.jage.query.IQueryResult.Entry)
	 */
    public void addEntry(IQueryResult.Entry entry) {
        _entries.put(entry.getAddress(), entry);
    }

    /**
	 * Adds a query result to this query result.
	 * 
	 * @param result
	 *            the query result to add
	 */
    public void addAll(IQueryResult result) {
        addEntries(result.entries());
        for (Iterator iter = result.getMetaPropertiesSet().iterator(); iter.hasNext(); ) {
            MetaProperty metaProperty = (MetaProperty) iter.next();
            String propertyName = metaProperty.getName();
            try {
                Object propertyValue = result.getProperty(propertyName).getValue();
                if (this.getMetaPropertiesSet().hasMetaProperty(propertyName) && this.getMetaPropertiesSet().getMetaProperty(propertyName).isWriteable()) {
                    this.getProperty(propertyName).setValue(propertyValue);
                } else {
                    this.addProperty(new SimpleProperty(metaProperty, propertyValue));
                }
            } catch (InvalidPropertyOperationException ex) {
                _log.fatal("Cannot read readable property or write writeable property.");
            } catch (DuplicatePropertyNameException ex) {
                _log.fatal("Properties with duplicated names existed in original query result.");
            }
        }
    }

    /**
	 * @see org.jage.query.IQueryResult#entries()
	 */
    public Collection<IQueryResult.Entry> entries() {
        return Collections.unmodifiableCollection(_entries.values());
    }

    /**
	 * @see org.jage.query.IQueryResult#getEntry(org.jage.address.IAgentAddress)
	 */
    public IQueryResult.Entry getEntry(IAgentAddress address) {
        return _entries.get(address);
    }

    /**
	 * @see org.jage.query.IQueryResult#getEntryCount()
	 */
    public int getEntryCount() {
        return _entries.size();
    }

    /**
	 * @see org.jage.query.IQueryResult#removeEntry(org.jage.address.IAgentAddress)
	 */
    public IQueryResult.Entry removeEntry(IAgentAddress address) {
        return _entries.remove(address);
    }

    /**
	 * @see org.jage.query.AbstractQueryResult#getNewQueryResult()
	 */
    @Override
    protected IQueryResult getNewQueryResult() {
        return new QueryResult();
    }

    /**
	 * @see org.jage.query.IQueryable#query(org.jage.query.Query)
	 */
    public IQueryResult query(Query query) {
        return super.query(query, _entries);
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        return "[ QueryResult " + (_entries.isEmpty() ? "empty" : "..") + " ]";
    }
}
