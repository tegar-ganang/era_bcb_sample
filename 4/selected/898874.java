package org.xaware.server.engine.instruction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IBizViewSession;
import org.xaware.server.engine.IChannelScope;
import org.xaware.server.engine.IInstruction;
import org.xaware.server.engine.IInstructionParser;
import org.xaware.server.engine.IResourceManager;
import org.xaware.server.engine.IScriptNode;
import org.xaware.server.engine.ISessionStateRegistry;
import org.xaware.server.engine.ITransactionContext;
import org.xaware.server.engine.context.SubstitutionHelper;
import org.xaware.server.engine.controller.transaction.TransactionContext;
import org.xaware.server.engine.controller.transaction.XAwareTransactionException;
import org.xaware.server.engine.enums.SubstitutionFailureLevel;
import org.xaware.server.engine.enums.TransactionPropagation;
import org.xaware.server.engine.exceptions.XAwareSpecialHandlerException;
import org.xaware.server.resources.XAwareBeanFactory;
import org.xaware.server.streaming.XmlOutputStreamer;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareInvalidEnumerationValueException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class forms the base for ScriptNode and SecondPassScriptNode It represents one node (or Element) of the XML
 * program being interpreted
 * 
 * @author hcurtis
 * 
 */
public abstract class BaseScriptNode implements IScriptNode {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(BaseScriptNode.class.getName());

    private static final String CLASS_NAME = "BaseScriptNode";

    private final boolean loggerIsFinestEnabled;

    protected Element element;

    protected IBizViewContext context;

    protected IInstructionParser ip;

    protected List<IInstruction> instructionList = new ArrayList<IInstruction>();

    protected List<IInstruction> postInstructionList = new ArrayList<IInstruction>();

    private boolean completed = false;

    private TransactionPropagation declaredTransactionPropagation = null;

    private boolean startsNewTransactionScope;

    private ITransactionContext transactionContext;

    private ITransactionContext implicitTransactionContext = null;

    private boolean startsNewChannelScope;

    private IChannelScope channelScope;

    private SubstitutionFailureLevel declaredSubstitutionFailureLevel = null;

    private SubstitutionFailureLevel effectiveSubstitutionFailureLevel = null;

    private String pathToErrorHandler = null;

    private boolean configured = false;

    private boolean initialized = false;

    private XmlOutputStreamer outputStreamer = null;

    private boolean startsNewOutputStream;

    private boolean isInsidePruneElement = false;

    protected boolean isElementVisibleForStreamingOut = true;

    private final Map<String, Object> streamRegistry = new HashMap<String, Object>();

    protected boolean skipStartingTag = false;

    /** 
     * Used for special processing in output streaming cases where the error handler contents
     * has to be seen in both the BizDoc results and in the output stream
     */
    protected boolean isStreamingOutErrorHandling = false;

    private boolean streamedOutStartTag = false;

    private List<Content> processedContent = new ArrayList<Content>();

    /**
     * Default constructor. Required for spring bean construction. Must be followed by a call to setup().
     */
    public BaseScriptNode() {
        loggerIsFinestEnabled = lf.isFinestEnabled();
    }

    /**
     * When using the Spring Bean factory the following attributes are set in the ScriptNode by the InstructionParser
     * 
     * @param aContext -
     *            BizViewContext for the ScriptNode
     * @param anElement -
     *            Element of focus for the ScriptNode
     * @param anIP -
     *            Instruction Parser
     */
    public void setup(final IBizViewContext aContext, final Element anElement, final IInstructionParser anIP) {
        this.element = anElement;
        this.context = aContext;
        this.ip = anIP;
    }

    /**
     * Returns a boolean indicating whether the configure() method has been called on this ScriptNode.
     * 
     * @return a boolean indicating whether this ScriptNode has been configured.
     */
    public final boolean hasBeenConfigured() {
        return configured;
    }

    /**
     * Performs configuration for the ScriptNode.
     * 
     * @throws IllegalStateException
     *             if this ScriptNode has already been configured.
     * @throws XAwareException
     *             if a processing error occurs during configuration.
     */
    public final void configure() throws XAwareException {
        if (configured) {
            return;
        }
        element.setAttribute(XAwareConstants.ON_SUBSTITUTE_FAILURE_ATTR, effectiveSubstitutionFailureLevel.getValue(), XAwareConstants.xaNamespace);
        ip.populateInstructions(this);
        for (final Iterator iter = instructionList.iterator(); iter.hasNext(); ) {
            final IInstruction instr = (IInstruction) iter.next();
            instr.configure();
            if (instr.isNotVisibleForStreamingOut()) {
                isElementVisibleForStreamingOut = false;
            }
            if (instr.hasBeenCompleted()) {
                iter.remove();
            }
        }
        for (final Iterator iter = postInstructionList.iterator(); iter.hasNext(); ) {
            final IInstruction instr = (IInstruction) iter.next();
            instr.configure();
            if (instr.isNotVisibleForStreamingOut()) {
                isElementVisibleForStreamingOut = false;
            }
            if (instr.hasBeenCompleted()) {
                iter.remove();
            }
        }
        element.removeAttribute(XAwareConstants.ON_SUBSTITUTE_FAILURE_ATTR, XAwareConstants.xaNamespace);
        configured = true;
    }

    /**
     * Returns a boolean indicating whether the initialize() method has been called on this ScriptNode.
     * 
     * @return a boolean indicating whether this ScriptNode has been initialized.
     */
    public boolean hasBeenInitialized() {
        return initialized;
    }

