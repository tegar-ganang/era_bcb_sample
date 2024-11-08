package org.exist.webdav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.IndexInfo;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.DocumentImpl;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.util.LockException;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.util.VirtualTempFile;
import org.exist.util.VirtualTempFileInputSource;
import org.exist.webdav.exceptions.CollectionDoesNotExistException;
import org.exist.webdav.exceptions.CollectionExistsException;
import org.exist.xmldb.XmldbURI;
import org.xml.sax.SAXException;

/**
 * Class for accessing the Collection class of the exist-db native API.
 *
 * @author Dannes Wessels (dizzzz_at_exist-db.org)
 */
public class ExistCollection extends ExistResource {

    public ExistCollection(XmldbURI uri, BrokerPool pool) {
        if (LOG.isTraceEnabled()) LOG.trace("New collection object for " + uri);
        brokerPool = pool;
        this.xmldbUri = uri;
    }

    /**
     * Initialize Collection, authenticate() is required first
     */
    @Override
    public void initMetadata() {
        if (subject == null) {
            LOG.error("User not initialized yet");
            return;
        }
        if (isInitialized) {
            LOG.debug("Already initialized");
            return;
        }
        DBBroker broker = null;
        Collection collection = null;
        try {
            broker = brokerPool.get(subject);
            collection = broker.openCollection(xmldbUri, Lock.READ_LOCK);
            if (collection == null) {
                LOG.error("Collection for " + xmldbUri + " cannot be opened for  metadata");
                return;
            }
            permissions = collection.getPermissions();
            readAllowed = permissions.validate(subject, Permission.READ);
            writeAllowed = permissions.validate(subject, Permission.WRITE);
            executeAllowed = permissions.validate(subject, Permission.EXECUTE);
            creationTime = collection.getCreationTime();
            lastModified = creationTime;
            ownerUser = permissions.getOwner().getUsername();
            ownerGroup = permissions.getGroup().getName();
        } catch (PermissionDeniedException pde) {
            LOG.error(pde);
        } catch (EXistException e) {
            LOG.error(e);
        } finally {
            if (collection != null) {
                collection.release(Lock.READ_LOCK);
            }
            brokerPool.release(broker);
            isInitialized = true;
        }
    }

    /**
     * Retrieve full URIs of all Collections in this collection.
     */
    public List<XmldbURI> getCollectionURIs() {
        List<XmldbURI> collectionURIs = new ArrayList<XmldbURI>();
        DBBroker broker = null;
        Collection collection = null;
        try {
            broker = brokerPool.get(subject);
            collection = broker.openCollection(xmldbUri, Lock.READ_LOCK);
            Iterator<XmldbURI> collections = collection.collectionIteratorNoLock(broker);
            while (collections.hasNext()) {
                collectionURIs.add(xmldbUri.append(collections.next()));
            }
        } catch (EXistException e) {
            LOG.error(e);
            collectionURIs = null;
        } catch (PermissionDeniedException pde) {
            LOG.error(pde);
            collectionURIs = null;
        } finally {
            if (collection != null) {
                collection.release(Lock.READ_LOCK);
            }
            brokerPool.release(broker);
        }
        return collectionURIs;
    }

    /**
     * Retrieve full URIs of all Documents in the collection.
     */
    public List<XmldbURI> getDocumentURIs() {
        List<XmldbURI> documentURIs = new ArrayList<XmldbURI>();
        DBBroker broker = null;
        Collection collection = null;
        try {
            broker = brokerPool.get(subject);
            collection = broker.openCollection(xmldbUri, Lock.READ_LOCK);
            Iterator<DocumentImpl> documents = collection.iteratorNoLock(broker);
            while (documents.hasNext()) {
                documentURIs.add(documents.next().getURI());
            }
        } catch (PermissionDeniedException e) {
            LOG.error(e);
            documentURIs = null;
        } catch (EXistException e) {
            LOG.error(e);
            documentURIs = null;
        } finally {
            if (collection != null) {
                collection.release(Lock.READ_LOCK);
            }
            brokerPool.release(broker);
        }
        return documentURIs;
    }

