package game.neurons;

import org.w3c.dom.*;
import org.w3c.dom.Element;
import game.models.simplify.Grammar;
import game.models.ensemble.LayeredModel;
import game.trainers.Trainer;
import game.data.*;
import game.configuration.NetworkConfiguration;
import game.configuration.GlobalConfig;
import game.gui.GraphCanvas;
import game.gui.MyConfig;
import game.gui.GMDHtree;
import game.gui.Controls;
import game.utils.Utils;
import game.utils.MyRandom;
import game.utils.RMSData;
import game.utils.UnitLoader;
import java.awt.*;
import configuration.CfgBeanAllowable;

/**
 * GMDH network - the core of whole application
 */
public class GMDHnetwork implements java.io.Serializable, LayeredModel {

    transient GMDHtree tata;

    GMDHnetwork ja;

    public GMDHtree getMainClass() {
        return tata;
    }

    public void setMainClass(GMDHtree tata) {
        this.tata = tata;
    }

    public MyRandom getMyRandom() {
        return myRandom;
    }

    public void setMyRandom(MyRandom myRandom) {
        this.myRandom = myRandom;
    }

    public TreeData getMyData() {
        return myData;
    }

    public void setMyData(TreeData myData) {
        this.myData = myData;
    }

    public GraphCanvas getMyGraph() {
        return myGraph;
    }

    public void setMyGraph(GraphCanvas myGraph) {
        this.myGraph = myGraph;
    }

    transient TreeData myData;

    transient GraphCanvas myGraph;

    public NetworkConfiguration c;

    transient MyRandom myRandom;

    InputLayer iLayer;

    transient UnitLoader u;

    public int getLastLayer() {
        return lastLayer;
    }

    public void setLastLayer(int lastLayer) {
        this.lastLayer = lastLayer;
    }

    public RMSData getLayerErr() {
        return layerErr;
    }

    public void setLayerErr(RMSData layerErr) {
        this.layerErr = layerErr;
    }

    public int getActualLayer() {
        return actualLayer;
    }

    public void setActualLayer(int actualLayer) {
        this.actualLayer = actualLayer;
    }

    public int[][] getOvervarsigArray() {
        return overvarsigArray;
    }

    public int getJob() {
        return job;
    }

    public void setOvervarsigArray(int[][] overvarsigArray) {
        this.overvarsigArray = overvarsigArray;
    }

    public int[][] getOvervarsigProcArray() {
        return overvarsigProcArray;
    }

    public void setOvervarsigProcArray(int[][] overvarsigProcArray) {
        this.overvarsigProcArray = overvarsigProcArray;
    }

    public int getActualNeuron() {
        return actualNeuron;
    }

    public void setActualNeuron(int actualNeuron) {
        this.actualNeuron = actualNeuron;
    }

    public String getNrGgroupsForFR() {
        return nrGgroupsForFR;
    }

    public void setNrGgroupsForFR(String nrGgroupsForFR) {
        this.nrGgroupsForFR = nrGgroupsForFR;
    }

    RMSData layerErr;

    public boolean isNormalization() {
        return normalization;
    }

    public void setNormalization(boolean normalization) {
        this.normalization = normalization;
    }

    boolean normalization;

    private Layer[] layer = new Layer[NetworkConfiguration.MAX_LAYERS];

    int mode;

    static final int ACTIVE = 0;

    static final int PASSIVE = 1;

    static final int MAX_RAN = TreeData.MAX_INSTANCES;

    int job;

    static final int NONE = 0;

    public static final int NEW_NEURON = 1;

    public static final int ERROR_COMPUTING = 2;

    public static final int NEURONS_SORTING = 3;

    public static final int ENTERING_PASSIVE_MODE = 4;

    public static final int INPUT_NEURONS_SELECTION = 5;

    public static final int BUILDING_MEMORY_STRUCTURE = 6;

    private static final int ALAYER = 0;

    private static final int INPUT_LAYER = 1;

    private static final int GMDH_LAYER = 2;

    static final int GAME_LAYER = 3;

    transient double[][] ivector;