    /**
     * Performs initialization for the ScriptNode.
     * 
     * @throws IllegalStateException
     *             if this ScriptNode has already been initialized.
     * @throws XAwareException
     *             if a processing error occurs during initialization.
     */
    public final void initialize() throws XAwareException {
        if (initialized) {
            throw new IllegalStateException("ScriptNode has already been initialized.");
        }
        init();
        initialized = true;
    }

    /**
     * This method performs initialization specific to each type of ScriptNode.
     * 
     * @throws XAwareException
     */
    protected abstract void init() throws XAwareException;

    /**
     * Returns a boolean indicating whether the getNextInstruction() method needs to be called again on this ScriptNode.
     * This method acts together with the getNextInstruction() method to provide an Iterator-like interface for
     * executing an unknown number of Instructions within the ScriptNode.
     * 
     * @return a boolean indicating whether the getNextInstruction() method needs to be called again on this ScriptNode.
     */
    public boolean hasMoreInstructions() {
        if (instructionList.size() > 0) {
            final IInstruction instr = instructionList.get(0);
            if (instr.hasBeenCompleted()) {
                instructionList.remove(instr);
            }
        }
        return instructionList.size() != 0;
    }

    /**
     * Returns an IInstruction reference to the next non-post Instruction to be executed. Will return a reference to the
     * same Instruction returned on the previous call if that Instruction has not been completed. This method acts
     * together with the hasMoreInstructions() method to provide an Iterator-like interface for executing an unknown
     * number of Instructions within the ScriptNode.
     * 
     * @return an IInstruction reference to the next non-post Instruction to be executed.
     */
    public IInstruction getNextInstruction() {
        final IInstruction instr = instructionList.get(0);
        if (loggerIsFinestEnabled) {
            lf.finest("Instruction is: " + instr.toString(), CLASS_NAME, "getNextInstruction");
        }
        return instr;
    }

    /**
     * Returns a boolean indicating whether the getNextPostInstruction() method needs to be called again on this
     * ScriptNode. This method acts together with the getNextPostInstruction() method to provide an Iterator-like
     * interface for executing an unknown number of post Instructions within the ScriptNode.
     * 
     * @return a boolean indicating whether the getNextPostInstruction() method needs to be called again on this
     *         ScriptNode.
     */
    public boolean hasMorePostInstructions() {
        if (postInstructionList.size() > 0) {
            final IInstruction instr = postInstructionList.get(0);
            if (instr.hasBeenCompleted()) {
                postInstructionList.remove(instr);
            }
        }
        return postInstructionList.size() != 0;
    }

    /**
     * Returns an IInstruction reference to the next post Instruction to be executed. Will return a reference to the
     * same Instruction returned on the previous call if that Instruction has not been completed. This method acts
     * together with the hasMorePostInstructions() method to provide an Iterator-like interface for executing an unknown
     * number of post Instructions within the ScriptNode.
     * 
     * @return an IInstruction reference to the next post Instruction to be executed.
     */
    public IInstruction getNextPostInstruction() {
        final IInstruction instr = postInstructionList.get(0);
        if (loggerIsFinestEnabled) {
            lf.finest("Post Instruction is: " + instr.toString(), CLASS_NAME, "getNextPostInstruction");
        }
        return instr;
    }

    /**
     * Returns the current instruction, or null if none remain.
     * 
     * @return the current instruction, or null if none remain.
     */
    protected IInstruction getCurrentInstruction() {
        if (instructionList.size() > 0) {
            return instructionList.get(0);
        }
        if (postInstructionList.size() > 0) {
            return postInstructionList.get(0);
        }
        return null;
    }

    /**
     * Completes (finishes) processing for the ScriptNode.
     * 
     * @param success
     *            a boolean indicating whether execution of the node was successful. A value of "false" indicates that
     *            this method was invoked because processing of this ScriptNode was abnormally terminated due to an
     *            error.
     * @throws XAwareException
     *             if this ScriptNode has already been completed, or if any error occurs during completion processing,
     *             such as releasing of resources.
     */
    public final void complete(final boolean success) throws XAwareException {
        if (completed) {
            throw new IllegalStateException("ScriptNode has already been completed.");
        }
        if (success) {
            finish();
        } else {
            finishOnError();
        }
        if (this.implicitTransactionContext != null) {
            throw new XAwareTransactionException("implicitTransactionContext left open");
        }
        completed = true;
    }

    /**
     * This method performs completion processing specific to each type of ScriptNode.
     */
    protected abstract void finish() throws XAwareException;

    /**
     * This method performs completion processing when processing completed due to abnormal termination.
     */
    protected void finishOnError() throws XAwareException {
        final IInstruction currentInstr = getCurrentInstruction();
        if ((currentInstr != null) && currentInstr.hasBeenInitialized()) {
            currentInstr.finishOnError();
        }
        if (isStreamingOut() && !isStreamingOutErrorHandling) {
            if (isElementVisibleForStreamingOut && getPathToErrorHandler() == null && streamedOutStartTag) {
                getOutputStreamer().streamOutEndTag(element);
            }
        }
    }

    /**
     * Accessor to the Element that the ScriptNode is operating on
     * 
     * @return JDOM Element that the ScriptNode is focused on
     */
    public final Element getElement() {
        return element;
    }

    /**
     * Returns the BizViewContext associated with this ScriptNode
     * 
     * @return the BizViewContext associated with this ScriptNode
     */
    public final IBizViewContext getContext() {
        return context;
    }

