package at.jku.semwiq.mediator.registry.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import at.jku.semwiq.mediator.registry.RegistryException;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author dorgon, Andreas Langegger, al@jku.at
 * 
 * Attention!
 * 
 * All sub-classes using the RdfWrapperBase should take care of concurrency and fulfill
 * the locking contract, see http://jena.sourceforge.net/how-to/concurrency.html
 * 
 * For updates, the requestExclusiveWriteLock()/returnExclusiveWriteLock() support methods
 * can be used - a setter method just needs to call checkLock() before changing the resource.
 */
public class UpdatableResource extends WrappedResource {

    private static final Logger log = LoggerFactory.getLogger(UpdatableResource.class);

    private static final int WAIT_INTERVAL = 1000;

    /** store the owner of the lock */
    private Thread lockOwner = null;

    /**
	 * constructor
	 * @param r
	 */
    public UpdatableResource(Resource r) {
        super(r);
    }

    /**
	 * always call returnExclusiveWriteLock() in a finally {} block !!!
	 * TODO: not sync'ed to prevent from dead locks, but should rethink and improve locking
	 */
    public void requestExclusiveWriteLock() {
        while (lockOwner != null) {
            try {
                wait(WAIT_INTERVAL);
            } catch (InterruptedException ignore) {
                log.debug("Thread " + Thread.currentThread() + " is waiting for exclusive write lock for " + this + " which is still owned by Thread " + lockOwner + "...");
            }
        }
        lockOwner = Thread.currentThread();
        model.enterCriticalSection(Lock.WRITE);
        if (log.isDebugEnabled()) log.debug("Thread " + Thread.currentThread().getId() + " obtained exclusive write lock for " + this + ".");
    }

    /**
	 * should be called in a finally {} block after requrestExclusiveWriteLock()
	 */
    public void returnExclusiveWriteLock() {
        if (lockOwner == Thread.currentThread()) {
            model.leaveCriticalSection();
            lockOwner = null;
            if (log.isDebugEnabled()) log.debug("Thread " + Thread.currentThread().getId() + " returned exclusive write lock for " + this + ".");
        } else throw new RuntimeException("Attempt to return exclusive write lock by another thread.");
    }

    protected synchronized void checkLock() throws RegistryException {
        if (lockOwner != Thread.currentThread()) throw new RegistryException("Attempt to modify UpdatableResource <" + this + "> without owning the exclusive write lock.");
    }

    public void setSameAs(String uri) throws RegistryException {
        checkLock();
        resource.removeAll(OWL.sameAs);
        addSameAs(uri);
    }

    public void addSameAs(String uri) throws RegistryException {
        checkLock();
        if (!resource.hasProperty(OWL.sameAs, model.getResource(uri))) resource.addProperty(OWL.sameAs, model.createResource(uri));
    }

    public void setLabel(String label) throws RegistryException {
        checkLock();
        if (resource.hasProperty(RDFS.label)) resource.getProperty(RDFS.label).changeObject(label); else resource.getModel().add(resource, RDFS.label, label);
    }
}