    transient double[][] oattr;

    transient double[] crit;

    double[] inputVector;

    int iNum;

    private int oNum;

    int group;

    int netInputs;

    int lastLayer;

    int maxNeuronsInLayer;

    int actualLayer;

    int actualNeuron;

    private String name;

    int[][] overvarsigArray;

    int[][] overvarsigProcArray;

    private String nrGgroupsForFR;

    public Neuron getOutputNeuron() {
        while (layer[lastLayer] == null && (lastLayer > 0)) {
            lastLayer--;
        }
        if (layer[lastLayer] == null) {
            return null;
        }
        return layer[lastLayer].n[0];
    }

    public int getLayersNumber() {
        return lastLayer + 1;
    }

    public Neuron[] getLayer(int index) {
        if (layer == null) return null;
        if (layer[index] == null) return null;
        return layer[index].n;
    }

    public ANeuron[] getILayer() {
        return iLayer.n;
    }

    public int getNeuronLayerNumber(Neuron n) {
        return 0;
    }

    public Dimension getNeuronPosition(Neuron n) {
        return null;
    }

    public boolean inPassiveMode() {
        return mode == PASSIVE;
    }

    public void toPassiveMode() {
        mode = PASSIVE;
    }

    public void toActiveMode() {
        mode = ACTIVE;
    }

    public double getOutput(double[] input_vector) {
        return getResponse(input_vector);
    }

    public double getNormalizedOutput(double[] normalized_input_vector) {
        return getStandardResponse(normalized_input_vector);
    }

    public int getTargetVariable() {
        return c.getCriterion();
    }

    public void setTargetVariable(int targetVariable) {
        c.setCriterion(targetVariable);
    }

    public String toEquation(String[] inputEquation) {
        return null;
    }

    @Override
    public Class getConfigClass() {
        return null;
    }

    /**
     * class ALayer represents an abstract layer of the network
     */
    public abstract class ALayer implements java.io.Serializable {

        int number;

        int inumber;

        public int getNumber() {
            return number;
        }

        public int getINumber() {
            return inumber;
        }

        public ANeuron[] getRandomNeurons(int num) {
            return null;
        }

        public ANeuron getNeuron(int num) {
            return null;
        }

        public ALayer getPreviousLayer() {
            return null;
        }

        public int getInputsNumber() {
            return 0;
        }

        public void computeOutputs() {
        }

        public int getType() {
            return ALAYER;
        }
    }

    /**
     * class InputLayer represents a layer of input neurons in the network
     */
    public class InputLayer extends ALayer implements java.io.Serializable {

        INeuron[] n = new INeuron[netInputs];

        int actVector = 0;

        public InputLayer(boolean withoutInstances) {
        }

        public InputLayer() {
            for (int i = 0; i < netInputs; i++) {
                n[i] = new INeuron(this, i);
            }
            number = netInputs;
            inumber = number;
        }

        /**
         * This constructor creates new Input Layer using element of the PMML document.
         *
         * @param e is <NeuralInputs> element
         * @author Vit Ulicny
         */
        public InputLayer(Element e) {
            NodeList nodeListInputs = e.getElementsByTagName("NeuralInput");
            iNum = netInputs;
            for (int i = 0; i < netInputs; i++) {
                n[i] = new INeuron(this, i);
            }
            number = netInputs;
            inumber = number;
        }

        /**
         * INeuron is a neuron of first network layer
         */
        public class INeuron extends ANeuron implements java.io.Serializable {

            GMDHnetwork.InputLayer myLay;

            int myFactor;

            public INeuron(GMDHnetwork.InputLayer myLayer, int iFactor) {
                myFactor = iFactor;
                myLay = myLayer;
                inputs = 1;
                setId(iFactor);
            }

            public double getOutputValue() {
                outValue = myLay.getActualVectorValue(myFactor);
                return outValue;
            }

            public void setValue() {
                outValue = myLay.getActualVectorValue(myFactor);
            }

            public ALayer getLayer() {
                return myLay;
            }

