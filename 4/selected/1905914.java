package taskgraph.tasks.data;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import taskgraph.ports.InputPort;
import taskgraph.ports.OutputPort;
import taskgraph.tasks.PrimitiveTask;

/**
 * Merges two incoming streams of elements into a single stream.
 * 
 * <p>This task will read an element from the first stream and write to the
 * output port; then will read an element from the second stream and write
 * it to the output port; in that order. Thus the output port will contain
 * interleaved elements from both streams as long as there are elements coming
 * from both. At the end of one stream, elements of the other will continue
 * to be put through the output stream until both streams are finished.
 * 
 * @author Armando Blancas
 * @see taskgraph.ports.InputPort
 * @see taskgraph.ports.OutputPort
 * @param <E> The type of elements merged by this task.
 */
public class Merge<E> extends PrimitiveTask {

    private InputPort<E> first;

    private InputPort<E> second;

    private OutputPort<E> output;

    /**
	 * This constructor allows creating instances as beans.
	 */
    public Merge() {
    }

    /**
     * Merge constructor.
     * 
     * @param first The first element input stream.
     * @param second The second element input stream.
     * @param output The single element output stream.
     */
    public Merge(final InputPort<E> first, final InputPort<E> second, final OutputPort<E> output) {
        setFirst(first);
        setSecond(second);
        setOutput(output);
    }

    /**
	 * Gets the input port 'first'.
	 * 
	 * @return The input port 'first'.
	 */
    public InputPort<E> getFirst() {
        return first;
    }

    /**
	 * Gets the output port.
	 * 
	 * @return The output port.
	 */
    public OutputPort<E> getOutput() {
        return output;
    }

    /**
	 * Gets the input port 'second'.
	 * 
	 * @return The input port 'second'.
	 */
    public InputPort<E> getSecond() {
        return second;
    }

    /**
     * Performs the work of this task.
     * 
	 *<pre>
	 *loop while this thread is alive
	 *    if first is active
	 *        read an element from first
	 *        write it to the output port
	 *    if second is active
	 *        read an element from second
	 *        write it to the output port
	 *    if neither is active
	 *        quit the loop
	 * end loop
	 * close first
	 * close second
	 * close output
	 *</pre>
	 */
    @Override
    public void run() {
        try {
            boolean firstActive = true;
            boolean secondActive = true;
            while (!Thread.currentThread().isInterrupted()) {
                if (firstActive) {
                    try {
                        output.write(first.read());
                    } catch (EOFException e) {
                        firstActive = false;
                    }
                }
                if (secondActive) {
                    try {
                        output.write(second.read());
                    } catch (EOFException e) {
                        secondActive = false;
                    }
                }
                if (!firstActive && !secondActive) {
                    break;
                }
            }
        } catch (InterruptedIOException e) {
            log.error("Merge#run()", e);
        } catch (IOException e) {
            log.error("Merge#run()", e);
        } finally {
            first.close();
            second.close();
            output.close();
        }
    }

    /**
	 * Sets the input port 'first'.
	 * 
	 * @param first The input port to set.
	 */
    public void setFirst(final InputPort<E> first) {
        if (first == null) throw new IllegalArgumentException("first == null");
        this.first = first;
    }

    /**
	 * Sets the single output port.
	 * 
	 * @param output The output to set.
	 */
    public void setOutput(final OutputPort<E> output) {
        if (output == null) throw new IllegalArgumentException("output == null");
        this.output = output;
    }

    /**
	 * Sets the input port 'second'.
	 * 
	 * @param second The input port to set.
	 */
    public void setSecond(final InputPort<E> second) {
        if (second == null) throw new IllegalArgumentException("second == null");
        this.second = second;
    }
}
