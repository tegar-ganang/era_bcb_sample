package thread.semaphores;

import junit.framework.Assert;
import org.junit.Test;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

/**
 * You can use a Semaphore to turn any collection into a
 * blocking bounded collection, as illustrated by
 * BoundedHashSet in Listing. The semaphore is initialized
 * to the desired maximum size of the collection. The add
 * operation acquires a permit before adding the item into
 * the underlying collection. If the underlying add
 * operation does not actually add anything, it releases the
 * permit immediately. Similarly, a successful remove
 * operation releases a permit, enabling more elements to be
 * added. The underlying Set implementation knows nothing
 * about the bound; this is handled by BoundedHashSet.
 * 
 * @author Sergiy Doroshenko webserg@gmail.com Feb 9, 2009
 *         1:27:37 PM
 */
public class BoundedHashSet<T> {

    private final Set<T> set;

    private final Semaphore sem;

    public BoundedHashSet() {
        this.set = Collections.synchronizedSet(new HashSet<T>());
        sem = new Semaphore(0);
    }

    public BoundedHashSet(int bound) {
        this.set = Collections.synchronizedSet(new HashSet<T>());
        sem = new Semaphore(bound);
    }

    public boolean add(T o) throws InterruptedException {
        sem.acquire();
        boolean wasAdded = false;
        try {
            wasAdded = set.add(o);
            return wasAdded;
        } finally {
            if (!wasAdded) sem.release();
        }
    }

    public int getSize() {
        return set.size();
    }

    public boolean remove(Object o) {
        boolean wasRemoved = set.remove(o);
        if (wasRemoved) sem.release();
        return wasRemoved;
    }

    /**
     * 
     * @throws InterruptedException
     */
    @Test(timeout = 2000)
    public void useBoundedHashSet() throws InterruptedException {
        final BoundedHashSet<String> bset = new BoundedHashSet<String>(10);
        System.out.println(bset.getSize());
        final CyclicBarrier barrier = new CyclicBarrier(2, new Runnable() {

            public void run() {
                Assert.assertEquals(10, bset.getSize());
            }
        });
        Thread write = new Thread() {

            public void run() {
                for (int i = 0; i < 20; i++) {
                    try {
                        bset.add("" + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    barrier.await();
                } catch (InterruptedException ex) {
                    return;
                } catch (java.util.concurrent.BrokenBarrierException ex) {
                    return;
                }
            }
        };
        Thread remove = new Thread() {

            public void run() {
                for (int i = 0; i < 10; i++) {
                    bset.remove("" + i);
                }
                try {
                    barrier.await();
                } catch (InterruptedException ex) {
                    return;
                } catch (BrokenBarrierException ex) {
                    return;
                }
            }
        };
        write.start();
        while (true) {
            if (bset.getSize() == 10) {
                remove.start();
                break;
            }
        }
        Assert.assertEquals(10, bset.getSize());
    }
}
