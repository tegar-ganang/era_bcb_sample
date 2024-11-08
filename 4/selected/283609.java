package org.inqle.core.ds;

import java.util.Dictionary;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;

public class TestService implements ITestService {

    private ITestComponent component;

    protected ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public void setComponent(ITestComponent c) {
        readWriteLock.writeLock().lock();
        System.out.println("registering component: " + c.toString());
        component = c;
        testComponents();
        readWriteLock.writeLock().unlock();
    }

    public synchronized void unsetComponent(ITestComponent c) {
        readWriteLock.writeLock().lock();
        component = null;
        readWriteLock.writeLock().unlock();
    }

    public synchronized void testComponents() {
        readWriteLock.readLock().lock();
        if (component == null) {
            System.out.println("Component = null");
        } else {
            Dictionary<?, ?> props = component.getProperties();
            System.out.println("Component = " + props.toString());
            System.out.println("test.property=" + props.get("test.property"));
        }
        readWriteLock.readLock().unlock();
    }
}
