package org.jcompany.commons.semaphore;

import java.util.LinkedList;

/**
 * jCompany. Classe utilit�ria para manter singletons de caching
 *      thread-safetey, controlando concorr�ncia 
 *		no momento da atualiza��o e permitindo concorr�ncia nas leituras
 * Baseado em algoritmo testado em sites de larga escala.
 * @since jCompany 3.0
 * @version $Id: PlcReaderPersister.java,v 1.2 2006/05/17 20:38:13 rogerio_baldini Exp $
*/
public class PlcReaderPersister {

    private int active_readers;

    private int waiting_readers;

    private int active_writers;

    /**   
	I keep a linked list of writers waiting for access so that I can
	release them in the order that the requests were received.  The size of
	this list is the "waiting writers" count.  Note that the monitor of the
	PlcReaderPersister object itself is used to lock out readers
	while writes are in progress, thus there's no need for a separate
	"reader_lock."
	*/
    private final LinkedList writer_locks = new LinkedList();

    /**
	Request the read lock. Block until a read operation can be performed
	safely.  This call must be followed by a call to
	read_accomplished() when the read operation completes.
	*/
    public synchronized void reading() {
        if (active_writers == 0 && writer_locks.size() == 0) ++active_readers; else {
            ++waiting_readers;
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
	This version of leitura() requests read access and returns
	true if you get it. If it returns false, you may not
	safely read from the guarded resource. If it returns true, you
	should do the read, then call read_accomplished in the
	normal way. Here's an example:

		public void leitura()
		{   if( lock.request_immediate_leitura() )
			{   try
				{
					// do the read operation here
				}
				finally
				{   lock.leituraCompleta();
				}
			}
			else
				// couldn't read safely.
		}
	*/
    public synchronized boolean requestImmediateReading() {
        if (active_writers == 0 && writer_locks.size() == 0) {
            ++active_readers;
            return true;
        }
        return false;
    }

    /**   
	Release the lock. You must call this method when you're done
	with the read operation.
	*/
    public synchronized void readingFull() {
        if (--active_readers == 0) notify_writers();
    }

    /** 
	Request the write lock. Block until a write operation can be performed
	safely. Write requests are guaranteed to be executed in the order
	received. Pending read requests take precedence over all write
	requests.  This call must be followed by a call to
	gravacaoCompleta() when the write operation completes.
	*/
    public void writing() {
        Object lock = new Object();
        synchronized (lock) {
            synchronized (this) {
                boolean okay_to_write = writer_locks.size() == 0 && active_readers == 0 && active_writers == 0;
                if (okay_to_write) {
                    ++active_writers;
                    return;
                }
                writer_locks.addLast(lock);
            }
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**    
	This version of the write request returns false immediately
	(without blocking) if any read or write operations are in progress and
	a write isn't safe; otherwise, it returns true and acquires the
	resource. Use it like this:

		public void gravacao()
		{   if( lock.request_immediate_gravacao() )
			{   try
				{
					// do the write operation here
				}
				finally
				{   lock.gravacaoCompleta();
				}
			}
			else
				// couldn't write safely.
		}
	*/
    public synchronized boolean requestImmediateWriting() {
        if (writer_locks.size() == 0 && active_readers == 0 && active_writers == 0) {
            ++active_writers;
            return true;
        }
        return false;
    }

    /**      
	Release the lock. You must call this method when you're done
	with the read operation.
	*/
    public synchronized void writingFull() {
        --active_writers;
        if (waiting_readers > 0) notify_readers(); else notify_writers();
    }

    /**
	Notify all the threads that have been waiting to read.
    */
    private void notify_readers() {
        active_readers += waiting_readers;
        waiting_readers = 0;
        notifyAll();
    }

    /**   
	 Notify the writing thread that has been waiting the longest.
     */
    private void notify_writers() {
        if (writer_locks.size() > 0) {
            Object oldest = writer_locks.removeFirst();
            ++active_writers;
            synchronized (oldest) {
                oldest.notify();
            }
        }
    }
}