            /**
             * returns the transfer function of the unit in the text form
             */
            public String toEquation() {
                InputFactor ipf = ((InputFactor) GlobalData.getInstance().iFactor.elementAt(myFactor));
                String s = ipf.getName();
                if (normalization) {
                    double range = ipf.getMax() - ipf.getMin();
                    if (ipf.getMin() > 0) s = "(" + s + "-" + Utils.convertDouble(ipf.getMin()) + ")/" + Utils.convertDouble(range); else s = "(" + s + "+" + Utils.convertDouble(-ipf.getMin()) + ")/" + Utils.convertDouble(range);
                }
                return s;
            }

            public String toSimplifiedEquation() {
                return toEquation();
            }
        }

        public double getActualVectorValue(int factor) {
            return (actVector != group ? ivector[actVector][factor] : inputVector[factor]);
        }

        public void setActualVector(int num) {
            actVector = num;
        }

        public ANeuron[] getRandomNeurons(int num) {
            INeuron[] ret = new INeuron[num];
            MyRandom mr = new MyRandom(getNumber());
            mr.resetRandom();
            for (int i = 0; i < num; i++) {
                ret[i] = n[mr.getRandom(getNumber())];
            }
            return ret;
        }

        public ANeuron getNeuron(int num) {
            if (num < getNumber()) {
                return n[num];
            } else {
                return null;
            }
        }

        public int getInputsNumber() {
            return 1;
        }

        public void computeOutputs() {
            for (int i = 0; i < number; i++) {
                n[i].getOutputValue();
            }
        }

        public int getType() {
            return INPUT_LAYER;
        }
    }

    /**
     * class Layer represents a layer of network
     */
    public class Layer extends ALayer implements java.io.Serializable {

        int inputsToNeuron;

        Neuron[] n = new Neuron[maxNeuronsInLayer];

        Neuron berle;

        ALayer prevLayer;

        int layerProgress;

        int index;

        public Layer() {
        }

        /**
         * This constructor is called when network is loaded from PMML document
         *
         * @param e is the <NeuralLayer> element from the PMML document
         * @author Vit Ulicny
         */
        public Layer(Element e) {
            String typeOfFunction;
            NodeList nodeListOfNeurons = e.getElementsByTagName("Neuron");
            int numberOfNeurons = nodeListOfNeurons.getLength();
            number = inumber = numberOfNeurons;
            Neuron[] neurons = new Neuron[numberOfNeurons];
            for (int i = 0; i < numberOfNeurons; i++) {
                typeOfFunction = ((org.w3c.dom.Element) nodeListOfNeurons.item(i)).getAttribute("type");
                Neuron neuron = new Neuron();
                if (typeOfFunction.equals("Sigmoid")) {
                    neuron = new SigmNeuron();
                } else if (typeOfFunction.equals("Linear")) {
                    neuron = new LinearNeuron();
                } else if (typeOfFunction.equals("MultiGaussian")) {
                    neuron = new MultiGaussianNeuron();
                } else if (typeOfFunction.equals("Rational")) {
                    neuron = new PolyFractNeuron();
                } else if (typeOfFunction.equals("Polynomial")) {
                    neuron = new PolySimpleNeuron();
                } else if (typeOfFunction.equals("Polynomial - NR")) {
                    neuron = new PolySimpleNRNeuron();
                } else if (typeOfFunction.equals("Sine")) {
                    neuron = new SinusNeuron();
                } else if (typeOfFunction.equals("Polynomial - combi")) {
                    neuron = new CombiNeuron();
                } else if (typeOfFunction.equals("Exponential")) {
                    neuron = new ExpNeuron();
                } else if (typeOfFunction.equals("Gaussian")) {
                    neuron = new GaussianNeuron();
                } else if (typeOfFunction.equals("Gauss")) {
                    neuron = new GaussNeuron();
                }
                neurons[i] = neuron;
            }
            this.n = neurons;
        }

