package com.nipun.neural.engine.runners;

import java.util.ArrayList;
import java.util.List;
import com.nipun.neural.objects.Network;

/**
 * can predict a series of Doubles
 * @author NipunKumar
 *
 */
public class TimeSeriesPredictor extends SimpleNetworkRunner {

    private int values;

    private queue q;

    /**
	 * series is expected to be a list containing Doubles.
	 * 
	 * we will set input values in progression from the list and check against
	 * required number of following values
	 * 
	 * we also assume only one output neuron for the network
	 * 
	 * @param network
	 * @param series
	 */
    public TimeSeriesPredictor(Network network) {
        super(network);
        values = 0;
        q = new queue(network.getInputNodes().length);
    }

    public List run(List inputs) {
        List outList = new ArrayList();
        double err = 0;
        Double currResult = null;
        double currError = 0;
        for (int i = 0; i < inputs.size(); i++) {
            values++;
            currResult = runForCurrent((Double) inputs.get(i));
            if (currResult != null) {
                currError = (currResult.doubleValue() - ((Double) inputs.get(i)).doubleValue()) / (((Double) inputs.get(i)).doubleValue());
                err += currError * currError;
            }
        }
        System.out.println("average error:::=-> " + Math.sqrt(err / values) * 100 + "%");
        outList.add(currResult);
        return outList;
    }

    public Double runForCurrent(Double currVal) {
        Double result = null;
        if (q.isQFull()) {
            List resList = super.run(q.getValues());
            List tempList = new ArrayList();
            tempList.add(currVal);
            result = (Double) resList.get(0);
            super.backPropagate(tempList);
        }
        q.addToQueue(currVal);
        return result;
    }

    public void backPropagate(List expectations) {
        System.out.println("no we will not backpropagate in timeseries predictor...it is done during every run");
    }

    private class queue {

        private Object[] q;

        private boolean isFull;

        private int pos;

        private int size;

        public queue(int size) {
            isFull = false;
            this.size = size;
            pos = -1;
            if (size > 0) {
                q = new Object[size];
            } else {
                System.out.println("bogus queue created...");
            }
        }

        public void addToQueue(Object o) {
            if (!isFull) {
                pos++;
                q[pos] = o;
            } else {
                for (int i = 0; i < size - 1; i++) {
                    q[i] = q[i + 1];
                }
                q[size - 1] = o;
            }
            if (pos >= (size - 1)) {
                isFull = true;
            }
        }

        public boolean isQFull() {
            return isFull;
        }

        public List getValues() {
            if (!isFull) {
                System.out.println("why are you trying to get the values from a q which is not full yet??");
            }
            List l = new ArrayList();
            for (int i = 0; i < size; i++) {
                l.add(q[i]);
            }
            return l;
        }
    }
}
