package qbts.preprocessing.discretization.rm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;

public class NormalizationExtended extends Operator {

    /** The parameter name for &quot;Determines whether to perform a normalization (minimum 0 and maximum 1) or not to the values of the attributes for each example &quot; */
    public static final String PARAMETER_NORMALIZATION_SERIES = "normalization";

    /** The parameter name for &quot;Determines whether to perform a z-transformation (mean 0 and variance 1) or not to the values of the attributes for each example&quot; */
    public static final String PARAMETER_TYPIFICATION_SERIES = "typification";

    /** The parameter name for &quot;Determines whether to perform a difference between the values of consecutive attributes or not; if set the last attribute is remove&quot; */
    public static final String PARAMETER_DIFFERENCE_SERIES = "difference";

    private static final Class[] INPUT_CLASSES = { ExampleSet.class };

    private static final Class[] OUTPUT_CLASSES = { ExampleSet.class };

    public NormalizationExtended(OperatorDescription description) {
        super(description);
    }

    public IOObject[] apply() throws OperatorException {
        ExampleSet exampleSet = getInput(ExampleSet.class);
        List<Attribute> lAtt = new ArrayList<Attribute>();
        Iterator<AttributeRole> r = exampleSet.getAttributes().regularAttributes();
        while (r.hasNext()) {
            AttributeRole role = r.next();
            Attribute att = role.getAttribute();
            lAtt.add(att);
        }
        if (getParameterAsBoolean(PARAMETER_TYPIFICATION_SERIES) || getParameterAsBoolean(PARAMETER_NORMALIZATION_SERIES) || getParameterAsBoolean(PARAMETER_DIFFERENCE_SERIES)) {
            for (Example ex : exampleSet) {
                double[] valores = new double[lAtt.size()];
                int i = 0;
                for (Attribute att : lAtt) {
                    valores[i++] = ex.getValue(att);
                }
                double[] val2 = Preprocess_Series(valores, true, getParameterAsBoolean(PARAMETER_NORMALIZATION_SERIES), getParameterAsBoolean(PARAMETER_TYPIFICATION_SERIES), getParameterAsBoolean(PARAMETER_DIFFERENCE_SERIES));
                i = 0;
                for (Attribute att : lAtt) {
                    if (i < val2.length) ex.setValue(att, val2[i++]);
                }
            }
            if (getParameterAsBoolean(PARAMETER_DIFFERENCE_SERIES)) {
                if (lAtt.get(lAtt.size() - 1).getBlockType() == Ontology.VALUE_SERIES_END) {
                    lAtt.get(lAtt.size() - 2).setBlockType(Ontology.VALUE_SERIES_END);
                    exampleSet.getAttributes().remove(lAtt.get(lAtt.size() - 1));
                }
            }
        }
        return new IOObject[] { exampleSet };
    }

    public static final double[] Preprocess_Series(double[] v, boolean seriesFixedLength, boolean Nor, boolean Tip, boolean Dif) {
        int longitud = v.length;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double total = 0;
        for (int pos = 0; pos < longitud; pos++) {
            if (v[pos] < min) min = v[pos];
            if (v[pos] > max) max = v[pos];
            total += v[pos];
        }
        double amplitud = max - min;
        double media = total / longitud;
        double suma = 0;
        if (Nor) {
            for (int i = 0; i < longitud; i++) v[i] = (v[i] - min) / amplitud;
        } else if (Tip) {
            for (int i = 0; i < longitud; i++) suma += Math.pow(v[i] - media, 2);
            if (suma > 0) {
                double desv = Math.sqrt(suma / (longitud - 1));
                for (int i = 0; i < longitud; i++) v[i] = (v[i] - media) / desv;
            }
        }
        if (Dif) longitud--;
        double[] aux = new double[longitud];
        if (Dif) for (int i = 0; i < longitud; i++) aux[i] = v[i + 1] - v[i]; else for (int i = 0; i < longitud; i++) aux[i] = v[i];
        return aux;
    }

    public Class<?>[] getInputClasses() {
        return INPUT_CLASSES;
    }

    public Class<?>[] getOutputClasses() {
        return OUTPUT_CLASSES;
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        ParameterType type = new ParameterTypeBoolean(PARAMETER_NORMALIZATION_SERIES, "Preprocess Series. [0,1] Normalization", false);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeBoolean(PARAMETER_TYPIFICATION_SERIES, "Preprocess Series. Typification.", true);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeBoolean(PARAMETER_DIFFERENCE_SERIES, "Preprocess Series. Difference of adjacent values", false);
        type.setExpert(false);
        types.add(type);
        type = new ParameterTypeBoolean("disable_exampleset_output", "Disable the output of a Discretized version of the input ExampleSet.", true);
        type.setExpert(false);
        types.add(type);
        return types;
    }
}