    void delete() {
        if (LOG.isDebugEnabled()) LOG.debug("Deleting '" + xmldbUri + "'");
        DBBroker broker = null;
        Collection collection = null;
        TransactionManager transact = brokerPool.getTransactionManager();
        Txn txn = transact.beginTransaction();
        try {
            broker = brokerPool.get(subject);
            collection = broker.openCollection(xmldbUri, Lock.WRITE_LOCK);
            if (collection == null) {
                transact.abort(txn);
                return;
            }
            broker.removeCollection(txn, collection);
            transact.commit(txn);
            if (LOG.isDebugEnabled()) LOG.debug("Document deleted sucessfully");
        } catch (EXistException e) {
            LOG.error(e);
            transact.abort(txn);
        } catch (IOException e) {
            LOG.error(e);
            transact.abort(txn);
        } catch (PermissionDeniedException e) {
            LOG.error(e);
            transact.abort(txn);
        } catch (TriggerException e) {
            LOG.error(e);
            transact.abort(txn);
        } finally {
            if (collection != null) {
                collection.release(Lock.WRITE_LOCK);
            }
            brokerPool.release(broker);
            if (LOG.isDebugEnabled()) LOG.debug("Finished delete");
        }
    }

    public XmldbURI createCollection(String name) throws PermissionDeniedException, CollectionExistsException, EXistException {
        if (LOG.isDebugEnabled()) LOG.debug("Create  '" + name + "' in '" + xmldbUri + "'");
        XmldbURI newCollection = xmldbUri.append(name);
        DBBroker broker = null;
        Collection collection = null;
        TransactionManager transact = brokerPool.getTransactionManager();
        Txn txn = transact.beginTransaction();
        try {
            broker = brokerPool.get(subject);
            collection = broker.openCollection(newCollection, Lock.WRITE_LOCK);
            if (collection != null) {
                LOG.debug("Collection already exists");
                transact.abort(txn);
                throw new CollectionExistsException("Collection already exists");
            }
            Collection created = broker.getOrCreateCollection(txn, newCollection);
            broker.saveCollection(txn, created);
            broker.flush();
            transact.commit(txn);
            if (LOG.isDebugEnabled()) LOG.debug("Collection created sucessfully");
        } catch (EXistException e) {
            LOG.error(e);
            transact.abort(txn);
            throw e;
        } catch (IOException e) {
            LOG.error(e);
            transact.abort(txn);
        } catch (PermissionDeniedException e) {
            LOG.error(e);
            transact.abort(txn);
            throw e;
        } catch (Throwable e) {
            LOG.error(e);
            transact.abort(txn);
            throw new EXistException(e);
        } finally {
            if (collection != null) {
                collection.release(Lock.WRITE_LOCK);
            }
            brokerPool.release(broker);
            if (LOG.isDebugEnabled()) LOG.debug("Finished creation");
        }
        return newCollection;
    }

    public XmldbURI createFile(String newName, InputStream is, Long length, String contentType) throws IOException, PermissionDeniedException, CollectionDoesNotExistException {
        if (LOG.isDebugEnabled()) LOG.debug("Create '" + newName + "' in '" + xmldbUri + "'");
        XmldbURI newNameUri = XmldbURI.create(newName);
        MimeType mime = MimeTable.getInstance().getContentTypeFor(newName);
        if (mime == null) {
            mime = MimeType.BINARY_TYPE;
        }
        DBBroker broker = null;
        Collection collection = null;
        BufferedInputStream bis = new BufferedInputStream(is);
        VirtualTempFile vtf = new VirtualTempFile();
        BufferedOutputStream bos = new BufferedOutputStream(vtf);
        IOUtils.copy(bis, bos);
        bis.close();
        bos.close();
        vtf.close();
        if (mime.isXMLType() && vtf.length() == 0L) {
            if (LOG.isDebugEnabled()) LOG.debug("Creating dummy XML file for null resource lock '" + newNameUri + "'");
            vtf = new VirtualTempFile();
            IOUtils.write("<null_resource/>", vtf);
            vtf.close();
        }
        TransactionManager transact = brokerPool.getTransactionManager();
        Txn txn = transact.beginTransaction();
        try {
            broker = brokerPool.get(subject);
            collection = broker.openCollection(xmldbUri, Lock.WRITE_LOCK);
            if (collection == null) {
                LOG.debug("Collection " + xmldbUri + " does not exist");
                transact.abort(txn);
                throw new CollectionDoesNotExistException(xmldbUri + "");
            }
            if (mime.isXMLType()) {
                if (LOG.isDebugEnabled()) LOG.debug("Inserting XML document '" + mime.getName() + "'");
                VirtualTempFileInputSource vtfis = new VirtualTempFileInputSource(vtf);
                IndexInfo info = collection.validateXMLResource(txn, broker, newNameUri, vtfis);
                DocumentImpl doc = info.getDocument();
                doc.getMetadata().setMimeType(mime.getName());
                collection.store(txn, broker, info, vtfis, false);
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("Inserting BINARY document '" + mime.getName() + "'");
                InputStream fis = vtf.getByteStream();
                bis = new BufferedInputStream(fis);
                DocumentImpl doc = collection.addBinaryResource(txn, broker, newNameUri, bis, mime.getName(), length.longValue());
                bis.close();
            }
            transact.commit(txn);
            if (LOG.isDebugEnabled()) LOG.debug("Document created sucessfully");
        } catch (EXistException e) {
            LOG.error(e);
            transact.abort(txn);
            throw new IOException(e);
        } catch (TriggerException e) {
            LOG.error(e);
            transact.abort(txn);
            throw new IOException(e);
        } catch (SAXException e) {
            LOG.error(e);
            transact.abort(txn);
            throw new IOException(e);
        } catch (LockException e) {
            LOG.error(e);
            transact.abort(txn);
            throw new PermissionDeniedException(xmldbUri + "");
        } catch (IOException e) {
            LOG.error(e);
            transact.abort(txn);
            throw e;
        } catch (PermissionDeniedException e) {
            LOG.error(e);
            transact.abort(txn);
            throw e;
        } finally {
            if (vtf != null) {
                vtf.delete();
            }
            if (collection != null) {
                collection.release(Lock.WRITE_LOCK);
            }
            brokerPool.release(broker);
            if (LOG.isDebugEnabled()) LOG.debug("Finished creation");
        }
        XmldbURI newResource = xmldbUri.append(newName);
        return newResource;
    }

