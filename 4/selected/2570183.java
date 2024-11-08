package jather;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.View;

/**
 * Instances of this class execute individual process requests.
 * 
 * @author blueneil
 * 
 */
public class ExecutiveRunnable implements Runnable {

    protected static final Log log = LogFactory.getLog(ExecutiveRunnable.class);

    private Executive executive;

    private ProcessDefinition processDefinition;

    private Throwable innerThrowable;

    /**
	 * Executive constructor.
	 * 
	 * @param executive
	 *            the parent executive.
	 * @param processDefinition
	 *            the process definition to manage.
	 */
    public ExecutiveRunnable(Executive executive, ProcessDefinition processDefinition) {
        setExecutive(executive);
        setProcessDefinition(processDefinition);
    }

    /**
	 * @see java.lang.Runnable#run()
	 */
    @Override
    public void run() {
        try {
            log.debug("Running process for:" + getProcessId());
            View view = getExecutive().getRpcDispatcher().getChannel().getView();
            Set<Address> group = new HashSet<Address>();
            group.addAll(view.getMembers());
            group.remove(getExecutive().getRpcDispatcher().getChannel().getLocalAddress());
            group.remove(getProcessId().getSource());
            if (getExecutive().getMaxGroupSize() < 0 || group.size() <= getExecutive().getMaxGroupSize()) {
                JatherHandlerStub stub = new JatherHandlerStub(getExecutive().getRpcDispatcher(), getProcessId().getSource());
                ProcessContext result = stub.processRequest(new RequestContext(getProcessId(), getExecutive().getChannel().getLocalAddress()));
                ChannelClassLoader cl = new ChannelClassLoader(getExecutive().getRpcDispatcher(), getProcessId().getSource());
                Callable<?> callable = result.getCallable(cl);
                if (callable != null) {
                    log.debug("Executing: " + callable);
                    try {
                        result.setResult(callable.call());
                    } catch (Exception e) {
                        result.setCallableException(e);
                    }
                    stub.processResult(result);
                }
            }
            getExecutive().getProcessMap().remove(getProcessId());
        } catch (Throwable e) {
            log.error(getProcessDefinition(), e);
            setInnerThrowable(e);
        }
    }

    /**
	 * The parent executive.
	 * 
	 * @return the executive
	 */
    public Executive getExecutive() {
        return executive;
    }

    /**
	 * The parent executive.
	 * 
	 * @param executive
	 *            the executive to set
	 */
    public void setExecutive(Executive executive) {
        this.executive = executive;
    }

    /**
	 * The process id being managed.
	 * 
	 * @return the processId
	 */
    public ProcessId getProcessId() {
        return getProcessDefinition().getProcessId();
    }

    /**
	 * The requested process definition.
	 * 
	 * @return the processDefinition
	 */
    protected ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    /**
	 *The requested process definition.
	 * 
	 * @param processDefinition
	 *            the processDefinition to set
	 */
    protected void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

    /**
	 * The unhandled throwable from the run method if there is one.
	 * 
	 * @return the innerThrowable
	 */
    protected Throwable getInnerThrowable() {
        return innerThrowable;
    }

    /**
	 * The unhandled throwable from the run method.
	 * 
	 * @param innerThrowable
	 *            the innerThrowable to set
	 */
    protected void setInnerThrowable(Throwable innerThrowable) {
        this.innerThrowable = innerThrowable;
    }
}