        /**
         * construct a GMDH network layer
         * @param num
         * @param pl
         * @param inputs_per_neuron
         */
        public Layer(int num, ALayer pl, int inputs_per_neuron) {
            prevLayer = pl;
            index = num;
            ANeuron[] copyN;
            ANeuron[] inputsChoosen = new ANeuron[inputs_per_neuron];
            prevLayer = pl;
            ALayer aLayer;
            ANeuron choosen;
            Neuron berle = new Neuron();
            layerProgress = 0;
            boolean tryAgain;
            inputsToNeuron = inputs_per_neuron;
            number = c.getLayerNeuronsNumber(num);
            inumber = c.getLayerInitialNeuronsNumber(num);
            if (number > inumber) {
                number = inumber;
            }
            int actInput, golayer, which, s;
            berle.init(ja, inputsChoosen, inputs_per_neuron);
            mode = PASSIVE;
            prevLayer.computeOutputs();
            for (int i = 0; i < inumber; i++) {
                copyN = prevLayer.getRandomNeurons(2);
                inputsChoosen[0] = copyN[0];
                inputsChoosen[1] = copyN[1];
                actInput = 2;
                while (actInput < inputs_per_neuron) {
                    do {
                        aLayer = prevLayer;
                        switch(c.getParents()) {
                            case NetworkConfiguration.YOUNG:
                                if (actInput >= aLayer.getNumber()) {
                                    aLayer = aLayer.getPreviousLayer();
                                }
                                break;
                            case NetworkConfiguration.YOUNGER:
                                for (int j = s = 0; j < num + 1; j++) {
                                    s += (j + 1);
                                }
                                s = myRandom.nextInt(s + 1) - num - 1;
                                for (int j = num; s > 0; s -= j--) {
                                    if (aLayer != null) {
                                        aLayer = aLayer.getPreviousLayer();
                                    }
                                }
                                break;
                            case NetworkConfiguration.MIDDLE:
                                s = myRandom.nextInt(num + 1);
                                for (int j = 0; j < s; j++) {
                                    if (aLayer != null) {
                                        aLayer = aLayer.getPreviousLayer();
                                    }
                                }
                                break;
                            case NetworkConfiguration.OLDER:
                                for (int j = s = 0; j < num + 1; j++) {
                                    s += (j + 1);
                                }
                                s = myRandom.nextInt(s + 1) - num - 1;
                                for (int j = num; s > 0; s -= (num - (--j))) {
                                    if (aLayer != null) {
                                        aLayer = aLayer.getPreviousLayer();
                                    }
                                }
                                break;
                            case NetworkConfiguration.OLD:
                                if (actInput >= aLayer.getNumber()) {
                                    while ((aLayer.getPreviousLayer() != null) && (aLayer.getPreviousLayer().getPreviousLayer() != null)) {
                                        aLayer = aLayer.getPreviousLayer();
                                    }
                                } else {
                                    while ((aLayer != null) && (aLayer.getPreviousLayer() != null)) {
                                        aLayer = aLayer.getPreviousLayer();
                                    }
                                }
                                break;
                        }
                        choosen = aLayer.getNeuron(myRandom.nextInt(aLayer.getNumber()));
                        tryAgain = false;
                        for (int j = 0; j < actInput; j++) {
                            if (inputsChoosen[j].equals(choosen)) {
                                tryAgain = true;
                            }
                        }
                    } while (tryAgain);
                    inputsChoosen[actInput] = choosen;
                    actInput++;
                }
                do {
                    which = myRandom.nextInt(u.getNeuronsNumber());
                } while (!c.neuronTypeAllowed(which));
                int train;
                do {
                    train = myRandom.nextInt(u.getTrainersNumber());
                } while (!GlobalConfig.getInstance().getGac().neuronTrainerAllowed(train));
                try {
                    n[i] = (Neuron) u.getNeuronClass(which).newInstance();
                    n[i].init(ja, inputsChoosen, inputs_per_neuron, u.getNeuronConfig(which));
                    Trainer tt = (Trainer) u.getTrainerClass(train).newInstance();
                    tt.init(n[i], tt, n[i].getCoefsNumber());
                    n[i].setTrainer(tt);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    i--;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    i--;
                }
            }
            mode = ACTIVE;
        }

