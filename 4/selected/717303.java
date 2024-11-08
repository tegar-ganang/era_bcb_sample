package org.xaware.server.engine.controller;

import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.xaware.api.IBizViewRequestOptions;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IBizViewSession;
import org.xaware.server.engine.IChannelScope;
import org.xaware.server.engine.IScriptNode;
import org.xaware.server.engine.IScriptProcessState;
import org.xaware.server.engine.IScriptProcessingError;
import org.xaware.server.engine.IScriptStatistics;
import org.xaware.server.engine.ISessionStateRegistry;
import org.xaware.server.engine.ITransactionContext;
import org.xaware.server.engine.context.BizDocContext;
import org.xaware.server.engine.context.OutStreamConfigTranslator;
import org.xaware.server.engine.context.StreamingBizDocContext;
import org.xaware.server.engine.context.SubstitutionHelper;
import org.xaware.server.engine.controller.transaction.TransactionContext;
import org.xaware.server.engine.enums.SubstitutionFailureLevel;
import org.xaware.server.engine.enums.TransactionPropagation;
import org.xaware.server.engine.exceptions.XAwareEarlyTerminationException;
import org.xaware.server.engine.exceptions.XAwareExitException;
import org.xaware.server.engine.exceptions.XAwareProcessingException;
import org.xaware.server.engine.exceptions.XAwareResponseException;
import org.xaware.server.engine.exceptions.XAwareUserException;
import org.xaware.server.resources.BizViewFactory;
import org.xaware.server.resources.XAwareBeanFactory;
import org.xaware.server.streaming.XmlOutputStreamer;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class maintains the high-level execution state for
 * a BizViewSession.
 * @author Tim Uttormark
 */
public class SessionStateRegistry implements ISessionStateRegistry {

    private static String CLASS_NAME = SessionStateRegistry.class.getName();

    private static XAwareLogger logger = XAwareLogger.getXAwareLogger(CLASS_NAME);

    /**
     * define the prefix that designates a session variable
     */
    public static final String SESSION_VARIABLE_PREFIX = "xavar:";

    protected Stack<IScriptNode> callStack;

    protected IBizViewContext rootContext = null;

    protected IBizViewSession session = null;

    private IScriptProcessingError currentError = null;

    private String previousErrorMessage = null;

    private String previousStackTrace = null;

    private boolean abort = false;

    private XmlOutputStreamer rootOutputStreamer = null;

    private List<Exception> parkedExceptions = new ArrayList<Exception>();

    /**
     * This map holds session level variables and their corresponding value.
     * All system variables are referenced as $xavar:[name]$ where the name is
     * replaced by the actual name used.  For example, $xavar:match_stream$ is 
     * a system variable with the name 'match_stream'.
     */
    protected Map<String, Object> systemVariables = null;

    /**
     * This is intended to get around the issue we have with the logging guard methods not returning the
     * correct response when the log level is higher than DEBUG.
     */
    private boolean dumpStack = true;

    protected SessionStateRegistry(IBizViewSession session) throws XAwareException {
        if (session == null) {
            throw new IllegalArgumentException("A non-null IBizViewSession must be provided");
        }
        this.session = session;
        callStack = new Stack<IScriptNode>();
    }

    /**
     * Constructor. Performs initialization based on the {@link IBizViewRequestOptions} provided.
     * 
     * @param options
     *            {@link IBizViewRequestOptions} with all the relevant data necessary to excute the specified BizView
     * @param session
     *            {@link IBizViewSession} in which the execution is to take place.
     * @throws XAwareException
     *             If there is a problem with the request options or if processing errors occurred during
     *             initialization.
     */
    public SessionStateRegistry(IBizViewRequestOptions options, IBizViewSession session) throws XAwareException {
        this(session);
        if (options == null) {
            throw new IllegalArgumentException("A non-null IBizViewRequestOptions object must be provided");
        }
        if (options.getBizViewName() == null || "".equals(options.getBizViewName().trim())) {
            throw new IllegalArgumentException("A non-null non-empty name must be supplied for the BizView");
        }
        if (options.getSessionVariables() == null) systemVariables = new HashMap<String, Object>(); else systemVariables = options.getSessionVariables();
        setupOverridingOutputStreaming(options);
        String viewName = getBizViewNameFromRequestOptions(options);
        Document inputXml = options.getInputXmlDocument();
        String inputXmlString = null;
        Reader inputXmlReader = null;
        if (inputXml == null) {
            inputXmlString = options.getInputXmlSerialized();
            if (inputXmlString == null) {
                inputXmlString = options.getInputXmlResourceName();
                if (inputXmlString == null) {
                    inputXmlReader = options.getInputXmlReader();
                }
            }
        }
        Map<String, Object> inputParams = options.getInputParams();
        BizDocContext bdContext = null;
        Document bizView = getBizViewDocumentFromRequestOptions(options);
        if (inputXmlString != null) {
            bdContext = new BizDocContext(viewName, bizView, inputParams, inputXmlString, this);
        } else if (inputXmlReader != null) {
            bdContext = new BizDocContext(viewName, bizView, inputParams, inputXmlReader, this);
        } else {
            bdContext = new BizDocContext(viewName, bizView, inputParams, inputXml, this);
        }
        push(bdContext);
    }

