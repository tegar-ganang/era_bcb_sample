package ch.unibe.inkml;

import java.util.ArrayList;
import org.w3c.dom.Element;
import ch.unibe.inkml.InkChannel.ChannelName;

public class InkAffineMapping extends InkMapping {

    private InkMatrix matrix;

    private int targetNr;

    private int sourceNr;

    public static InkAffineMapping createIdentityInkAffinMapping(InkInk ink, InkTraceFormat sourceFormat, InkTraceFormat targetFormat) {
        InkAffineMapping map = new InkAffineMapping(ink);
        int sCount = 0;
        int tCount = 0;
        for (InkChannel c : targetFormat.getChannels()) {
            tCount++;
            InkBind bt = new InkBind();
            bt.target = c.getName();
            map.addBind(bt);
            if (sourceFormat.containsChannel(c.getName())) {
                sCount++;
                InkBind b = new InkBind();
                b.source = c.getName();
                map.addBind(b);
            }
        }
        InkMatrix m = new InkMatrix(ink);
        double[][] fm = new double[tCount][sCount];
        double[] tr = new double[tCount];
        for (InkChannel c : targetFormat.getChannels()) {
            if (sourceFormat.containsChannel(c.getName())) {
                fm[map.getTargetIndex(targetFormat, c.getName())][map.getSourcetIndex(sourceFormat, c.getName())] = 1;
            } else {
                tr[map.getTargetIndex(targetFormat, c.getName())] = Double.NaN;
            }
        }
        m.setMatrix(fm, tr);
        map.matrix = m;
        return map;
    }

    public InkAffineMapping(InkInk ink) {
        super(ink);
    }

    @Override
    public Type getType() {
        return Type.AFFINE;
    }

    public void buildFromXMLNode(Element node) throws InkMLComplianceException {
        super.buildFromXMLNode(node);
        Element matrixNode = (Element) node.getElementsByTagName(InkMatrix.INKML_NAME).item(0);
        if (matrixNode == null) {
            matrixNode = (Element) node.getElementsByTagName("matrix").item(0);
        }
        if (matrixNode == null) {
            throw new InkMLComplianceException("A mapping with @type=\"affine\" must contain an element with name \"affine\"");
        }
        matrix = new InkMatrix(this.getInk());
        matrix.buildFromXMLNode(matrixNode);
    }

    public InkMatrix getInkMatrix() {
        return this.matrix;
    }

    protected void exportToInkMLHook(Element mappingNode) throws InkMLComplianceException {
        matrix.exportToInkML(mappingNode);
    }

    private InkChannel.ChannelName[] targetChanneName;

    private InkTraceFormat cached_targetFormat;

    /**
     * @param targetFormat
     * @return
	 * @throws InkMLComplianceException 
     */
    private InkChannel.ChannelName[] getTargetNames(InkTraceFormat targetFormat) {
        if (cached_targetFormat != targetFormat) {
            cached_targetFormat = targetFormat;
            ArrayList<InkBind> l = new ArrayList<InkBind>();
            for (InkBind b : this.getBinds()) {
                if (b.hasTarget()) {
                    l.add(b);
                }
            }
            targetChanneName = new InkChannel.ChannelName[l.size()];
            for (int i = 0; i < targetChanneName.length; i++) {
                targetChanneName[i] = l.get(i).getTarget(targetFormat);
            }
        }
        return targetChanneName;
    }

    private InkChannel.ChannelName[] sourceChanneName;

    private InkTraceFormat cached_sourceFormat;

    /**
     * @param sourceFormat
     * @return
	 * @throws InkMLComplianceException 
     */
    private InkChannel.ChannelName[] getSourceNames(InkTraceFormat sourceFormat) {
        if (cached_sourceFormat != sourceFormat) {
            cached_sourceFormat = sourceFormat;
            ArrayList<InkBind> binds = new ArrayList<InkBind>();
            for (InkBind b : this.getBinds()) {
                if (b.hasSource()) {
                    binds.add(b);
                }
            }
            sourceChanneName = new InkChannel.ChannelName[binds.size()];
            for (int i = 0; i < sourceChanneName.length; i++) {
                sourceChanneName[i] = binds.get(i).getSource(sourceFormat);
            }
        }
        return sourceChanneName;
    }

