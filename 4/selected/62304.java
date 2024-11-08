package sample.dataflow;

import java.util.concurrent.Future;
import org.hypergraphdb.app.dataflow.*;

/**
 * 
 * <p>
 * This is a very simple example demonstrating the basic data flow API. The main
 * program constructs a network that takes a single stream of integers input and
 * calculates the sums of all its even numbers, all its odd numbers and the total
 * sum. To do that, it splits the input into a stream of even numbers and a stream
 * of odd numbers. The sum of each of those streams is accumulated separately and finally
 * put back together. 
 * </p>
 * 
 * <p>
 * The processing nodes here are implemented by a few very simple classes, for illustration
 * purposes only. 
 * </p>
 * 
 * @author Borislav Iordanov
 *
 */
public class DataFlowArithmetic {

    /**
	 * 
	 * <p>
	 * Expected two inputs from two input channels and it writes the 
	 * output to a single output channel. The names of the channels are
	 * specified in the constructor.
	 * </p>
	 *
	 * @author Borislav Iordanov
	 *
	 */
    public static class Sum extends AbstractProcessor<Object> {

        private String left, right, output;

        public Sum() {
        }

        public Sum(String left, String right, String output) {
            this.left = left;
            this.right = right;
            this.output = output;
        }

        public void process(Object ctx, Ports ports) throws InterruptedException {
            InputPort<Integer> inLeft = ports.getInput(left);
            InputPort<Integer> inRight = ports.getInput(right);
            OutputPort<Integer> sumOut = ports.getOutput(output);
            for (Integer x : inLeft) {
                Integer y = inRight.take();
                if (inLeft.isEOS(y)) throw new RuntimeException("Sum is only defined on pair of numbers.");
                sumOut.put(x + y);
            }
        }
    }

    /**
	 * 
	 * <p>
	 * Expects a single input stream and it continuously calculates the sum
	 * of the numbers coming in, output it to its output channel only when
	 * the input reaches EOS. 
	 * </p>
	 *
	 * @author Borislav Iordanov
	 *
	 */
    public static class Accumulator extends AbstractProcessor<Object> {

        private String inputChannel;

        private String outputChannel;

        public Accumulator() {
        }

        public Accumulator(String inputChannel, String outputChannel) {
            this.inputChannel = inputChannel;
            this.outputChannel = outputChannel;
        }

        public void process(Object ctx, Ports ports) throws InterruptedException {
            InputPort<Integer> in = ports.getInput(inputChannel);
            OutputPort<Integer> out = ports.getOutput(outputChannel);
            int total = 0;
            for (Integer x : in) total += x;
            out.put(total);
        }
    }

    /**
	 * 
	 * <p>
	 * Split the input stream into even and odd, writing each to a different
	 * channel. Here, the channel names are hard-coded in the implementation
	 * rather than passed at construction time.
	 * </p>
	 *
	 * @author Borislav Iordanov
	 *
	 */
    public static class SplitParity extends AbstractProcessor<Object> {

        public void process(Object ctx, Ports ports) throws InterruptedException {
            InputPort<Integer> in = ports.getInput("random-stream");
            OutputPort<Integer> outEven = ports.getOutput("even-numbers");
            OutputPort<Integer> outOdd = ports.getOutput("odd-numbers");
            for (Integer x : in) {
                if (x % 2 == 0) outEven.put(x); else outOdd.put(x);
            }
        }
    }

    /**
	 * 
	 * <p>
	 * Prints to stdout whatever comes in from its single input port, prefixing
	 * it with a predefined, at construction time, string.
	 * </p>
	 *
	 * @author Borislav Iordanov
	 *
	 */
    public static class Printer extends AbstractProcessor<Object> {

        private String fromChannel;

        private String prefix;

        public Printer() {
        }

        public Printer(String prefix, String fromChannel) {
            this.fromChannel = fromChannel;
            this.prefix = prefix;
        }

        public void process(Object ctx, Ports ports) throws InterruptedException {
            InputPort<Integer> in = ports.getInput(fromChannel);
            for (Integer x = in.take(); !in.isEOS(x); x = in.take()) System.out.println(prefix + x);
        }
    }

    public static void main(String argv[]) {
        Integer EOF = Integer.MIN_VALUE;
        DataFlowNetwork<Object> network = new DataFlowNetwork<Object>();
        network.addChannel(new Channel<Integer>("random-stream", EOF));
        network.addChannel(new Channel<Integer>("even-numbers", EOF));
        network.addChannel(new Channel<Integer>("odd-numbers", EOF));
        network.addChannel(new Channel<Integer>("even-sum", EOF));
        network.addChannel(new Channel<Integer>("odd-sum", EOF));
        network.addChannel(new Channel<Integer>("total-sum", EOF));
        network.addNode(new SplitParity(), new String[] { "random-stream" }, new String[] { "even-numbers", "odd-numbers" });
        network.addNode(new Accumulator("even-numbers", "even-sum"), new String[] { "even-numbers" }, new String[] { "even-sum" });
        network.addNode(new Accumulator("odd-numbers", "odd-sum"), new String[] { "odd-numbers" }, new String[] { "odd-sum" });
        network.addNode(new Sum("even-sum", "odd-sum", "total-sum"), new String[] { "even-sum", "odd-sum" }, new String[] { "total-sum" });
        network.addNode(new Printer("Even sum=", "even-sum"), new String[] { "even-sum" }, new String[0]);
        network.addNode(new Printer("Odd sum=", "odd-sum"), new String[] { "odd-sum" }, new String[0]);
        network.addNode(new Printer("Total sum=", "total-sum"), new String[] { "total-sum" }, new String[0]);
        try {
            Future<Boolean> f = network.start();
            Channel<Integer> ch = network.getChannel("random-stream");
            for (int i = 0; i < 1000; i++) {
                ch.put((int) (Math.random() * 10000));
            }
            ch.put(EOF);
            System.out.println("Network completed successfully: " + f.get());
            f = network.start();
            ch = network.getChannel("random-stream");
            for (int i = 0; i < 1000; i++) {
                ch.put((int) (Math.random() * 10000));
            }
            ch.put(EOF);
            System.out.println("Network completed successfully: " + f.get());
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        } finally {
            network.shutdown();
        }
    }
}