    void resourceCopyMove(XmldbURI destCollectionUri, String newName, Mode mode) throws EXistException {
        if (LOG.isDebugEnabled()) LOG.debug(mode + " '" + xmldbUri + "' to '" + destCollectionUri + "' named '" + newName + "'");
        XmldbURI newNameUri = null;
        try {
            newNameUri = XmldbURI.xmldbUriFor(newName);
        } catch (URISyntaxException ex) {
            LOG.error(ex);
            throw new EXistException(ex.getMessage());
        }
        DBBroker broker = null;
        Collection srcCollection = null;
        Collection destCollection = null;
        TransactionManager txnManager = brokerPool.getTransactionManager();
        Txn txn = txnManager.beginTransaction();
        try {
            broker = brokerPool.get(subject);
            XmldbURI srcCollectionUri = xmldbUri;
            srcCollection = broker.openCollection(srcCollectionUri, Lock.WRITE_LOCK);
            if (srcCollection == null) {
                txnManager.abort(txn);
                return;
            }
            destCollection = broker.openCollection(destCollectionUri, Lock.WRITE_LOCK);
            if (destCollection == null) {
                LOG.debug("Destination collection " + xmldbUri + " does not exist.");
                txnManager.abort(txn);
                return;
            }
            if (mode == Mode.COPY) {
                broker.copyCollection(txn, srcCollection, destCollection, newNameUri);
            } else {
                broker.moveCollection(txn, srcCollection, destCollection, newNameUri);
            }
            txnManager.commit(txn);
            if (LOG.isDebugEnabled()) LOG.debug("Collection " + mode + "d sucessfully");
        } catch (LockException e) {
            LOG.error("Resource is locked.", e);
            txnManager.abort(txn);
            throw new EXistException(e.getMessage());
        } catch (EXistException e) {
            LOG.error(e);
            txnManager.abort(txn);
            throw e;
        } catch (IOException e) {
            LOG.error(e);
            txnManager.abort(txn);
            throw new EXistException(e.getMessage());
        } catch (PermissionDeniedException e) {
            LOG.error(e);
            txnManager.abort(txn);
            throw new EXistException(e.getMessage());
        } catch (TriggerException e) {
            LOG.error(e);
            txnManager.abort(txn);
            throw new EXistException(e.getMessage());
        } finally {
            if (destCollection != null) {
                destCollection.release(Lock.WRITE_LOCK);
            }
            if (srcCollection != null) {
                srcCollection.release(Lock.WRITE_LOCK);
            }
            brokerPool.release(broker);
            if (LOG.isDebugEnabled()) LOG.debug("Finished " + mode);
        }
    }
}