    /**
     * The user-supplied OutputStream into which results should be outstreamed. If non-null, all results of BizDoc
     * execution are streamed out into this OutputStream (unless the OutputStream is overridden by an xa:stream Element
     * within the Bizview). Set to null if results are not to be sent to a user-supplied OutputStream.
     * 
     * @param options
     * @throws XAwareException
     *             There is a problem setting up the {@link XmlOutputStreamer}.
     */
    private void setupOverridingOutputStreaming(IBizViewRequestOptions options) throws XAwareException {
        OutputStream os = options.getOutputStream();
        if (os != null) {
            rootOutputStreamer = new XmlOutputStreamer(os);
        } else {
            String outputFileName = options.getOutputStreamName();
            if (outputFileName != null && !"".equals(outputFileName.trim())) {
                File outputFile = new File(outputFileName);
                if (!outputFile.exists()) {
                    File parentDir = outputFile.getParentFile();
                    if (!parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                }
                os = session.getOutputStream(outputFileName, false);
            }
        }
    }

    /**
     * @param options
     * @return
     */
    private String getBizViewNameFromRequestOptions(IBizViewRequestOptions options) {
        return options.getBizViewName();
    }

    /**
     * @param options
     * @return
     * @throws XAwareException There is a problem creating the BizView JDOM structure.
     */
    private Document getBizViewDocumentFromRequestOptions(IBizViewRequestOptions options) throws XAwareException {
        if (!options.isBizDocument()) {
            BizCompExecutionHelper.setUpBizCompExecute(options);
        }
        Document viewDocument = options.getBizViewDocument();
        if (viewDocument != null) {
            return viewDocument;
        }
        BizViewFactory factory = XAwareBeanFactory.getResourceManager().getBizDocumentFactory();
        String resourceSpec = options.getBizViewName();
        if (resourceSpec != null && !"".equals(resourceSpec.trim())) {
            return factory.createInstance(resourceSpec);
        }
        throw new XAwareException("No JDOM structure specified");
    }

    /**
     * set the IBizViewSession
     * 
     * @param an IBizViewSession
     */
    public void setSession(IBizViewSession p_session) {
        this.session = p_session;
    }

    /**
     * @return Returns the IBizViewSession object that contains this Session
     * State Registory
     */
    public IBizViewSession getBizViewSession() {
        return session;
    }

    /**
     * Acquire the session identifier from the BizViewSession
     * @return Returns the session identifier
     */
    public String getSessionID() {
        return getBizViewSession().getId();
    }

    /**
     * Returns the name of the BizView being run.
     * @return a String containing the name of the BizView
     * being run.
     */
    public String getBizViewName() {
        return rootContext.getBizViewName();
    }

    /**
     * Returns a boolean indicating whether execution
     * of the BizView is complete.
     * @return a boolean indicating whether execution
     * of the BizView is complete.
     */
    public boolean processingComplete() {
        return callStack.isEmpty() && parkedExceptions.isEmpty();
    }

    /**
     * Returns an IScriptNode reference to the ScriptNode at the top
     * of the call stack within the registry, without removing it
     * from the call stack.
     * @return an IScriptNode reference to the ScriptNode at the top
     * of the node call stack within the registry.
     * @throws EmptyStackException if the call stack is empty.
     */
    public IScriptNode peek() {
        return callStack.peek();
    }

    /**
     * Completes processing of the currently executing ScriptNode
     * by closing the OutputStreamer (if this node opened it),
     * finishing the current transaction (if this node
     * demarcated the transaction) and popping the top ScriptNode
     * off of the call stack.
     * @param success a boolean indicating whether execution of 
     * the node was successful.  This is used to determine whether
     * to commit the transaction demarcated by the node
     * currently being executed.
     * @return an IScriptNode reference to the node popped off
     * the call stack.
     * @throws XAwareException 
     * @throws EmptyStackException if the call stack is empty.
     */
    public IScriptNode finishNode(boolean success) throws XAwareException {
        if (callStack.isEmpty()) {
            return null;
        }
        IScriptNode finishedNode = callStack.pop();
        if (logger.isDebugEnabled()) {
            logger.debug(dumpStack("pop"), CLASS_NAME, "finishNode");
        }
        try {
            finishedNode.complete(success);
        } finally {
            try {
                if (finishedNode.startsNewChannelScope() && success) {
                    finishedNode.getChannelScope().complete(success);
                }
            } catch (RuntimeException e) {
                String errMsg = "Failed to close channel: " + e;
                logger.severe(errMsg, CLASS_NAME, "finishNode");
                throw new XAwareException(errMsg, e);
            } finally {
                try {
                    if (finishedNode.startsNewOutputStream()) {
                        finishedNode.getOutputStreamer().closeStream();
                    }
                } catch (RuntimeException e) {
                    String errMsg = "Failed to close OutputStream: " + e;
                    logger.severe(errMsg, CLASS_NAME, "finishNode");
                    throw new XAwareException(errMsg, e);
                } finally {
                    try {
                        if (finishedNode.getContext().getScriptRoot() == finishedNode.getElement()) {
                            finishedNode.getContext().endPassProcessing();
                        }
                    } finally {
                        if (finishedNode.startsNewTransactionScope()) {
                            ITransactionContext transactionContext = finishedNode.getTransactionContext();
                            boolean processingComplete = this.processingComplete();
                            if (!processingComplete) {
                                logger.finer("completing transaction for " + transactionContext.getTransactionName() + " with success = " + success, CLASS_NAME, "finishNode");
                            }
                            transactionContext.complete(success, processingComplete);
                        }
                    }
                }
            }
        }
        return finishedNode;
    }

    /**
     * Creates a new ScriptNode for the root element of the
     * IBizViewContext provided and pushes it on the call stack.
     * If the BizViewContext is streaming then create the streaming context 
     * and place it on the stack.  The streaming context will handle
     * streaming in and the adding of the BizDocument to the stack for each
     * iteration through the input stream XML.
     * Also, if the context declares an OutputStream, associate
     * the XmlOutputStreamer with the startingScriptNode of the Context.
     * @param context The new BizViewContext to be executed.
     * @throws XAwareException if an processing error occurs
     * initializing the StreamingBizDocContext or the startingScriptNode.
     */
    public void push(IBizViewContext context) throws XAwareException {
        IScriptNode startingScriptNode = context.getStartingScriptNode();
        if (preconfigureScriptNode(startingScriptNode)) {
            try {
                context.configure();
                if (context.isStreamingIn()) {
                    Element rootElem = startingScriptNode.getElement();
                    rootElem.setAttribute(XAwareConstants.ON_SUBSTITUTE_FAILURE_ATTR, startingScriptNode.getEffectiveSubstitutionFailureLevel().getValue(), XAwareConstants.xaNamespace);
                    String errHandlerPath = startingScriptNode.getPathToErrorHandler();
                    if (errHandlerPath != null) {
                        rootElem.setAttribute(XAwareConstants.BIZDOCUMENT_ATTR_ON_ERROR, errHandlerPath, XAwareConstants.xaNamespace);
                    }
                    TransactionPropagation tp = startingScriptNode.getDeclaredTransactionPropagation();
                    if (tp != null) {
                        rootElem.setAttribute(XAwareConstants.BIZDOCUMENT_ATTR_TRANSACTION, tp.getAttributeValue(), XAwareConstants.xaNamespace);
                    }
                    if (startingScriptNode.startsNewTransactionScope()) {
                        startingScriptNode.getTransactionContext().complete(false, false);
                    }
                    push(new StreamingBizDocContext(context));
                    return;
                }
                OutStreamConfigTranslator streamingConfig = context.getOutStreamConfig();
                if (streamingConfig != null) {
                    if ((callStack.isEmpty()) || (!peek().isInsidePruneElement())) {
                        XmlOutputStreamer streamer = new XmlOutputStreamer(streamingConfig);
                        streamer.setupOutputStream(startingScriptNode);
                        startingScriptNode.setOutputStreamer(streamer);
                    }
                }
            } catch (Exception e) {
                parkException(e);
            }
        }
        if (rootContext == null) {
            rootContext = context;
        }
        inheritValuesAndPush(startingScriptNode);
    }

    /**
     * Performs early configuration steps on the ScriptNode provided,
     * and pushes the ScriptNode on the call stack.
     * @param node the IScriptNode to execute next.
     * @throws XAwareException 
     */
    public void push(IScriptNode node) throws XAwareException {
        preconfigureScriptNode(node);
        inheritValuesAndPush(node);
    }

    /**
     * Performs early configuration steps on a ScriptNode.
     * Returns a boolean indication of whether these configuration
     * steps succeeded.  If not, the ScriptNode should not be pushed
     * onto the call stack.
     * @param node the ScriptNode to process.
     * @return a boolean indicating whether configuration succeeded.
     */
    private boolean preconfigureScriptNode(IScriptNode node) {
        boolean success = true;
        try {
            SubstitutionFailureLevel inheritedSubstLevel = getInheritedSubstitutionFailureLevel();
            node.setEffectiveSubstitutionFailureLevel(inheritedSubstLevel);
            IChannelScope previousChannelScope = (callStack.isEmpty()) ? null : peek().getChannelScope();
            String scope = node.getDeclaredScope();
            if (scope != null) {
                node.setStartsNewChannelScope(true);
                node.setChannelScope(new ChannelScope(node.getPathToNode(), previousChannelScope));
            } else if (previousChannelScope != null) {
                node.setStartsNewChannelScope(false);
                node.setChannelScope(previousChannelScope);
            } else {
                node.setStartsNewChannelScope(true);
                node.setChannelScope(new ChannelScope("BizViewSession", null));
            }
            ITransactionContext previousTxContext = (callStack.isEmpty()) ? null : peek().getTransactionContext();
            node.setDeclaredTransactionPropagation();
            TransactionPropagation declaredTxPropagationLevel = node.getDeclaredTransactionPropagation();
            ITransactionContext newTxContext = TransactionContext.startNewTransactionIfNeeded(declaredTxPropagationLevel, previousTxContext, node.getPathToNode());
            node.setStartsNewTransactionScope(newTxContext != previousTxContext);
            node.setTransactionContext(newTxContext);
        } catch (Exception e) {
            parkException(e);
        }
        try {
            node.setPathToErrorHandler();
        } catch (Exception e) {
            parkException(e);
            success = false;
        }
        return success;
    }

    /**
     * Sets inherited values from ancestor ScriptNodes into the ScriptNode
     * provided, and pushes it onto the call stack.
     * 
     * @param node
     *            the IScriptNode to execute next.
     */
    private void inheritValuesAndPush(IScriptNode node) {
        try {
            node.configure();
        } catch (Exception e) {
            parkException(e);
        }
        IScriptNode previousSN = null;
        XmlOutputStreamer previousOutputStreamer = null;
        if (!callStack.isEmpty()) {
            previousSN = peek();
            previousOutputStreamer = previousSN.getOutputStreamer();
        } else {
            previousOutputStreamer = rootOutputStreamer;
        }
        node.setInsidePruneElement(this.getNextInsidePruneElementSetting(node, previousSN));
        XmlOutputStreamer newOutputStreamer = this.getOutputStreamerForNode(node, previousOutputStreamer);
        node.setOutputStreamer(newOutputStreamer);
        node.setStartsNewOutputStream((newOutputStreamer != null) && (newOutputStreamer != previousOutputStreamer));
        callStack.push(node);
        if (logger.isDebugEnabled() && dumpStack) {
            logger.debug(dumpStack("push"), CLASS_NAME, "inheritValuesAndPush");
        }
    }

    /**
     * Prints out the contents of the call stack for debugging purposes.
     * @param out the PrintStream to which the output is sent.
     */
    @SuppressWarnings("unused")
    private String dumpStack(String stackOp) {
        if (!dumpStack) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("\nCall Stack after ").append(stackOp).append(":\n");
        buf.append("======================\n");
        int size = callStack.size();
        if (size == 0) {
            buf.append("Call stack is empty.\n");
        } else {
            for (int i = size - 1; i >= 0; i--) {
                IScriptNode sn = callStack.get(i);
                String fullScriptNodeClass = sn.getClass().getName();
                String scriptNodeClass = fullScriptNodeClass.substring(fullScriptNodeClass.lastIndexOf('.') + 1);
                String fullContextClass = sn.getContext().getClass().getName();
                String contextClass = fullContextClass.substring(fullContextClass.lastIndexOf('.') + 1);
                buf.append("ScriptNode #").append(i).append(" (").append(scriptNodeClass).append(" object on ").append(contextClass).append("):\n");
                buf.append(sn).append("\n");
                if (i != 0) {
                    buf.append("----------------------\n");
                }
            }
        }
        return buf.toString();
    }

    /**
     * Parks an Exception which occurred during configuration or
     * initialization until execution resumes and the Exception
     * can be handled properly.
     * @param e the Exception to be parked.
     */
    private void parkException(Exception e) {
        parkedExceptions.add(e);
    }

    /**
     * Accessor for parkedExceptions.  The live list is returned
     * so that it can be manipulated.
     * @return a reference to parkedExceptions
     */
    public List getParkedExceptions() {
        return parkedExceptions;
    }

    /**
     * Returns a boolean indicating whether a ScriptNode is
     * running inside of an xa:visible="prune" declaration.
     * The prune declaration could be on the node's Element
     * itslef, or on any ancestor Element on the CallStack.
     * This flag is used to disable outstreaming when the
     * results are subject to pruning.
     * @param node the ScriptNode for which the prune setting
     * is being determined
     * @param previousSN the previous ScriptNode from which
     * the prune setting may be inherited.
     * @return a boolean indicating whether a ScriptNode is
     * running inside of an xa:visible="prune" declaration.
     */
    private boolean getNextInsidePruneElementSetting(IScriptNode node, IScriptNode previousSN) {
        return node.isInsidePruneElement() || ((previousSN != null) && previousSN.isInsidePruneElement());
    }

    /**
     * Determines and returns the XmlOutputStreamer in effect
     * for a given ScriptNode.  The more local OutputStream
     * declarations take scope over inherited OutputStreams.
     * If a ScriptNode declares an OutputStream, then any
     * existing OutputStreamer is "parked", and the new 
     * OutputStreamer is in effect until this ScriptNode is
     * complete.  If no OutputStream has been declared (or
     * passed in from the user via the API), then the
     * result will be null.  Also, some types of Contexts
     * do not stream out, so ScriptNodes from those Contexts
     * cannot inherit an outputStreamer.
     * @param node the ScriptNode for which the effective
     * XmlOutputStreamer is being determined.
     * @param previousOutputStreamer the previous
     * XmlOutputStreamer which may be inherited.
     * @return the XmlOutputStreamer in effect for a given
     * ScriptNode, or null if it is not OutStreaming.
     */
    private XmlOutputStreamer getOutputStreamerForNode(IScriptNode node, XmlOutputStreamer previousOutputStreamer) {
        XmlOutputStreamer outputStreamer = node.getOutputStreamer();
        if ((outputStreamer == null) && (node.getContext().inheritsOutStream())) {
            outputStreamer = previousOutputStreamer;
        }
        node.setOutputStreamer(outputStreamer);
        return outputStreamer;
    }

    /**
     * Gets the SubstitutionFailureLevel to be inherited by new
     * ScriptNodes about to be added to the call stack.  This is
     * the SubstitutionFailureLevel of the ScriptNode on the top
     * of the call stack, if any; or the default SubstitutionFailureLevel
     * if the call stack is empty.
     * @return the SubstitutionFailureLevel to be inherited by new
     * ScriptNodes about to be added to the call stack.
     */
    protected SubstitutionFailureLevel getInheritedSubstitutionFailureLevel() {
        if (!callStack.isEmpty()) {
            IScriptNode topScriptNode = peek();
            return topScriptNode.getEffectiveSubstitutionFailureLevel();
        }
        return SubstitutionFailureLevel.IGNORE;
    }

    /**
     * Returns the effective SubstitutionFailureLevel for the Element
     * provided, which is the level declared on the Element, if any,
     * otherwise the level is inherited from the top node of the
     * call stack.  This method does not have the side effect of
     * removing the Attribute from the Element.
     * @param elem the Element for which the effective
     * SubstitutionFailureLevel is determined.
     * @param checkElemAncestors a boolean indicating whether the
     * search for a SubstitutionFailureLevel declaration should include
     * ancestor Elements of elem.  If false, only elem is searched.
     * @return the effective SubstitutionFailureLevel for the Element
     * provided.
     * @throws XAwareException 
     */
    public SubstitutionFailureLevel getEffectiveSubstitutionFailureLevel(Element elem, boolean checkElemAncestors) throws XAwareException {
        Attribute attr = null;
        do {
            attr = SubstitutionHelper.getSubstitutionFailureLevelAttribute(elem);
            elem = elem.getParentElement();
        } while ((attr == null) && (checkElemAncestors) && (elem != null));
        if (attr != null) {
            return SubstitutionHelper.getDeclaredSubstitutionFailureLevel(attr);
        }
        return getInheritedSubstitutionFailureLevel();
    }

    /**
     * Creates and returns a stack trace String representing the
     * current state of the call stack.
     * @return a stack trace String representing the
     * current state of the call stack.
     */
    public String getStackTrace() {
        if (callStack.isEmpty()) {
            return "Stack is empty -- execution is complete.";
        }
        Stack<String> traceStack = new Stack<String>();
        StringBuffer nodePathBuffer = new StringBuffer();
        String traceLine = null;
        IScriptNode firstScriptNode = callStack.get(0);
        String previousBizViewName = firstScriptNode.getContext().getBizViewName();
        for (ListIterator iter = callStack.listIterator(); iter.hasNext(); ) {
            IScriptNode sn = (IScriptNode) iter.next();
            String currentBizViewName = sn.getContext().getBizViewName();
            if (!currentBizViewName.equals(previousBizViewName)) {
                traceLine = formatStackTraceLine(previousBizViewName, nodePathBuffer);
                traceStack.push(traceLine);
                nodePathBuffer.setLength(0);
                previousBizViewName = currentBizViewName;
            }
            nodePathBuffer.append('/').append(sn.getName());
        }
        StringBuffer traceBuf = new StringBuffer();
        traceLine = formatStackTraceLine(previousBizViewName, nodePathBuffer);
        traceBuf.append(traceLine);
        while (!traceStack.empty()) {
            traceLine = traceStack.pop();
            traceBuf.append("\n").append(traceLine);
        }
        return traceBuf.toString();
    }

    /**
     * Formats one line of a stack trace.
     * @param bizViewName the name of the BizView used for all
     * nodes on this line of the stack trace
     * @param nodePathBuffer the buffer containing the unformatted
     * content of one stack trace line
     * @return the formatted results for one stack trace line.
     */
    private String formatStackTraceLine(String bizViewName, StringBuffer nodePathBuffer) {
        StringBuffer traceLineBuf = new StringBuffer();
        traceLineBuf.append("at ").append(nodePathBuffer.toString()).append(" (").append(bizViewName).append(")");
        return traceLineBuf.toString();
    }

    /**
     * Repeatedly pops the top node off of the call stack until either the top
     * node has an error handler (in which case the content of the top node is
     * replaced by its error handler), or the call stack is exhausted.
     * 
     * @param exception
     *            the exception to be handled
     * @throws XAwareException
     */
    public void resolveError(XAwareException exception) throws XAwareException {
        while (!callStack.isEmpty()) {
            try {
                handleStepError(exception);
                resetCurrentError();
                return;
            } catch (XAwareException e) {
                if (exception != e) {
                    exception = e;
                    updateCurrentError(exception);
                }
                continue;
            }
        }
        logger.severe("Unresolved error at end of execution" + " - default error handler will be invoked here", CLASS_NAME, "resolveError");
        throw exception;
    }

    /**
     * Adjusts the call stack in response to the error.
     * If the current node contains an error handler,
     * the current node is replaced by its error handler.
     * Otherwise, the exception is rethrown, wrapped if
     * needed as an XAwareException.
     * @param exception the exception to be handled
     * @throws XAwareException wrapping the exception if
     * the current node does not have an error handler.
     */
    public void handleStepError(Exception exception) throws XAwareException {
        if (exception instanceof XAwareEarlyTerminationException) {
            if (exception instanceof XAwareExitException) {
                handleExit((XAwareExitException) exception);
                return;
            }
            if (exception instanceof XAwareResponseException) {
                handleResponse((XAwareResponseException) exception);
                return;
            }
            finishNode(true);
            throw (XAwareEarlyTerminationException) exception;
        }
        XAwareException xawareException = updateCurrentError(exception);
        IScriptNode currentNode = finishNode(false);
        if (currentNode == null) {
            throw xawareException;
        }
        ITransactionContext parentTxContext = null;
        if (!callStack.isEmpty()) {
            parentTxContext = peek().getTransactionContext();
        }
        boolean errorHandled = currentNode.handleError(xawareException, parentTxContext);
        if (!errorHandled) {
            try {
                if (currentNode.startsNewChannelScope()) {
                    currentNode.getChannelScope().complete(false);
                }
            } catch (RuntimeException e) {
                String errMsg = "Failed to close channel: " + e;
                logger.severe(errMsg, CLASS_NAME, "handleStepError");
                throw new XAwareException(errMsg, xawareException);
            } finally {
                try {
                    if (currentNode.startsNewOutputStream()) {
                        currentNode.getOutputStreamer().closeStream();
                    }
                } catch (RuntimeException e) {
                    String errMsg = "Failed to close OutputStream: " + e;
                    logger.severe(errMsg, CLASS_NAME, "handleStepError");
                    throw new XAwareException(errMsg, xawareException);
                }
            }
            throw xawareException;
        }
        push(currentNode);
    }

    /**
     * Handles processing to be done when an XAwareExitException
     * is thrown (which results when an xa:exit element is executed).
     * @param exitException the exception being handled
     * @throws XAwareExitException if the exception needs to be
     * propagated further up the call stack before it can be handled
     * @throws XAwareException if a separate error occurred when finishing
     * the processing of nodes being popped off of the stack.
     */
    private void handleExit(XAwareExitException exitException) throws XAwareException {
        try {
            finishNode(false);
        } catch (XAwareException e) {
            throw e;
        }
        if (!callStack.isEmpty()) {
            throw exitException;
        }
        Element exitElement = exitException.getElement();
        if (exitElement != null) {
            rootContext.setScriptRoot(exitElement);
        }
    }

    /**
     * Handles processing to be done when an XAwareResponseException
     * is thrown (which results when an xa:response element is executed
     * in a BizDoc).
     * @param responseException the exception being handled
     * @throws XAwareResponseException if the exception needs to be
     * propagated further up the call stack before it can be handled
     * @throws XAwareException if a separate error occurred when finishing
     * the processing of nodes being popped off of the stack.
     */
    private void handleResponse(XAwareResponseException responseException) throws XAwareException {
        IScriptNode poppedNode;
        try {
            poppedNode = finishNode(true);
        } catch (XAwareException e) {
            throw e;
        }
        IBizViewContext poppedNodeContext = poppedNode.getContext();
        if (!callStack.isEmpty()) {
            IScriptNode previousSN = callStack.peek();
            if (poppedNodeContext.equals(previousSN.getContext())) {
                throw responseException;
            }
        }
        Element responseElement = responseException.getElement();
        if (responseElement != null) {
            poppedNodeContext.setScriptRoot(responseElement);
        }
    }

    /**
     * Clears out the state of the current Error, preserving the
     * errorMessage and stackTrace so that they are available
     * via $xavar variables later in BizDoc execution.
     */
    private void resetCurrentError() {
        if (currentError != null) {
            previousErrorMessage = currentError.getErrorMessage();
            previousStackTrace = currentError.getFullStackTrace();
            currentError = null;
        }
    }

    /**
     * Returns a reference to the Document containing the
     * results of executing the BizView.
     * @return a Document containing the results of executing the BizView.
     */
    public Document getResult() {
        return rootContext.getResult();
    }

    /**
     * Returns the Exception associated with the current error
     * @return the Exception associated with the current error
     */
    public XAwareException getCurrentException() {
        if (currentError == null) {
            return null;
        }
        return currentError.getException();
    }

    /**
     * Returns the message associated with the current error
     * @return the message associated with the current error
     */
    public String getErrorMessage() {
        if (currentError != null) {
            return currentError.getErrorMessage();
        }
        if (previousErrorMessage != null) {
            return previousErrorMessage;
        }
        return "";
    }

    /**
     * Returns the stack trace associated with the current error
     * @return the stack trace associated with the current error
     */
    public String getErrorStackTrace() {
        if (currentError != null) {
            return currentError.getFullStackTrace();
        }
        if (previousStackTrace != null) {
            return previousStackTrace;
        }
        return "";
    }

    /**
     * Updates the current error.
     * @param exception the exception which triggered the error
     * @return the exception wrapped in an XAwareException if needed.
     */
    private XAwareException updateCurrentError(Exception exception) {
        if ((currentError == null) || (!(exception instanceof XAwareException)) || (currentError.getException() != exception)) {
            XAwareException xawareException = null;
            if (exception instanceof XAwareUserException) {
                xawareException = (XAwareUserException) exception;
            } else {
                IBizViewContext context = null;
                if (!callStack.isEmpty()) {
                    context = peek().getContext();
                } else {
                    context = rootContext;
                }
                String enhancedErrMsg = context.getErrorMessagePrefix() + " ";
                if (exception.getMessage() != null) {
                    enhancedErrMsg += exception.getMessage();
                } else {
                    enhancedErrMsg += ExceptionMessageHelper.getExceptionMessage(exception);
                }
                xawareException = new XAwareProcessingException(enhancedErrMsg, exception);
            }
            currentError = new ScriptProcessingError(xawareException, getStackTrace(), currentError);
            return xawareException;
        }
        return (XAwareException) exception;
    }

    /**
     * Returns the currently logged in userid, or null if no
     * user is logged in.
     * 
     * TODO: This is a stub declaration for future implementation.
     * 
     * @return the currently logged in userid
     */
    public String getLoginIdentity() {
        return null;
    }

    /**
     * Returns a path to the current executing node.
     * 
     * TODO: This is a stub declaration for future implementation.
     * 
     * @return a String representation of the path to
     * the current executing node.
     */
    public String getPathToCurrentNode() {
        return null;
    }

    /**
     * Returns the state object containing a representation
     * of the current state of execution.
     * 
     * TODO: This is a stub declaration for future implementation.
     * 
     * @return the state object containing a representation
     * of the current state of execution.
     */
    public IScriptProcessState getScriptProcessState() {
        return null;
    }

    /**
     * Get statistics about a script's processing.
     * 
     * TODO: This is a stub declaration for future implementation.
     * 
     * @return an IScriptStatistics object containing
     * statistics about a script's processing.
     */
    public IScriptStatistics getScriptStatistics() {
        return null;
    }

    /**
     * Set a flag indicating that we should abort.
     * 
     * TODO: This is a stub declaration for future implementation.
     * 
     * @param abort
     */
    public void setAbort(boolean abort) {
        this.abort = abort;
        Element abortElem = new Element("abort");
        this.parkException(new XAwareExitException("Execution aborted by setAbort()", abortElem));
    }

    /**
     * Return the flag indicating whether or not to abort.
     * 
     * TODO: This is a stub declaration for future implementation.
     * 
     * @return boolean
     */
    public boolean shouldAbort() {
        return abort;
    }

    /**
     * This mutator is used to set the value of one system variable in the 
     * systemVariables Map.  The system variable can be stored using the full 
     * 'xavar:name' or just the 'name' string.  The names are stored using 
     * just the name string in the Map.
     * @param name - The name string of the system variable.  The name
     * does not have the xavar prefix.
     * @param value - The value associated with the system variable
     */
    public synchronized void setSystemVariable(String name, Object value) {
        String localName = name;
        if (localName.startsWith(SESSION_VARIABLE_PREFIX)) localName = localName.substring(SESSION_VARIABLE_PREFIX.length());
        if (value == null) systemVariables.remove(localName); else systemVariables.put(localName, value);
    }

    /**
     * Accessor to a registered system variable stored in the 
     * systemVariables Map.
     * @param name - The name of the system variable whose associated value
     * is requested.  The system variable can be requested using the full 
     * 'xavar:name' or just the 'name' string.  The names are stored using 
     * just the name string in the Map. 
     * @return Returns the value associated with the system variable or
     * null if it can not be found.
     */
    public synchronized Object getSystemVariable(String name) {
        String localName = name;
        if (localName.startsWith(SESSION_VARIABLE_PREFIX)) localName = localName.substring(SESSION_VARIABLE_PREFIX.length());
        return systemVariables.get(localName);
    }

    /**
     * Gets an unmodifiable List view of the call stack.
     * @return an unmodifiable List view of the call stack.
     */
    public List<IScriptNode> getCallStack() {
        return Collections.unmodifiableList(callStack);
    }

    public String getInputXmlQualifiedRootName() {
        Element rootElem = rootContext.getInputXmlRoot();
        if (rootElem == null) {
            return null;
        }
        return rootElem.getQualifiedName();
    }

    /**
     * @return the dumpStack
     */
    public boolean isDumpStack() {
        return dumpStack;
    }

    /**
     * @param dumpStack the dumpStack to set
     */
    public void setDumpStack(boolean dumpStack) {
        this.dumpStack = dumpStack;
    }
}
