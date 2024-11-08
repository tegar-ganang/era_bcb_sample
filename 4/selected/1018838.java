package ch.ethz.mxquery.xqj;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Properties;
import java.util.Vector;
import javax.xml.transform.Result;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import ch.ethz.mxquery.exceptions.MXQueryException;
import ch.ethz.mxquery.model.XDMIterator;
import ch.ethz.mxquery.query.PreparedStatement;
import ch.ethz.mxquery.util.PlatformDependentUtils;
import ch.ethz.repackaged.stream.XMLStreamReader;
import ch.ethz.repackaged.xquery.XQConnection;
import ch.ethz.repackaged.xquery.XQException;
import ch.ethz.repackaged.xquery.XQItem;
import ch.ethz.repackaged.xquery.XQItemType;
import ch.ethz.repackaged.xquery.XQQueryException;
import ch.ethz.repackaged.xquery.XQResultSequence;

public class MXQueryXQForwardSequence implements XQResultSequence {

    ItemAccessor ia;

    private MXQueryXQConnection connection;

    private MXQueryXQDynamicContext expression;

    int position = 0;

    boolean closed = false;

    boolean retrieved = false;

    private Vector store = new Vector();

    protected XQItem resultItem;

    protected MXQueryXQForwardSequence(XDMIterator iterator, MXQueryXQConnection connection, MXQueryXQDynamicContext expr) {
        ia = new ItemAccessor(iterator);
        this.connection = connection;
        expression = expr;
    }

    protected MXQueryXQForwardSequence(PreparedStatement statement, MXQueryXQConnection connection, MXQueryXQDynamicContext expr) throws XQException {
        ia = new ItemAccessor(statement);
        this.connection = connection;
        expression = expr;
    }

    public XQConnection getConnection() throws XQException {
        checkNotClosed();
        return connection;
    }

    public String getAtomicValue() throws XQException {
        return getCurrentXQItem(true).getAtomicValue();
    }

    public boolean getBoolean() throws XQException {
        return getCurrentXQItem(true).getBoolean();
    }

    public byte getByte() throws XQException {
        return getCurrentXQItem(true).getByte();
    }

    public double getDouble() throws XQException {
        return getCurrentXQItem(true).getDouble();
    }

    public float getFloat() throws XQException {
        return getCurrentXQItem(true).getFloat();
    }

    public int getInt() throws XQException {
        return getCurrentXQItem(true).getInt();
    }

    public XQItemType getItemType() throws XQException {
        return getCurrentXQItem(false).getItemType();
    }

    public long getLong() throws XQException {
        return getCurrentXQItem(true).getLong();
    }

    public Node getNode() throws XQException {
        return getCurrentXQItem(true).getNode();
    }

    public URI getNodeUri() throws XQException {
        return getCurrentXQItem(false).getNodeUri();
    }

    public Object getObject() throws XQException {
        return getCurrentXQItem(true).getObject();
    }

    public short getShort() throws XQException {
        return getCurrentXQItem(true).getShort();
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return getCurrentXQItem(false).instanceOf(type);
    }

    public boolean absolute(int itempos) throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void afterLast() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void beforeFirst() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void close() throws XQException {
        closed = true;
        for (int i = 0; i < this.store.size(); i++) {
            ((MXQueryXQItem) store.get(i)).close();
        }
        this.store.removeAllElements();
        ia = null;
    }

    public int count() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean first() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public XQItem getItem() throws XQException {
        checkNotClosed();
        checkAndSetRetrieved();
        if (this.resultItem == null) throw new XQException("Error in retrieving item!");
        return resultItem;
    }

    public int getPosition() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public String getSequenceAsString(Properties props) throws XQException {
        checkNotClosed();
        StringBuffer sb = new StringBuffer();
        if (this.retrieved) {
            throw new XQException("Forward only sequence, a get or write method has already been invoked on the current item");
        }
        if (this.position == 0) next();
        sb.append(getCurrentXQItem(true).getItemAsString(props));
        while (next()) {
            sb.append(" " + getCurrentXQItem(true).getItemAsString(props));
        }
        return sb.toString();
    }

    public boolean isAfterLast() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isBeforeFirst() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isClosed() {
        if (connection.isClosed() || expression.isClosed()) {
            closed = true;
        }
        return closed;
    }