        public ANeuron[] getRandomNeurons(int num) {
            ANeuron[] ret = new ANeuron[num];
            myRandom.resetRandom();
            for (int i = 0; i < num; i++) {
                ret[i] = n[myRandom.getRandom(getNumber())];
            }
            return ret;
        }

        public ANeuron getNeuron(int num) {
            if (num < getNumber()) {
                return n[num];
            } else {
                return null;
            }
        }

        public ALayer getPreviousLayer() {
            return prevLayer;
        }

        public int getInputsNumber() {
            return inputsToNeuron;
        }

        public int getLayerIndex() {
            return index;
        }

        public double getBestOutput(int vector) {
            double ret = 0, div = 0, num = getNumber();
            iLayer.setActualVector(vector);
            mode = PASSIVE;
            prevLayer.computeOutputs();
            if (GlobalConfig.getInstance().getGc().isCommonResponse()) {
                for (int i = 0; i < num; i++) {
                    ret += (num - i) * (num - i) * n[i].getOutputValue();
                    div += (num - i) * (num - i);
                }
                mode = ACTIVE;
                return ret / div;
            }
            ret = n[0].getOutputValue();
            mode = ACTIVE;
            return ret;
        }

        /**
         * teaching of one layer of the GMDH network
         * @param iLayer
         */
        public double teachLayer(InputLayer iLayer) {
            double squareError, error;
            Neuron swap;
            int lastIndex;
            boolean anyChange;
            int vTest = (int) (group * (c.getVectorsInTestingSet() / 100.0));
            int rVector;
            job = ENTERING_PASSIVE_MODE;
            mode = PASSIVE;
            myRandom.resetRandom();
            for (int i = 0; i < group - vTest; i++) {
                rVector = myRandom.getRandomLearningVector();
                iLayer.setActualVector(rVector);
                layerProgress = (int) (100.0 * i / (double) (group - vTest - 1));
                if (layerProgress % 100 == 1) {
                    myGraph.redraw();
                }
                prevLayer.computeOutputs();
                for (int act = 0; act < inumber; act++) {
                    n[act].storeInputValue(rVector);
                }
            }
            myGraph.redraw();
            mode = ACTIVE;
            job = NEW_NEURON;
            for (int act = 0; act < inumber; act++) {
                actualNeuron = act;
                n[act].learnYourself((Unit) n[act]);
            }
            job = ERROR_COMPUTING;
            myRandom.resetRandom();
            for (int i = 0; i < group; i++) {
                rVector = 0;
                if (i < group - vTest) {
                    rVector = myRandom.getRandomLearningVector();
                }
                if (i == group - vTest) {
                    myRandom.resetRandom();
                }
                if (i >= group - vTest) {
                    rVector = myRandom.getRandomTestingVector();
                }
                iLayer.setActualVector(rVector);
                mode = PASSIVE;
                prevLayer.computeOutputs();
                for (int act = 0; act < inumber; act++) {
                    error = n[act].getError(rVector);
                    n[act].setSquareError(n[act].getSquareError() + error * error);
                    layerProgress = (int) (100.0 * (i + (double) act / (double) (inumber - 1)) / (double) (group));
                }
                if (layerProgress % 100 == 1) {
                    myGraph.redraw();
                }
                mode = ACTIVE;
            }
            myGraph.redraw();
            job = NEURONS_SORTING;
            lastIndex = inumber - 1;
            do {
                anyChange = false;
                for (int i = 0; i < lastIndex; i++) {
                    if (n[i].getSquareError() > n[i + 1].getSquareError()) {
                        anyChange = true;
                        swap = n[i];
                        n[i] = n[i + 1];
                        n[i + 1] = swap;
                    }
                }
                lastIndex--;
                layerProgress = 100 * (inumber - lastIndex - 1) / (inumber - 1);
                if (layerProgress % 100 == 1) {
                    myGraph.redraw();
                }
            } while (anyChange);
            layerProgress = 100;
            myGraph.redraw();
            job = NONE;
            for (int i = number; i < inumber; i++) {
                n[i] = null;
            }
            if (c.isCommonResponse()) {
                double ret = 0, div = 0;
                for (int i = 0; i < number; i++) {
                    ret += (number - i) * (number - i) * n[i].getSquareError();
                    div += (number - i) * (number - i);
                }
                return ret / div;
            }
            return n[0].getSquareError();
        }