    /**
     * Returns the SessionStateRegistry associated with this ScriptNode
     * 
     * @return the SessionStateRegistry associated with this ScriptNode
     */
    public final ISessionStateRegistry getRegistry() {
        return context.getRegistry();
    }

    /**
     * Returns the BizViewSession associated with this ScriptNode
     * 
     * @return the BizViewSession associated with this ScriptNode
     */
    public final IBizViewSession getBizViewSession() {
        return getRegistry().getBizViewSession();
    }

    /**
     * Add an instruction to the list of instructions that will be executed in the initial execution phase of the
     * instruction
     * 
     * @param instruction
     */
    public final void addInstruction(final IInstruction instruction) {
        instructionList.add(instruction);
    }

    /**
     * Add an instruction to the list of post instructions that will be executed after all of the initial execution
     * phase instructions and the child elements have been executed
     * 
     * @param instruction
     */
    public final void addPostInstruction(final IInstruction instruction) {
        postInstructionList.add(instruction);
    }

    /**
     * Get an immutable list of the instructions in the ScriptNode.
     * 
     * @return list of instructions
     */
    final List<IInstruction> getInstructionList() {
        return Collections.unmodifiableList(instructionList);
    }

    /**
     * Get an immutable list of the post instructions in the ScriptNode
     * 
     * @return list of post instructions
     */
    final List<IInstruction> getPostInstructionList() {
        return Collections.unmodifiableList(postInstructionList);
    }

    /**
     * Returns the value of the xa:transaction tag associated with this ScriptNode, or null if there is no such tag.
     * 
     * @return a TransactionPropagation enum value representing the xa:transaction tag associated with this ScriptNode.
     */
    public final TransactionPropagation getDeclaredTransactionPropagation() {
        return declaredTransactionPropagation;
    }

    /**
     * Finds the xa:transaction tag associated with this ScriptNode, removes the attribute, performs substitution on the
     * value, and stores it in a field. Stores null if there if no xa:transaction tag associated with this ScriptNode.
     * 
     * @throws XAwareException
     *             if the attribute is present but its value does not match any legal value.
     * @throws XAwareSubstitutionException
     *             if the attribute is present but an error occurs substituting its value.
     */
    public final void setDeclaredTransactionPropagation() throws XAwareException {
        final Attribute attr = element.getAttribute(XAwareConstants.BIZDOCUMENT_ATTR_TRANSACTION, XAwareConstants.xaNamespace);
        if (attr != null) {
            element.removeAttribute(attr);
            final String attrValue = attr.getValue();
            final String substAttrValue = context.substitute(attrValue, element, null, effectiveSubstitutionFailureLevel);
            try {
                declaredTransactionPropagation = TransactionPropagation.getTransactionPropagation(substAttrValue);
            } catch (final XAwareInvalidEnumerationValueException e) {
                throw new XAwareException("Invalid value for xa:transaction attribute: \"" + attrValue + "\"", e);
            }
            element.removeAttribute(attr);
        }
    }

    /**
     * Returns a boolean indicating whether this ScriptNode is responsible for starting a new transaction scope.
     * 
     * @return startsNewTransactionScope.
     */
    public final boolean startsNewTransactionScope() {
        return this.startsNewTransactionScope;
    }

    /**
     * Sets a boolean indicating whether this ScriptNode is responsible for starting a new transaction scope.
     * 
     * @param startsNewTransactionScope
     *            The new startsNewTransactionScope value to set.
     */
    public final void setStartsNewTransactionScope(final boolean startsNewTransactionScope) {
        this.startsNewTransactionScope = startsNewTransactionScope;
    }

    /**
     * Returns a reference to the TransactionContext in which this ScriptNode is executing.
     * 
     * @return Returns the transactionContext.
     */
    public final ITransactionContext getTransactionContext() {
        return (this.implicitTransactionContext != null) ? this.implicitTransactionContext : this.transactionContext;
    }