    public boolean isFirst() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isLast() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean isOnItem() throws XQException {
        checkNotClosed();
        return position > 0;
    }

    public boolean isScrollable() throws XQException {
        checkNotClosed();
        return false;
    }

    public boolean last() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean next() throws XQException {
        checkNotClosed();
        if (position < 0) {
            return false;
        }
        try {
            Item it = (Item) ia.next();
            if (it == null) {
                position = -1;
                return false;
            } else {
                position++;
                resultItem = new MXQueryXQItem(connection, it);
                this.store.add(resultItem);
                retrieved = false;
                return true;
            }
        } catch (MXQueryException e) {
            throw new XQQueryException(e.toString(), PlatformDependentUtils.getJavaxQName(e.getErrorCode()));
        } catch (IOException e) {
            return false;
        }
    }

    public boolean previous() throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public boolean relative(int itempos) throws XQException {
        throw new XQException("Sequence is forwards-only");
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        checkNotClosed();
        getCurrentXQItem(true).writeItem(os, props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        checkNotClosed();
        getCurrentXQItem(true).writeItem(ow, props);
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        getCurrentXQItem(true).writeItemToSAX(saxHandler);
    }

    public void writeItemToResult(Result result) throws XQException {
        getCurrentXQItem(true).writeItemToResult(result);
    }

    public void writeSequence(OutputStream os, Properties props) throws XQException {
        checkNotClosed();
        boolean hasNext = true;
        if (this.position == 0) hasNext = this.next();
        while (hasNext) {
            getCurrentXQItem(true).writeItem(os, props);
            try {
                os.write(' ');
            } catch (IOException e) {
                throw new XQQueryException("Could not write sequence " + e);
            }
            hasNext = next();
        }
        ;
    }

    public void writeSequence(Writer ow, Properties props) throws XQException {
        checkNotClosed();
        boolean hasNext = true;
        if (this.position == 0) hasNext = this.next();
        while (hasNext) {
            getCurrentXQItem(true).writeItem(ow, props);
            try {
                ow.write(' ');
            } catch (IOException e) {
                throw new XQQueryException("Could not write sequence " + e);
            }
            hasNext = next();
        }
        ;
    }

    public void writeSequenceToSAX(ContentHandler saxhdlr) throws XQException {
        checkNotClosed();
        boolean hasNext = true;
        if (position == 0) {
            hasNext = this.next();
        }
        if (position < 0) {
            throw new XQException("The XQSequence is positioned after the last item");
        }
        while (hasNext) {
            checkAndSetRetrieved();
            resultItem.writeItemToSAX(saxhdlr);
            hasNext = next();
        }
        ;
    }

    protected void checkNotClosed() throws XQException {
        if (connection.isClosed()) {
            close();
        }
        if (isClosed()) {
            throw new XQException("Sequence has been closed");
        }
    }

    protected void checkAndSetRetrieved() throws XQException {
        if (retrieved) {
            throw new XQException("Item has already been retrieved");
        }
        retrieved = true;
    }

    private XQItem getCurrentXQItem(boolean retrievedCheck) throws XQException {
        checkNotClosed();
        if (retrievedCheck) checkAndSetRetrieved();
        if (position == 0) {
            throw new XQException("The XQSequence is positioned before the first item");
        } else if (position < 0) {
            throw new XQException("The XQSequence is positioned after the last item");
        }
        return resultItem;
    }

    public XMLStreamReader getSequenceAsStream() throws XQException {
        throw new UnsupportedOperationException("Stax is not implemented!");
    }

    public void writeSequenceToResult(Result result) throws XQException {
        checkNotClosed();
        boolean hasNext = true;
        if (position == 0) {
            hasNext = this.next();
        }
        if (position < 0) {
            throw new XQException("The XQSequence is positioned after the last item");
        }
        while (hasNext) {
            checkAndSetRetrieved();
            resultItem.writeItemToResult(result);
            hasNext = next();
        }
        ;
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        checkNotClosed();
        if (position == 0) {
            throw new XQException("The XQSequence is positioned before the first item");
        } else if (position < 0) {
            throw new XQException("The XQSequence is positioned after the last item");
        }
        checkAndSetRetrieved();
        return resultItem.getItemAsStream();
    }

    public String getItemAsString(Properties props) throws XQException {
        return getCurrentXQItem(true).getItemAsString(props);
    }
}