        public int getLayerProgress() {
            return layerProgress;
        }

        public void computeOutputs() {
            prevLayer.computeOutputs();
            for (int i = 0; i < number; i++) {
                n[i].getOutputValue();
            }
        }

        public int getType() {
            return GMDH_LAYER;
        }
    }

    /**
     * Inicializes the network
     *
     * @param iname the name of the network usually corresponds to the name of the output attribute
     * @param boss  the parent application
     * @param info  input output data and many more functions
     * @param gr    output graph
     * @param conf  the configuration of this network
     */
    public GMDHnetwork(String iname, GMDHtree boss, TreeData info, GraphCanvas gr, NetworkConfiguration conf) {
        tata = boss;
        ja = this;
        name = iname;
        myData = info;
        c = conf;
        myGraph = gr;
        layerErr = new RMSData(0.0001, 40, 0, NetworkConfiguration.MAX_LAYERS);
        u = UnitLoader.getInstance();
        normalization = c.isNormalization();
        initializeValues();
    }

    /**
     * ...., normalizing, copying data vectors
     */
    void initializeValues() {
        double critMax = Double.MIN_VALUE;
        double critMin = Double.MAX_VALUE;
        double hlp;
        myData = GlobalData.getInstance();
        oNum = myData.getONumber();
        iNum = myData.getINumber();
        netInputs = iNum;
        group = GlobalData.getInstance().getInstNumber();
        inputVector = new double[iNum];
        ivector = new double[group][iNum];
        oattr = new double[group][oNum];
        crit = new double[group];
        job = NONE;
        myRandom = new MyRandom(group);
        for (int j = 0; j < group; j++) {
            for (int i = 0; i < iNum; i++) {
                if (normalization) {
                    ivector[j][i] = ((Instance) myData.group.elementAt(j)).getStiValue(i);
                } else {
                    ivector[j][i] = ((Instance) myData.group.elementAt(j)).getiVal(i);
                }
            }
        }
        for (int j = 0; j < group; j++) {
            for (int i = 0; i < oNum; i++) {
                if (normalization) {
                    oattr[j][i] = ((Instance) myData.group.elementAt(j)).getStoValue(i);
                } else {
                    oattr[j][i] = ((Instance) myData.group.elementAt(j)).getoVal(i);
                }
            }
        }
        if (c.getCriterion() < oNum) {
            for (int j = 0; j < group; j++) {
                crit[j] = oattr[j][c.getCriterion()];
            }
        } else {
            for (int j = 0; j < group; j++) {
                hlp = getCriterionValue(j);
                if (hlp > critMax) {
                    critMax = hlp;
                }
                if (hlp < critMin) {
                    critMin = hlp;
                }
            }
            for (int j = 0; j < group; j++) {
                crit[j] = ((getCriterionValue(j) - critMin) / (critMax - critMin));
            }
        }
    }

    /**
     * returns the number of the output attribute this network is modelling
     * @param column
     */
    double getCriterionValue(int column) {
        int crit = c.getCriterion();
        double sign;
        if (crit < oNum) {
            return oattr[column][crit];
        } else {
            double r = 0;
            for (int i = 0; i < oNum; i++) {
                if (c.getInCrit()[i]) {
                    sign = ((OutputAttribute) GlobalData.getInstance().oAttr.elementAt(i)).getStandardSign();
                    r += (oattr[column][i] * sign * (((OutputAttribute) GlobalData.getInstance().oAttr.elementAt(i)).isPositive() ? 1.0 : -1.0));
                }
            }
            return r;
        }
    }

