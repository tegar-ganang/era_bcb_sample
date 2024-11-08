package net.sf.mytoolbox.concurrent;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls concurrent accesses to methods. <br/>
 * @author ggrussenmeyer
 * @see ConcurrentAccess
 */
public class ConcurrentAccessController implements MethodInterceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ReadWriteLock readWriteLock;

    /**
     * Creates a new controller. <br/>
     */
    public ConcurrentAccessController() {
        this.readWriteLock = new ReentrantReadWriteLock(true);
    }

    private Lock acquireLock(ConcurrentAccess configuration) {
        Lock acquiredLock = null;
        if (configuration != null) {
            if (configuration.exclusive()) {
                acquiredLock = this.readWriteLock.writeLock();
            } else {
                acquiredLock = this.readWriteLock.readLock();
            }
            acquiredLock.lock();
            if (this.log.isDebugEnabled()) {
                log.debug("Acquired " + (configuration.exclusive() ? "exclusive " : "") + "lock");
            }
        } else if (this.log.isDebugEnabled()) {
            this.log.debug("No lock acquisition required");
        }
        return acquiredLock;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method targetMethod = invocation.getMethod();
        ConcurrentAccess configuration = targetMethod.getAnnotation(ConcurrentAccess.class);
        final Lock acquiredLock = this.acquireLock(configuration);
        try {
            return invocation.proceed();
        } finally {
            if (acquiredLock != null) {
                acquiredLock.unlock();
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Lock released");
                }
            }
        }
    }
}
