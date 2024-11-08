package org.exist.synchro;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.exist.collections.Collection;
import org.exist.collections.CollectionConfigurationException;
import org.exist.collections.triggers.DocumentTrigger;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.DocumentImpl;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;
import org.jgroups.blocks.MethodCall;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class WatchDocument implements DocumentTrigger {

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
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void prepare(int event, DBBroker broker, Txn transaction, XmldbURI documentPath, DocumentImpl existingDocument) throws TriggerException {
    }

    @Override
    public void finish(int event, DBBroker broker, Txn transaction, XmldbURI documentPath, DocumentImpl document) {
    }

    @Override
    public void beforeCreateDocument(DBBroker broker, Txn transaction, XmldbURI uri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_CREATE_DOCUMENT, comm.getChannel().getName(), uri));
    }

    @Override
    public void afterCreateDocument(DBBroker broker, Txn transaction, DocumentImpl document) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_CREATE_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void beforeUpdateDocument(DBBroker broker, Txn transaction, DocumentImpl document) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_UPDATE_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void afterUpdateDocument(DBBroker broker, Txn transaction, DocumentImpl document) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_UPDATE_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void beforeCopyDocument(DBBroker broker, Txn transaction, DocumentImpl document, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_COPY_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void afterCopyDocument(DBBroker broker, Txn transaction, DocumentImpl document, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_COPY_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void beforeMoveDocument(DBBroker broker, Txn transaction, DocumentImpl document, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_MOVE_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void afterMoveDocument(DBBroker broker, Txn transaction, DocumentImpl document, XmldbURI newUri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_MOVE_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void beforeDeleteDocument(DBBroker broker, Txn transaction, DocumentImpl document) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.BEFORE_DELETE_DOCUMENT, comm.getChannel().getName(), document.getURI()));
    }

    @Override
    public void afterDeleteDocument(DBBroker broker, Txn transaction, XmldbURI uri) throws TriggerException {
        if (comm == null) return;
        comm.callRemoteMethods(new MethodCall(Communicator.AFTER_DELETE_DOCUMENT, comm.getChannel().getName(), uri));
    }

    @Override
    public boolean isValidating() {
        return false;
    }

    @Override
    public void setValidating(boolean validating) {
    }

    @Override
    public void setOutputHandler(ContentHandler handler) {
    }

    @Override
    public void setLexicalOutputHandler(LexicalHandler handler) {
    }

    @Override
    public ContentHandler getOutputHandler() {
        return null;
    }

    @Override
    public ContentHandler getInputHandler() {
        return null;
    }

    @Override
    public LexicalHandler getLexicalOutputHandler() {
        return null;
    }

    @Override
    public LexicalHandler getLexicalInputHandler() {
        return null;
    }
}