    public double[][] getMatrix() {
        return matrix.getMatrix();
    }

    public double[] getTranslationVector() {
        return matrix.getTranslation();
    }

    public int getTargetD() {
        if (targetNr == 0) {
            for (InkBind b : this.getBinds()) {
                if (b.hasTarget()) {
                    targetNr++;
                }
            }
        }
        return targetNr;
    }

    public int getSourceD() {
        if (sourceNr == 0) {
            for (InkBind b : this.getBinds()) {
                if (b.hasSource()) {
                    sourceNr++;
                }
            }
        }
        return sourceNr;
    }

    @Override
    public boolean isInvertible() {
        return matrix.isInvertible();
    }

    @Override
    public void backTransform(double[][] sourcePoints, double[][] targetPoints, InkTraceFormat canvasFormat, InkTraceFormat sourceFormat) throws InkMLComplianceException {
        ChannelName[] sourceNames = getSourceNames(sourceFormat);
        int[] sourceIndices = new int[sourceNames.length];
        for (int i = 0; i < sourceNames.length; i++) {
            sourceIndices[i] = sourceFormat.indexOf(sourceNames[i]);
        }
        ChannelName[] targetNames = getTargetNames(canvasFormat);
        int[] targetIndices = new int[targetNames.length];
        for (int i = 0; i < targetNames.length; i++) {
            targetIndices[i] = canvasFormat.indexOf(targetNames[i]);
        }
        this.matrix.backtransform(sourcePoints, targetPoints, sourceIndices, targetIndices);
    }

    @Override
    public void transform(double[][] sourcePoints, double[][] targetPoints, InkTraceFormat sourceFormat, InkTraceFormat targetFormat) throws InkMLComplianceException {
        this.matrix.transform(sourcePoints, targetPoints, getSourceIndices(sourceFormat), getTargetIndices(targetFormat));
    }

    private int[] getTargetIndices(InkTraceFormat targetFormat) throws InkMLComplianceException {
        ChannelName[] targetNames = getTargetNames(targetFormat);
        int[] targetIndices = new int[targetNames.length];
        for (int i = 0; i < targetNames.length; i++) {
            targetIndices[i] = targetFormat.indexOf(targetNames[i]);
        }
        return targetIndices;
    }

    private int[] getSourceIndices(InkTraceFormat sourceFormat) throws InkMLComplianceException {
        ChannelName[] sourceNames = getSourceNames(sourceFormat);
        int[] sourceIndices = new int[sourceNames.length];
        for (int i = 0; i < sourceNames.length; i++) {
            sourceIndices[i] = sourceFormat.indexOf(sourceNames[i]);
        }
        return sourceIndices;
    }

    public int getTargetIndex(InkTraceFormat targetFormat, ChannelName name) {
        ChannelName[] targetNames = getTargetNames(targetFormat);
        for (int i = 0; i < targetNames.length; i++) {
            if (targetNames[i] == name) {
                return i;
            }
        }
        throw new Error();
    }

    public int getSourcetIndex(InkTraceFormat sourceFormat, ChannelName name) {
        ChannelName[] sourcetNames = getSourceNames(sourceFormat);
        for (int i = 0; i < sourcetNames.length; i++) {
            if (sourcetNames[i] == name) {
                return i;
            }
        }
        throw new Error();
    }

    public InkMapping clone(InkInk ink) {
        InkAffineMapping n = (InkAffineMapping) super.clone(ink);
        n.targetNr = targetNr;
        n.sourceNr = sourceNr;
        n.matrix = matrix.clone(ink);
        return n;
    }
}