    /**
     * generates the GMDH network (learning process)
     */
    public void constructNetwork() {
        double layerError, firstLayerErr;
        iLayer = new InputLayer();
        double sqerr;
        int i = 0;
        layerErr.reset();
        myRandom.generateLearningAndTestingSet((int) (group * (c.getVectorsInTestingSet() / 100.0)));
        maxNeuronsInLayer = c.getLayerInitialNeuronsNumber(i);
        actualLayer = i;
        layerError = firstLayerErr = Double.MAX_VALUE;
        GraphCanvas.getInstance().setLayerConstruction(true);
        if (c.isJustTwo()) {
            layer[i] = new Layer(i, iLayer, 2);
        } else {
            layer[i] = new Layer(i, iLayer, i + 2);
        }
        while ((sqerr = layer[i].teachLayer(iLayer)) < layerError) {
            if (i == 0) {
                firstLayerErr = sqerr;
            }
            if (i > 1) {
                if (((layerError - sqerr) * 30.0) < (firstLayerErr - layerError)) {
                    if (!c.isBuildWhileDec()) {
                        actualLayer = ++i;
                        layerError = sqerr;
                        layerErr.add(sqerr);
                        break;
                    }
                }
            }
            actualLayer = ++i;
            layerError = sqerr;
            layerErr.add(sqerr);
            if (i > NetworkConfiguration.MAX_LAYERS - 2) {
                break;
            }
            maxNeuronsInLayer = c.getLayerInitialNeuronsNumber(i);
            if (c.isJustTwo()) {
                layer[i] = new Layer(i, layer[i - 1], 2);
            } else {
                layer[i] = new Layer(i, layer[i - 1], i + 2);
            }
        }
        layerErr.add(sqerr);
        lastLayer = i - 1;
        ivector = oattr = null;
        crit = null;
        GraphCanvas.getInstance().setLayerConstruction(false);
    }

    /**
     * returns name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTrainedBy() {
        return "LMS method";
    }

    public void setTrainedBy(String trainerName) {
    }

    /**
     * returns currently processed unit
     */
    public Neuron getProcessedNeuron() {
        return (layer[actualLayer].n[actualNeuron]);
    }

    /**
     * returns currently processed layer
     */
    public Layer getProcessedLayer() {
        return (layer[actualLayer]);
    }

    /**
     * to refresh memory and get the input-output data when deserializing
     */
    public void restore() {
        ja = this;
        oNum = myData.getONumber();
        iNum = myData.getINumber();
        netInputs = iNum;
    }

    /**
     * returns the response of the network normalized to <0,1>
     *
     * @param vector input vector should be given in normalized form to <0,1>
     */
    double getStandardResponse(double[] vector) {
        double[] vinput = new double[iNum];
        double voutput = Double.NaN;
        if (lastLayer < 0) {
            return voutput;
        }
        if (!normalization) {
            for (int i = 0; i < iNum; i++) {
                vector[i] = ((InputFactor) GlobalData.getInstance().iFactor.elementAt(i)).decodeStandardValue(vector[i]);
            }
        } else {
        }
        System.arraycopy(vector, 0, inputVector, 0, iNum);
        voutput = layer[lastLayer].getBestOutput(group);
        if (!normalization) {
            return ((OutputAttribute) GlobalData.getInstance().oAttr.elementAt(c.getCriterion())).getStandardValue(voutput);
        } else return voutput;
    }

    /**
     * returns the response of the network
     *
     * @param vector input vector
     * @return double
     */
    double getResponse(double[] vector) {
        double[] vinput = new double[iNum];
        double voutput = Double.NaN;
        if (lastLayer < 0) {
            return voutput;
        }
        System.arraycopy(vector, 0, vinput, 0, iNum);
        if (normalization) {
            for (int i = 0; i < iNum; i++) {
                vinput[i] = ((InputFactor) GlobalData.getInstance().iFactor.elementAt(i)).getStandardValue(vector[i]);
            }
        }
        System.arraycopy(vinput, 0, inputVector, 0, iNum);
        voutput = layer[lastLayer].getBestOutput(group);
        if (normalization) return ((OutputAttribute) GlobalData.getInstance().oAttr.elementAt(c.getCriterion())).decodeStandardValue(voutput);
        return voutput;
    }

