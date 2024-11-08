package org.neuroph.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import org.neuroph.util.NeuronFactory;
import org.neuroph.util.NeuronProperties;

/**
 *<pre>
 * Layer of neurons in a neural network. The Layer is basic neuron container (a collection of neurons),
 * and it provides methods for manipulating neurons (add, remove, get, set, calculate, randomize).
 * </pre>
 * 
 * @see Neuron
 * @author Zoran Sevarac <sevarac@gmail.com>
 */
public class Layer implements Serializable {

    /**
	 * The class fingerprint that is set to indicate serialization 
	 * compatibility with a previous version of the class
	 */
    private static final long serialVersionUID = 3L;

    /**
	 * Reference to parent neural network
	 */
    private NeuralNetwork parentNetwork;

    /**
	 * Array of neurons (Neuron instances)
	 */
    protected Neuron[] neurons;

    /**
         * Label for this layer
         */
    private String label;

    /**
	 *  Creates an instance of empty Layer
	 */
    public Layer() {
        this.neurons = new Neuron[0];
    }

    /**
	 * Creates an instance of Layer with the specified number of neurons with
	 * specified neuron properties
         *
         * @param neuronsCount
         *          number of neurons in layer
         * @param neuronProperties
         *          properties of neurons in layer
	 */
    public Layer(int neuronsCount, NeuronProperties neuronProperties) {
        this();
        for (int i = 0; i < neuronsCount; i++) {
            Neuron neuron = NeuronFactory.createNeuron(neuronProperties);
            this.addNeuron(neuron);
        }
    }

    /**
	 * Sets reference on parent network
	 * 
	 * @param parent
	 *            parent network
	 */
    public void setParentNetwork(NeuralNetwork parent) {
        this.parentNetwork = parent;
    }

    /**
	 * Returns reference to parent network
	 * 
	 * @return reference on parent neural network
	 */
    public NeuralNetwork getParentNetwork() {
        return this.parentNetwork;
    }

    /**
	 * Returns array of neurons in this layer
	 * 
	 * @return array of neurons in this layer
	 */
    public final Neuron[] getNeurons() {
        return this.neurons;
    }

    /**
	 * Adds specified neuron to this layer
	 * 
	 * @param neuron
	 *            neuron to add
	 */
    public void addNeuron(Neuron neuron) {
        neuron.setParentLayer(this);
        this.neurons = Arrays.copyOf(neurons, neurons.length + 1);
        this.neurons[neurons.length - 1] = neuron;
    }

    /**
	 * Adds specified neuron to this layer,at specified index position
	 * 
	 * @param neuron
	 *            neuron to add
	 * @param idx
	 *            index position at which neuron should be added
	 */
    public void addNeuron(int idx, Neuron neuron) {
        neuron.setParentLayer(this);
        this.neurons = Arrays.copyOf(neurons, neurons.length + 1);
        for (int i = neurons.length - 1; i > idx; i++) this.neurons[i] = this.neurons[i - 1];
        this.neurons[idx] = neuron;
    }

    /**
	 * Sets (replace) the neuron at specified position in layer
	 * 
	 * @param idx
	 *            index position to set/replace
	 * @param neuron
	 *            new Neuron object to set
	 */
    public void setNeuron(int idx, Neuron neuron) {
        neuron.setParentLayer(this);
        this.neurons[idx] = neuron;
    }

    /**
	 * Removes neuron from layer
	 * 
	 * @param neuron
	 *            neuron to remove
	 */
    public void removeNeuron(Neuron neuron) {
        int idx = indexOf(neuron);
        removeNeuronAt(idx);
    }

    /**
	 * Removes neuron at specified index position in this layer
	 * 
	 * @param idx
	 *            index position of neuron to remove
	 */
    public void removeNeuronAt(int idx) {
        for (int i = idx; i < neurons.length - 1; i++) {
            neurons[i] = neurons[i + 1];
        }
        neurons[neurons.length - 1] = null;
        neurons = Arrays.copyOf(neurons, neurons.length - 1);
    }

    /**
	 * Returns neuron at specified index position in this layer
	 * 
	 * @param idx
	 *            neuron index position
	 * @return neuron at specified index position
	 */
    public Neuron getNeuronAt(int idx) {
        return this.neurons[idx];
    }

    /**
	 * Returns the index position in layer for the specified neuron
	 * 
	 * @param neuron
	 *            neuron object
	 * @return index position of specified neuron
	 */
    public int indexOf(Neuron neuron) {
        for (int i = 0; i < this.neurons.length; i++) if (neurons[i] == neuron) return i;
        return -1;
    }

    /**
	 * Returns number of neurons in this layer
	 * 
	 * @return number of neurons in this layer
	 */
    public int getNeuronsCount() {
        return neurons.length;
    }

    /**
	 * Performs calculaton for all neurons in this layer
	 */
    public void calculate() {
        for (Neuron neuron : this.neurons) {
            neuron.calculate();
        }
    }

    /**
	 * Resets the activation and input levels for all neurons in this layer
	 */
    public void reset() {
        for (Neuron neuron : this.neurons) {
            neuron.reset();
        }
    }

    /**
	 * Randomize input connection weights for all neurons in this layer
	 */
    public void randomizeWeights() {
        for (Neuron neuron : this.neurons) {
            neuron.randomizeInputWeights();
        }
    }

    /**
	 * Randomize input connection weights for all neurons in this layer
         * within specified value range
	 */
    public void randomizeWeights(double minWeight, double maxWeight) {
        for (Neuron neuron : this.neurons) {
            neuron.randomizeInputWeights(minWeight, maxWeight);
        }
    }

    /**
         * Initialize connection weights for the whole layer to to specified value
         * 
         * @param value the weight value
         */
    public void initializeWeights(double value) {
        for (Neuron neuron : this.neurons) {
            neuron.initializeWeights(value);
        }
    }

    /**
         * Initialize connection weights for the whole layer using a
         * random number generator
         *
         * @param generator the random number generator
         */
    public void initializeWeights(Random generator) {
        for (Neuron neuron : this.neurons) {
            neuron.initializeWeights(generator);
        }
    }

    public void initializeWeights(double min, double max) {
        for (Neuron neuron : this.neurons) {
            neuron.initializeWeights(min, max);
        }
    }

    /**
         * Get layer label
         * @return layer label
         */
    public String getLabel() {
        return label;
    }

    /**
         * Set layer label
         * @param label layer label to set
         */
    public void setLabel(String label) {
        this.label = label;
    }
}
