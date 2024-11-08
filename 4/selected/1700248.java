package org.exist.synchro;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.exist.collections.Collection;
import org.exist.collections.CollectionConfigurationException;
import org.exist.collections.triggers.CollectionTrigger;
import org.exist.collections.triggers.TriggerException;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;
import org.jgroups.blocks.MethodCall;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class WatchCollection implements CollectionTrigger {

    private Communicator comm = null;

    @Override
    public void configure(DBBroker broker, Collection parent, Map<String, List<?>> parameters) throws CollectionConfigurationException {
        List<?> objs = parameters.get(Communicator.COMMUNICATOR);
        if (objs != null) comm = (Communicator) objs.get(0);
    }

    @Override
    public Logger getLogger() {
        return null;
    }

    @Override
    public void prepare(int event, DBBroker broker, Txn transaction, Collection collection, Collection newCollection) throws TriggerException {
    }

    @Override
    public void finish(int event, DBBroker broker, Txn transaction, Collection collection, Collection newCollection) {
    }

    @Override
    public void beforeCreateCollection(DBBroker broker, Txn transaction, XmldbURI uri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_CREATE_COLLECTION, comm.getChannel().getName(), uri));
    }

    @Override
    public void afterCreateCollection(DBBroker broker, Txn transaction, Collection collection) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_CREATE_COLLECTION, comm.getChannel().getName(), collection.getURI()));
    }

    @Override
    public void beforeCopyCollection(DBBroker broker, Txn transaction, Collection collection, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_COPY_COLLECTION, comm.getChannel().getName(), collection.getURI()));
    }

    @Override
    public void afterCopyCollection(DBBroker broker, Txn transaction, Collection collection, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_COPY_COLLECTION, comm.getChannel().getName(), collection.getURI()));
    }

    @Override
    public void beforeMoveCollection(DBBroker broker, Txn transaction, Collection collection, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_MOVE_COLLECTION, comm.getChannel().getName(), collection.getURI()));
    }

    @Override
    public void afterMoveCollection(DBBroker broker, Txn transaction, Collection collection, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_MOVE_COLLECTION, comm.getChannel().getName(), collection.getURI()));
    }

    @Override
    public void beforeDeleteCollection(DBBroker broker, Txn transaction, Collection collection) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_DELETE_COLLECTION, comm.getChannel().getName(), collection.getURI()));
    }

    @Override
    public void afterDeleteCollection(DBBroker broker, Txn transaction, XmldbURI uri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_DELETE_COLLECTION, comm.getChannel().getName(), uri));
    }
}