    /**
     * Sets the TransactionContext in which this ScriptNode is executing.
     * 
     * @param transactionContext
     *            The transactionContext to set.
     */
    public final void setTransactionContext(final ITransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    /**
     * Begins an Implicit transaction wrapping an Instruction if there is not
     * already an active Transaction
     * 
     * @param transactionName
     *            the name of the transaction to be created.
     * @return whether a new Transaction was started.
     * @throws XAwareTransactionException
     *             if a system error occurred starting the transaction.
     */
    public boolean beginImplicitTransactionIfNeeded(final String transactionName) throws XAwareTransactionException {
        if (this.transactionContext == null) {
            throw new XAwareTransactionException("TransactionContext is null");
        }
        if (this.implicitTransactionContext != null) {
            throw new XAwareTransactionException("implicitTransactionContext already in progress");
        }
        ITransactionContext newTxContext = TransactionContext.startNewTransactionIfNeeded(TransactionPropagation.REQUIRED, this.transactionContext, transactionName);
        if (newTxContext == this.transactionContext) {
            return false;
        }
        this.implicitTransactionContext = newTxContext;
        return true;
    }

    /**
     * Completes an Implicit transaction wrapping an Instruction, either by
     * committing or rolling back the transaction.
     * 
     * @param success
     *            a boolean indicating whether the transaction processing was
     *            successful. True indicates commit; false indicates rollback.
     * @throws XAwareTransactionException
     *             if any error occurs completing the transaction
     */
    public void completeImplicitTransaction(final boolean success) throws XAwareTransactionException {
        try {
            if (this.implicitTransactionContext != null) {
                implicitTransactionContext.complete(success, false);
            }
        } finally {
            implicitTransactionContext = null;
        }
    }

    /**
     * Returns the value of the xa:scope tag associated with this ScriptNode,
     * or null if there is no such tag.
     * 
     * @return the String value of the xa:scope tag associated with
     *         this ScriptNode.
     * @throws XAwareException
     *             if the value is present but invalid.
     */
    public String getDeclaredScope() throws XAwareException {
        String attrValue = element.getAttributeValue(XAwareConstants.XAWARE_ATTR_SCOPE, XAwareConstants.xaNamespace);
        if ((attrValue != null) && (!attrValue.equals("new"))) {
            throw new XAwareException("Invalid value for xa:scope: " + attrValue);
        }
        return attrValue;
    }

    /**
     * Returns a boolean indicating whether this ScriptNode is responsible for starting a new channel scope.
     *
     * @return startsNewChannelScope.
     */
    public boolean startsNewChannelScope() {
        return this.startsNewChannelScope;
    }

    /**
     * Sets a boolean indicating whether this ScriptNode is responsible for starting a new channel scope.
     *
     * @param startsNewChannelScope
     *            The new startsNewChannelScope value to set.
     */
    public void setStartsNewChannelScope(boolean startsNewChannelScope) {
        this.startsNewChannelScope = startsNewChannelScope;
    }

    /**
     * Returns a reference to the ChannelScope in which this ScriptNode is executing.
     *
     * @return Returns the ChannelScope.
     */
    public IChannelScope getChannelScope() {
        return channelScope;
    }

    /**
     * Sets the ChannelScope in which this ScriptNode is executing.
     *
     * @param channelScope
     *            The ChannelScope to set.
     */
    public void setChannelScope(IChannelScope channelScope) {
        this.channelScope = channelScope;
    }

    /**
     * Returns the value of the xa:on_substitute_failure tag associated with this ScriptNode, or null if there is no
     * such tag.
     * 
     * @return a SubstitutionFailureLevel object representing the xa:on_substitute_failure tag associated with this
     *         ScriptNode
     */
    public final SubstitutionFailureLevel getDeclaredSubstitutionFailureLevel() {
        return declaredSubstitutionFailureLevel;
    }

    /**
     * Finds the xa:on_substitute_failure tag associated with this ScriptNode, removes the attribute, performs
     * substitution on the value (using the inheritedSubstitutionFailureLevel in case of failures), and stores it in a
     * field. Stores null if there if no xa:on_substitute_failure tag associated with this ScriptNode.
     * 
     * @param inheritedSubstitutionFailureLevel
     *            the SubstitutionFailureLevel to use when performing substitution on the xa:on_substitute_failure
     *            attribute's value.
     * @throws XAwareException
     *             if the attribute is present but its value does not match any legal value.
     * @throws XAwareSubstitutionException
     *             if the attribute is present but an error occurs substituting its value.
     */
    private final void setDeclaredSubstitutionFailureLevel(final SubstitutionFailureLevel inheritedSubstitutionFailureLevel) throws XAwareException {
        final Attribute substFailureAttr = SubstitutionHelper.getSubstitutionFailureLevelAttribute(element);
        if (substFailureAttr != null) {
            final String substValue = context.substitute(substFailureAttr.getValue(), element, null, inheritedSubstitutionFailureLevel);
            substFailureAttr.setValue(substValue);
            declaredSubstitutionFailureLevel = SubstitutionHelper.getDeclaredSubstitutionFailureLevel(substFailureAttr);
        }
    }

    /**
     * Returns the effective SubstitutionFailureLevel for this ScriptNode. It may be declared directly on the Element,
     * or inherited from another Element deeper in the call stack.
     * 
     * @return the effective SubstitutionFailureLevel for this ScriptNode.
     */
    public final SubstitutionFailureLevel getEffectiveSubstitutionFailureLevel() {
        return effectiveSubstitutionFailureLevel;
    }

    /**
     * Sets the effective SubstitutionFailureLevel for this ScriptNode. It may be declared directly on the Element, or
     * inherited from another Element deeper in the call stack.
     * 
     * @param inheritedSubstitutionFailureLevel
     *            the inherited SubstitutionFailureLevel for this ScriptNode.
     * @throws XAwareException
     *             if the attribute is present but its value does not match any legal value.
     * @throws XAwareSubstitutionException
     *             if the attribute is present but an error occurs substituting its value.
     */
    public final void setEffectiveSubstitutionFailureLevel(final SubstitutionFailureLevel inheritedSubstitutionFailureLevel) throws XAwareException {
        effectiveSubstitutionFailureLevel = inheritedSubstitutionFailureLevel;
        setDeclaredSubstitutionFailureLevel(inheritedSubstitutionFailureLevel);
        if (declaredSubstitutionFailureLevel != null) {
            effectiveSubstitutionFailureLevel = declaredSubstitutionFailureLevel;
        }
    }

    /**
     * Gets a combined List of all (non-post and post) instructions for this ScriptNode.
     * 
     * @return a read-only List of all Instructions for this ScriptNode.
     */
    protected List<IInstruction> getAllInstructions() {
        final List<IInstruction> allInstructions = new ArrayList<IInstruction>(instructionList);
        allInstructions.addAll(postInstructionList);
        return Collections.unmodifiableList(allInstructions);
    }

    /**
     * Handles an error by applying the error handler declared on the
     * ScriptNode, if any.
     * <p>
     * If a error handler declaration is present, then the contents of this
     * ScriptNode's Element are replaced by a clone of the error handler
     * element. Additional changes are also made to replacementForThis
     * ScriptNode to prepare it to resume processing to execute the error
     * handler.
     * 
     * @param xawareException
     *            the exception which triggered the error handling, wrapped in
     *            an XAwareException if not originally of that type.
     * @param parentTxContext
     *            the TransactionContext in effect for the parent ScriptNode of
     *            this ScriptNode
     * @return a boolean indicating whether an error handler was applied.
     * @throws XAwareException
     *             if an error handler attribute exists on this Element, but the
     *             error handler Element cannot be found at that path.
     */
    public boolean handleError(final XAwareException xawareException, ITransactionContext parentTxContext) throws XAwareException {
        final Element errorHandlerElement = getErrorHandlerElement(xawareException);
        if (errorHandlerElement == null) {
            return false;
        }
        if (isStreamingOut()) {
            isStreamingOutErrorHandling = true;
        }
        lf.info("Applying error handler from " + errorHandlerElement + " to " + element, CLASS_NAME, "handleError");
        this.reset();
        this.configured = true;
        context.replaceContentsWithErrorHandler(getElement(), errorHandlerElement);
        this.pathToErrorHandler = null;
        if (this.startsNewTransactionScope()) {
            if (parentTxContext != null) {
                this.setStartsNewTransactionScope(false);
                this.setTransactionContext(parentTxContext);
            } else {
                this.setTransactionContext(TransactionContext.startNewTransactionIfNeeded(null, null, this.getPathToNode()));
            }
        }
        if (this.startsNewOutputStream()) {
            getOutputStreamer().reopenAppend(this);
        }
        declaredSubstitutionFailureLevel = null;
        getElement().removeAttribute(XAwareConstants.ON_SUBSTITUTE_FAILURE_ATTR, XAwareConstants.xaNamespace);
        effectiveSubstitutionFailureLevel = getRegistry().getEffectiveSubstitutionFailureLevel(getElement(), false);
        removeInstructions();
        IInstructionParser secondPassIP = XAwareBeanFactory.getResourceManager().getInstructionParser(IResourceManager.BIZ_DOC_SECOND_PASS_INST_PARSER);
        secondPassIP.populateInstructions(this);
        removeInstructions();
        return true;
    }

    /**
     * Reset state to resume processing after errors
     */
    protected void reset() {
        this.initialized = false;
        this.completed = false;
        this.skipStartingTag = true;
    }

    /**
     * 
     */
    private void removeInstructions() {
        for (IInstruction instr : instructionList) {
            instr.cleanupAttributes();
        }
        instructionList.clear();
        for (IInstruction instr : postInstructionList) {
            instr.cleanupAttributes();
        }
        postInstructionList.clear();
    }

    /**
     * Register the Input or Output stream object in the registry
     * 
     * @param stream -
     *            The streaming resource
     * @param name -
     *            The string name to be associated with the resource. Usually this would be a full path name to a file
     *            resource
     * @throws XAwareException
     */
    public void registerStreamResource(final String name, final Object stream) throws XAwareException {
        if (streamRegistry.containsKey(name)) {
            throw new XAwareException("Stream " + name + " already registered");
        } else {
            streamRegistry.put(name, stream);
        }
    }

    /**
     * Release the resource from the registration. This method does not close the stream. Instead, it simply removes it
     * from the registration.
     * 
     * @param name -
     *            The string name to be associated with the resource.
     * @return Returns true if the resouce was found and released from the registry.
     */
    public boolean releaseStreamResource(final String name) {
        final Object stream = streamRegistry.remove(name);
        if (stream == null) {
            return false;
        }
        return getBizViewSession().removeStreamResource(stream);
    }

    /**
     * Acquire from the Resource Manager an InputStream resource for the supplied file path. Also, register the stream
     * with the script node so when the script node's finish is called the InputStream will be closed if it isn't
     * already.
     * 
     * @param name -
     *            Name (path) of the stream file
     * @return Returns the InputStream object for the supplied source name
     */
    public InputStream getInputStreamResource(final String name) throws XAwareException {
        final InputStream stream = getBizViewSession().getInputStream(name);
        if (stream != null) {
            registerStreamResource(name, stream);
        }
        return stream;
    }

    /**
     * Acquire from the Resource Manager an OutputStream resource for the supplied file path. Also, register the stream
     * with the script node so when the script node's finish is called the OutputStream will be closed if it isn't
     * already.
     * 
     * @param name -
     *            Name (path) of the stream file
     * @param append -
     *            Set to true if the output stream should append to an existing file. If false and there is an existing
     *            file, it will be replaced by the requested file
     * @return Returns the OuputStream object for the supplied source name
     */
    public OutputStream getOutputStreamResource(final String name, final boolean append) throws XAwareException {
        final OutputStream stream = getBizViewSession().getOutputStream(name, append);
        if (stream != null) {
            registerStreamResource(name, stream);
        }
        return stream;
    }

    /**
     * Close the stream registered with the Resource Manager
     * 
     * @param name -
     *            Name of the stream file
     * @return Returns true if the file was closed, FALSE if the file was not found or was already closed
     * @throws XAwareException
     *             when an illegal file name is supplied
     */
    public boolean closeStreamResource(final String name) throws XAwareException {
        if (name == null) {
            throw new XAwareException("Requested file close supplied null file name");
        }
        if (name.length() == 0) {
            throw new XAwareException("File close request with empty file name");
        }
        final Object stream = streamRegistry.get(name);
        if (stream == null) {
            return false;
        }
        return getBizViewSession().closeStream(stream);
    }

    /**
     * Release any registered resources that are found in the streamRegistry Map. This method will handle the closing of
     * InputStream and OutputStream resources that were requested through the BizViewSession.
     */
    public void cleanUpStreamResources() {
        final Iterator iterMap = streamRegistry.entrySet().iterator();
        while (iterMap.hasNext()) {
            final Map.Entry entry = (Map.Entry) iterMap.next();
            final Object obj = entry.getValue();
            if (obj != null) {
                getBizViewSession().closeStream(obj);
            }
        }
        streamRegistry.clear();
    }

    /**
     * Returns the effective path to the error handler element, based on precedence rules. Can be null if no error
     * handler attribute is declared on this element.
     * 
     * @return the String value of the pathToErrorHandler declared on the element, or null if none exists.
     */
    public String getPathToErrorHandler() {
        return pathToErrorHandler;
    }

    /**
     * Finds the Error Handler Attributes on the element, if any, then removes the Attributes from the Element.
     * Determines the value for the pathToErrorHandler (on_error has precedence over on_err which has precedence
     * over on_doc_err) and caches it for subsequent calls.
     * 
     * @throws XAwareSubstitutionException
     *             if it finds an error handler attribute but a substitution error occurs while substituting the value
     *             for that attribute.
     * @throws XAwareException Exception thrown during processing of a functoid.
     */
    public void setPathToErrorHandler() throws XAwareSubstitutionException, XAwareException {
        final Attribute onErrorAttr = getOnErrorAttribute(element);
        final Attribute onErrAttr = getOnErrAttribute(element);
        final Attribute onDocErrAttr = getOnDocErrAttribute(element);
        if (onErrorAttr != null) {
            element.removeAttribute(onErrorAttr);
        }
        if (onErrAttr != null) {
            element.removeAttribute(onErrAttr);
        }
        if (onDocErrAttr != null) {
            element.removeAttribute(onDocErrAttr);
        }
        if (onErrorAttr != null) {
            pathToErrorHandler = context.substitute(onErrorAttr.getValue(), element, null, effectiveSubstitutionFailureLevel);
        } else if (onErrAttr != null) {
            pathToErrorHandler = context.substitute(onErrAttr.getValue(), element, null, effectiveSubstitutionFailureLevel);
        } else if (onDocErrAttr != null) {
            pathToErrorHandler = context.substitute(onDocErrAttr.getValue(), element, null, effectiveSubstitutionFailureLevel);
        }
    }

    /**
     * Finds and returns the xa:on_error Attribute attached to the Element provided.
     * 
     * @param elem
     *            the Element on which to find the xa:on_error Attribute
     * @return the the xa:on_error Attribute attached to the Element provided, if any or null otherwise.
     */
    public static Attribute getOnErrorAttribute(final Element elem) {
        return elem.getAttribute(XAwareConstants.BIZDOCUMENT_ATTR_ON_ERROR, XAwareConstants.xaNamespace);
    }

    /**
     * Finds and returns the xa:on_err Attribute attached to the Element provided.
     * 
     * @param elem
     *            the Element on which to find the xa:on_err Attribute
     * @return the the xa:on_err Attribute attached to the Element provided, if any or null otherwise.
     */
    public static Attribute getOnErrAttribute(final Element elem) {
        return elem.getAttribute(XAwareConstants.BIZDOCUMENT_ATTR_ON_ERR, XAwareConstants.xaNamespace);
    }

    /**
     * Finds and returns the xa:on_doc_err Attribute attached to the Element provided.
     * 
     * @param elem
     *            the Element on which to find the xa:on_doc_err Attribute
     * @return the the xa:on_doc_err Attribute attached to the Element provided, if any or null otherwise.
     */
    public static Attribute getOnDocErrAttribute(final Element elem) {
        return elem.getAttribute(XAwareConstants.BIZDOCUMENT_ATTR_ON_BIZDOC_ERR, XAwareConstants.xaNamespace);
    }

    /**
     * Finds the error handler Element for this ScriptNode. The path to this Element is the contained in the xa:on_error
     * xa:on_err or xa:on_doc_err attribute.
     * 
     * @return an Element reference to the error handler Element for this ScriptNode, or null if none is found.
     */
    private Element getErrorHandlerElement(final XAwareException xawareException) throws XAwareException {
        Element errorHandlerElement = null;
        final Throwable wrappedException = xawareException.getCause();
        String errorHandlerPath = null;
        if (wrappedException instanceof XAwareSpecialHandlerException) {
            errorHandlerPath = ((XAwareSpecialHandlerException) wrappedException).getErrorHandlerPath();
        } else {
            errorHandlerPath = getPathToErrorHandler();
        }
        if (errorHandlerPath != null) {
            String failureCause = null;
            try {
                errorHandlerElement = context.getElementAtPath(errorHandlerPath, element, null);
            } catch (final XAwareException e) {
                failureCause = e.getMessage();
            }
            if (errorHandlerElement == null) {
                if (failureCause == null) {
                    failureCause = "Element at path '" + errorHandlerPath + "' not found.";
                }
                final String errMsg = "Invalid path to error handler: '" + errorHandlerPath + "' found on node " + getPathToNode() + " in " + getContext().getBizViewName() + ", Cause: " + failureCause;
                lf.severe(errMsg, CLASS_NAME, "getErrorHandlerElement");
                throw new XAwareException(errMsg, xawareException);
            }
        }
        return errorHandlerElement;
    }

    /**
     * Returns a boolean indicating whether this ScriptNode is responsible for starting a new output stream.
     * 
     * @return Returns startsNewOutputStream.
     */
    public final boolean startsNewOutputStream() {
        return this.startsNewOutputStream;
    }

    /**
     * Sets a boolean indicating whether this ScriptNode is responsible for starting a new output stream.
     * 
     * @param startsNewOutputStream
     *            The startsNewOutputStream value to set.
     */
    public final void setStartsNewOutputStream(final boolean startsNewOutputStream) {
        this.startsNewOutputStream = startsNewOutputStream;
    }

    /**
     * Returns a boolean indicating whether the processing for this ScriptNode is being performed with streaming out in
     * effect. I.e., it indicates that either the associated BizView directly declares outstreaming, or that it inherits
     * outstreaming from other BizViews which call it through a reference instruction. Even if this flag is set, this
     * does not necessarily indicate that the results of processing this ScriptNode should be streamed out. For
     * instance, outstreaming may be suppressed if this Element declares xa:visible="prune" or this Element is nested
     * within another Element which declares xa:visible="prune".
     * 
     * @return true if streaming out is in effect during processing of this ScriptNode.
     */
    public boolean isStreamingOut() {
        return (outputStreamer != null);
    }

    /**
     * Returns the OutputStreamer which encapsulates the output stream and formatting used when outstreaming.
     * 
     * @return Returns the OutputStreamer, or null if outstreaming is not in effect.
     */
    public XmlOutputStreamer getOutputStreamer() {
        return this.outputStreamer;
    }

    /**
     * Setter for the OutputStreamer which encapsulates the output stream and formatting used when outstreaming.
     * 
     * @param streamer
     *            The OutputStreamer to use when outstreaming results.
     */
    public void setOutputStreamer(final XmlOutputStreamer streamer) {
        this.outputStreamer = streamer;
    }

    /**
     * Streams out the String provided to the output stream. Does nothing if outstreaming is not in effect.
     * 
     * @param elem
     *            the Element to be streamed out.
     * @throws XAwareException
     *             If there is no OutputStreamer or if there is an error writing to the output stream.
     */
    public void streamOutElementContent(final Element elem) throws XAwareException {
        if (outputStreamer != null) {
            outputStreamer.writeElementContent(elem);
        }
    }

    /**
     * Streams out the Element provided to the output stream. Does nothing if outstreaming is not in effect.
     * 
     * @param elem
     *            the Element to be streamed out.
     * @throws XAwareException
     *             If there is no OutputStreamer or if there is an error writing to the output stream.
     */
    public void streamOutElement(final Element elem) throws XAwareException {
        if (outputStreamer != null) {
            outputStreamer.writeElement(elem);
        }
    }

    /**
     * Streams out the starting tag and any remaining (non-instruction) attributes of the Element provided to the output
     * stream. Does nothing if outstreaming is not in effect.
     * 
     * @param elem
     *            the Element whose start tag and attributes are to be streamed out.
     * @throws XAwareException
     *             If there is no OutputStreamer or if there is an error writing to the output stream.
     */
    public void streamOutStartingTag(final Element elem) throws XAwareException {
        if (outputStreamer != null) {
            outputStreamer.streamOutStartTag(elem);
            streamedOutStartTag = true;
        }
    }

    /**
     * Streams out the ending tag for the Element provided to the output stream. Does nothing if outstreaming is not in
     * effect.
     * 
     * @param elem
     *            the Element whose end tag is to be streamed out.
     * @throws XAwareException
     *             If there is no OutputStreamer or if there is an error writing to the output stream.
     */
    public void streamOutEndingTag(final Element elem) throws XAwareException {
        if (outputStreamer != null) {
            outputStreamer.streamOutEndTag(elem);
        }
    }

    /**
     * Takes the content from the element starting at the given index and either going until the exclusiveEnd is reached
     * or until an Element is found in the content whichever comes first. The index of the last content object that was
     * streamed out is returned.  Substitution is performed on the Text Content objects.
     * 
     * @param start
     * @param exclusiveEnd
     * @return The index of the last content object that was streamed out
     * @throws XAwareException
     */
    protected int substituteMixedModeContent(int start, final int exclusiveEnd, final boolean visibleForStreamingOut) throws XAwareException {
        if (start < 0) {
            start = 0;
        }
        if (start > exclusiveEnd) {
            return start;
        }
        int lastIndex = start;
        if (start == exclusiveEnd && start > 0) {
            Content content = element.getContent(start - 1);
            if (!(content instanceof Element)) {
                lastIndex = start - 1;
            }
        }
        for (; lastIndex < exclusiveEnd; lastIndex++) {
            Content aContentItem = element.getContent(lastIndex);
            if (processedContent.contains(aContentItem)) {
                continue;
            } else {
                processedContent.add(aContentItem);
            }
            if (aContentItem instanceof Element) {
                return lastIndex - 1;
            }
            if (aContentItem instanceof Text) {
                Text text = (Text) aContentItem;
                String textString = text.getValue();
                String newText = substitute(textString);
                if (!textString.equals(newText)) {
                    element.setContent(lastIndex, new Text(newText));
                    element.removeContent(text);
                }
            } else if (aContentItem instanceof Comment) {
                Comment aComment = (Comment) aContentItem;
                aComment.setText(substitute(aComment.getText()));
            }
            if (isStreamingOut() && visibleForStreamingOut) {
                getOutputStreamer().writeContent(aContentItem);
            }
        }
        return lastIndex;
    }

    /**
     * Gets the value of the isInsidePruneElement field. This field
     * is used to suppress outstreaming when executing Elements inside
     * of xa:visible="prune", and is inherited into descendant Elements.
     * 
     * @return the value of the isInsidePruneElement field.
     */
    public boolean isInsidePruneElement() {
        return isInsidePruneElement;
    }

    /**
     * Sets the value of the isInsidePruneElement field. This field
     * is used to suppress outstreaming when executing Elements inside
     * of xa:visible="prune", and is inherited into descendant Elements.
     * 
     * @param insidePruneElement
     *            the new value to set for the isInsidePruneElement field.
     */
    public void setInsidePruneElement(final boolean insidePruneElement) {
        isInsidePruneElement = insidePruneElement;
    }

    /**
     * @param skipStartingTag
     *            the skipStartingTag to set
     */
    public void setSkipStartingTag(final boolean skipStartingTag) {
        this.skipStartingTag = skipStartingTag;
    }

    /**
     * Returns a String representation of the path to this ScriptNode.
     * 
     * @return a String representation of the path to this ScriptNode.
     */
    public String getPathToNode() {
        return "/" + getParentPath(element);
    }

    /**
     * Recurse up the tree to get path to current node in the current context.
     * 
     * @param elem
     * @return
     */
    private String getParentPath(final Element elem) {
        final Element parent = elem.getParentElement();
        final StringBuffer buff = new StringBuffer();
        if (parent != null) {
            buff.append(getParentPath(parent));
            buff.append('/');
        }
        final String prefix = elem.getNamespacePrefix();
        if (prefix.length() > 0) {
            buff.append(prefix).append(':');
        }
        buff.append(elem.getName());
        return buff.toString();
    }

    /**
     * Returns the name of this object.
     * 
     * @return a String containing the name of this object.
     */
    public String getName() {
        if (element != null) {
            return element.getName();
        }
        return null;
    }

    /**
     * Returns a String representation of the object, including attribute values of the associated Element.
     * 
     * @return a String representation of the object.
     */
    @Override
    public String toString() {
        return toString(element);
    }

    /**
     * Returns a String representation of the object, including attribute values of the Element argument.
     * 
     * @param elem
     * @return
     */
    public static String toString(final Element elem) {
        final Format pretty = Format.getPrettyFormat();
        final XMLOutputter op = new XMLOutputter(pretty);
        return op.outputString(elem);
    }

    /**
     * This method performs substitution using values appropriate to the context
     * in which the Instruction is running.
     * 
     * @param sOrig
     *            String containing the substitution tokens.
     * @return The result of performing substitution on the String provided.
     * @throws XAwareSubstitutionException
     *             if an error occurs during substitution and the effective
     *             substitution failure level is set to error.
     * @throws XAwareException
     *             if substitution involves execution of a functoid and that
     *             functoid execution fails.
     */
    public String substitute(final String sOrig) throws XAwareSubstitutionException, XAwareException {
        return context.substitute(sOrig, this, null);
    }

    /**
     * This method performs substitution using values appropriate to the context
     * in which the Instruction is running.
     * 
     * @param sOrig
     *            String containing the substitution tokens.
     * @param relativeElem
     *            the Element to be used as the relative element during substitution.
     * @return The result of performing substitution on the String provided.
     * @throws XAwareSubstitutionException
     *             if an error occurs during substitution and the effective
     *             substitution failure level is set to error.
     * @throws XAwareException
     *             if substitution involves execution of a functoid and that
     *             functoid execution fails.
     */
    public String substitute(final String sOrig, final Element relativeElem) throws XAwareSubstitutionException, XAwareException {
        return context.substitute(sOrig, this, relativeElem);
    }

    /**
     * Performs substitution on the element provided (element text and attributes), and all of its children recursively.
     * 
     * @param currentElem
     *            The current element upon which substitution is performed.
     * @param relativeElem
     *            the Element to be used as the relative element during substitution.
     * @throws XAwareSubstitutionException
     *             if an error occurs during substitution and the effective
     *             substitution failure level is set to error.
     * @throws XAwareException
     *             if substitution involves execution of a functoid and that
     *             functoid execution fails.
     */
    public void substituteAllElements(final Element currentElem, final Element relativeElem) throws XAwareSubstitutionException, XAwareException {
        context.substituteAllElements(currentElem, relativeElem, effectiveSubstitutionFailureLevel);
    }

    /**
     * Performs substitution on this ScriptNode's Element, including all attributes of the Element
     * and all of its text content objects even if the element has child elements.
     *
     * @param relativeElem
     *            the Element to be used as the relative element during substitution.
     * @throws XAwareSubstitutionException
     *             if an error occurs during substitution and the effective
     *             substitution failure level is set to error.
     * @throws XAwareException
     *             if substitution involves execution of a functoid and that
     *             functoid execution fails.
     */
    public void substituteElementTextAndAttributes(final Element relativeElem) throws XAwareSubstitutionException, XAwareException {
        context.substituteElementTextAndAttributes(this, relativeElem);
    }

    public void setStreamingOutErrorHandling(boolean streamingOutErrorHandling) {
        isStreamingOutErrorHandling = streamingOutErrorHandling;
    }
}