    /**
     * toEquation
     *
     * @param simplify
     * @return String
     */
    public String toEquation(boolean simplify) {
        while (layer[lastLayer] == null && (lastLayer > 0)) {
            lastLayer--;
        }
        if (layer[lastLayer] == null) {
            return "null";
        }
        String s;
        if (simplify) s = layer[lastLayer].n[0].toSimplifiedEquation(); else s = layer[lastLayer].n[0].toEquation();
        if (normalization) {
            OutputAttribute oatr = ((OutputAttribute) GlobalData.getInstance().oAttr.elementAt(c.getCriterion()));
            double range = oatr.getMax() - oatr.getMin();
            if (oatr.getMin() >= 0) s = "(" + s + ")*" + range + "+" + oatr.getMin(); else s = "(" + s + ")*" + range + " " + oatr.getMin();
        }
        if (simplify) {
            Grammar eq = new Grammar(s);
            s = eq.simplify();
        }
        return s;
    }

    /**
     * getSquareError
     *
     * @return double error of the best neuron
     */
    public double getSquareError() {
        if (layer[lastLayer] != null) if (layer[lastLayer].n[0] != null) return layer[lastLayer].n[0].getSquareError();
        return Double.NaN;
    }

    /**
     * Return neuron with ID.
     *
     * @param id is the ID of the neuron.
     * @return ANeuron
     * @author Vit Ulicny
     */
    public ANeuron getNeuron(int id) {
        int i = 0;
        while (i < this.netInputs) {
            if (id == this.iLayer.n[i].getId()) {
                return this.iLayer.n[i];
            }
            i++;
        }
        i = 0;
        int j = 0;
        int d = this.layer.length;
        while (i < d) {
            while (j < this.layer[i].n.length) {
                if (id == this.layer[i].n[j].getId()) {
                    return this.layer[i].n[j];
                }
                j++;
            }
            i++;
        }
        return null;
    }

    /**
     * Return new layer created by using PMML element "NeuralLayer".
     *
     * @param e is the "NeuralNetwork" element.
     * @return new layer.
     * @author Vit Ulicny
     */
    public Layer getNewLayer(org.w3c.dom.Element e) {
        Layer layer = new Layer(e);
        for (int i = 0; i < layer.n.length; i++) {
            layer.n[i].myNet = this;
        }
        return layer;
    }

    /**
     * Return new input layer created by using PMML element "NeuralInputs".
     *
     * @param e              is the "NeuralInputs" element.
     * @param numberOfInputs is the number of netwrok's inputs.
     * @return new input layer.
     * @author Vit Ulicny
     */
    public InputLayer getNewiLayer(Element e, int numberOfInputs) {
        netInputs = numberOfInputs;
        return new InputLayer(e);
    }

    /**
     * this function is mainly used for locating input units when visualising the GMDH network structure
     */
    public java.awt.Dimension getNeuronParentPosition(Neuron n, int inputNumber) {
        if (n.f[inputNumber] == null) return null;
        int layerDistance = 1;
        int neuronIndex = 0;
        GMDHnetwork.ALayer find;
        if (actualLayer < 1) {
            find = iLayer;
            while ((find.getNeuron(neuronIndex) != n.f[inputNumber]) && (neuronIndex < find.getINumber())) neuronIndex++;
            if (neuronIndex < find.getINumber()) return new java.awt.Dimension(neuronIndex, layerDistance); else return null;
        }
        find = layer[actualLayer - 1];
        if (find == null) return null;
        boolean notFound = true;
        while (notFound) {
            neuronIndex = 0;
            while ((find.getNeuron(neuronIndex) != n.f[inputNumber]) && (neuronIndex < find.getINumber())) neuronIndex++;
            if (neuronIndex < find.getINumber()) {
                notFound = false;
            } else {
                find = find.getPreviousLayer();
                layerDistance++;
                if (find == null) break;
            }
        }
        if (notFound) return null;
        return new java.awt.Dimension(neuronIndex, layerDistance);
    }
}
