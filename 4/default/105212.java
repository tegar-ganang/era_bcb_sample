import java.util.Vector;

/**
 * Implementa un cerrojo para un fichero de tal manera que el fichero puede ser
 * accedido o bien por varios lectores o bien por un único escritor. Es decir,
 * el lock o bien lo tiene un escritor o bien lo tienen 1 o más lectores.
 */
public class FileLock {

    private Vector<Thread> queue = new Vector<Thread>();

    private Vector<Thread> readers = new Vector<Thread>();

    private Thread currentWriter = null;

    public FileLock() {
    }

    /**
	 * Devuelve true si el lock está bloqueado por un escritor, si no
	 * devuelve false.
	 */
    private synchronized boolean writeLocked() {
        return currentWriter != null;
    }

    /**
	 * Devuelve true si el lock está bloqueado por uno o más lectores, si
	 * no devuelve false.
	 */
    private synchronized boolean readLocked() {
        return !readers.isEmpty();
    }

    /**
	 * Bloquea el lock para escritores, los lectores pueden seguir accediendo.
	 */
    public synchronized void readLock() {
        Thread reader = Thread.currentThread();
        if (writeLocked()) {
            queue.addElement(reader);
            while (writeLocked() || reader != queue.firstElement()) {
                try {
                    Log.println("bloqueado", Log.LOCK);
                    this.wait();
                } catch (InterruptedException e) {
                    Log.println("Exception: " + e);
                }
            }
            Log.println("despertado, lector", Log.LOCK);
            queue.removeElement(reader);
        }
        Log.println("conseguido lock, lector", Log.LOCK);
        readers.addElement(reader);
        this.notifyAll();
    }

    /**
	 * Bloquea el lock para lectores y los demás escritores.
	 */
    public synchronized void writeLock() {
        Thread writer = Thread.currentThread();
        if (writeLocked() || readLocked()) {
            queue.addElement(writer);
            while (writeLocked() || readLocked() || writer != queue.firstElement()) {
                try {
                    Log.println("bloqueado", Log.LOCK);
                    this.wait();
                } catch (InterruptedException e) {
                    Log.println("Exception: " + e);
                }
            }
            Log.println("despertado, escritor", Log.LOCK);
            queue.removeElement(writer);
        }
        Log.println("conseguido lock, escritor", Log.LOCK);
        currentWriter = writer;
        this.notifyAll();
    }

    /**
	 * Libera el lock si el thread que invoca el método es el escritor que
	 * tenía bloqueado el lock o si es el último lector que tenía bloqueado
	 * el lock.
	 */
    public synchronized void unlock() {
        Thread current = Thread.currentThread();
        if (current == currentWriter) {
            currentWriter = null;
            Log.println("liberado lock, escritor", Log.LOCK);
            this.notifyAll();
        } else if (readers.contains(current)) {
            readers.removeElement(current);
            if (readers.isEmpty()) {
                Log.println("liberado lock, lector", Log.LOCK);
                this.notifyAll();
            }
        }
    }
}
