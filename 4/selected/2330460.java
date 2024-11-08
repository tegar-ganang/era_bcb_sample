package de.grogra.gpuflux.scene;

import java.util.Stack;
import java.util.Vector;
import javax.vecmath.Matrix4d;
import de.grogra.gpuflux.scene.filter.ObjectFilter;
import de.grogra.gpuflux.scene.light.FluxLightBuilder;
import de.grogra.gpuflux.scene.shading.FluxShader;
import de.grogra.gpuflux.scene.shading.FluxShaderBuilder;
import de.grogra.gpuflux.scene.shading.channel.FluxChannelMap;
import de.grogra.gpuflux.scene.volume.FluxPrimitive;
import de.grogra.gpuflux.scene.volume.FluxSensor;
import de.grogra.gpuflux.scene.volume.FluxVolumeBuilder;
import de.grogra.graph.ArrayPath;
import de.grogra.graph.Graph;
import de.grogra.graph.GraphState;
import de.grogra.graph.Instantiator;
import de.grogra.graph.Path;
import de.grogra.graph.impl.Node;
import de.grogra.imp3d.DisplayVisitor;
import de.grogra.imp3d.IMP3D;
import de.grogra.imp3d.Renderable;
import de.grogra.imp3d.ViewConfig3D;
import de.grogra.imp3d.Visitor3D;
import de.grogra.imp3d.objects.Attributes;
import de.grogra.imp3d.objects.LightNode;
import de.grogra.imp3d.objects.SensorNode;
import de.grogra.imp3d.objects.Sky;
import de.grogra.imp3d.shading.AlgorithmSwitchShader;
import de.grogra.imp3d.shading.Shader;
import de.grogra.imp3d.shading.ShaderRef;
import de.grogra.math.Pool;
import de.grogra.ray.physics.Interior;
import de.grogra.ray.physics.Spectrum3d;
import de.grogra.ray2.ProgressMonitor;
import de.grogra.vecmath.geom.SensorDisc;
import de.grogra.vecmath.geom.Variables;
import de.grogra.xl.util.LongToIntHashMap;
import de.grogra.xl.util.ObjectList;

public class FluxSceneVisitor extends DisplayVisitor {

    private ProgressMonitor monitor;

    private ObjectFilter measureFilter;

    private Graph graph;

    private boolean enableSensors;

    private String log = new String();

    /**
	 * Map from node ids to group indices.
	 */
    private transient LongToIntHashMap nodeToGroup;

    /**
	 * Grouping counter. If positive, we are within a group.
	 */
    private transient int grouping;

    /**
	 * Next unused group index. Note that we have to start with 1 so that
	 * group index 0 is never used. If nodeToGroup.get(id) returns 0, we know
	 * that there is no group associated with id.
	 */
    private transient int nextGroupIndex;

    /**
	 * Group index to use within {@link #volumeCreated}. 
	 */
    private transient int currentGroupIndex;

    private Renderable currentRenderable = null;

    private Shader currentShader = null;

    private float currentIOR = 1.f;

    private Object currentObject = null;

    private Interior currentInterior = null;

    private long nextProgressTime = 0;

    private FluxLightBuilder lightBuilder = null;

    private FluxVolumeBuilder volumeBuilder = null;

    private FluxShaderBuilder shaderBuilder = null;

    private ObjectList<Interior> interiorStack = new ObjectList<Interior>();

    private Vector<FluxPrimitive> primitives = new Vector<FluxPrimitive>();

    private Vector<FluxPrimitive> infPrimitives = new Vector<FluxPrimitive>();

    private Vector<LightNode> lights = new Vector<LightNode>();

    private Vector<FluxSensor> sensors = new Vector<FluxSensor>();

    private boolean[] visibleLayers;

    public Vector<FluxPrimitive> getPrimitives() {
        return primitives;
    }

    public Vector<FluxSensor> getSensors() {
        return sensors;
    }

    public Vector<LightNode> getLights() {
        return lights;
    }

    public Vector<FluxPrimitive> getInfPrimitives() {
        return infPrimitives;
    }

    public LongToIntHashMap getNodeToGroup() {
        return nodeToGroup;
    }

    protected Shader resolveShader(Shader shader) {
        return (shader instanceof AlgorithmSwitchShader) ? ((AlgorithmSwitchShader) shader).getRaytracerShader() : (shader instanceof ShaderRef) ? ((ShaderRef) shader).resolve() : shader;
    }

    @Override
    protected boolean isInVisibleLayer(Object o, boolean asNode) {
        if (visibleLayers == null) {
            return super.isInVisibleLayer(o, asNode);
        }
        int layer = state.getIntDefault(o, asNode, Attributes.LAYER, 0);
        return (layer < 0) || (layer >= visibleLayers.length) || visibleLayers[layer];
    }

    public void visitScene(Graph graph, ViewConfig3D view, ObjectFilter measureFilter, boolean enableSensors, boolean sampleExplicit, ProgressMonitor monitor, float flatness, boolean[] visibleLayers) {
        this.measureFilter = measureFilter;
        this.graph = graph;
        this.visibleLayers = visibleLayers;
        this.monitor = monitor;
        this.enableSensors = enableSensors;
        Matrix4d m = new Matrix4d();
        m.setIdentity();
        init(GraphState.current(graph), m, view, view != null);
        volumeBuilder = new FluxVolumeBuilder(state, flatness) {

            private Pool pool = new Pool();

            @Override
            protected void addPrimitive(FluxPrimitive prim) {
                FluxSceneVisitor.this.addPrimitive(prim);
            }

            @Override
            protected void addInfinitePrimitive(FluxPrimitive prim) {
                FluxSceneVisitor.this.addInfinitePrimitive(prim);
            }

            @Override
            protected Matrix4d getCurrentTransformation() {
                return FluxSceneVisitor.this.getCurrentTransformation();
            }

            @Override
            public Shader getCurrentShader() {
                return FluxSceneVisitor.this.getCurrentShader();
            }

            @Override
            public GraphState getRenderGraphState() {
                return FluxSceneVisitor.this.getGraphState();
            }

            @Override
            public Pool getPool() {
                return pool;
            }

            @Override
            protected void addPrimitives(Vector<FluxPrimitive> primitives) {
                FluxSceneVisitor.this.addPrimitives(primitives);
            }
        };
        shaderBuilder = new FluxShaderBuilder() {

            protected void warning(String warning) {
                FluxSceneVisitor.this.warning(warning);
            }
        };
        lightBuilder = new FluxLightBuilder();
        nodeToGroup = new LongToIntHashMap();
        grouping = 0;
        nextGroupIndex = 1;
        currentGroupIndex = -1;
        long sceneBuildTime = System.currentTimeMillis();
        graph.accept(null, this, null);
        volumeBuilder.finish();
        lightBuilder.finnish(sampleExplicit, view);
        sceneBuildTime = System.currentTimeMillis() - sceneBuildTime;
        Variables temp = new Variables();
        for (FluxPrimitive prim : primitives) prim.computeExtent(temp);
        for (FluxPrimitive prim : infPrimitives) prim.computeExtent(temp);
        for (FluxSensor prim : sensors) prim.computeExtent(temp);
    }

    private void addPrimitives(Vector<FluxPrimitive> prim) {
        if (prim.size() > 0) {
            int groupIndex = -1;
            if (measureFilter == null || measureFilter.filter(currentObject)) {
                addNodeToGroup(currentObject);
                groupIndex = currentGroupIndex;
            }
            primitives.addAll(prim);
            FluxShader flusShader = shaderBuilder.buildShader(currentShader);
            for (int i = 0; i < prim.size(); i++) {
                prim.get(i).setGroupIndex(groupIndex);
                prim.get(i).setOwner(currentRenderable);
                prim.get(i).setFluxShader(flusShader);
                prim.get(i).setIOR(currentIOR);
            }
        }
    }

    private void setupPrimitive(FluxPrimitive prim) {
        int groupIndex = -1;
        if (measureFilter == null || measureFilter.filter(currentObject)) {
            addNodeToGroup(currentObject);
            groupIndex = currentGroupIndex;
        }
        prim.setGroupIndex(groupIndex);
        prim.setOwner(currentRenderable);
        prim.setFluxShader(shaderBuilder.buildShader(currentShader));
        prim.setIOR(currentIOR);
    }

    private void addPrimitive(FluxPrimitive prim) {
        if ((primitives.size() % 50) == 0) {
            long t = System.currentTimeMillis();
            if (t >= nextProgressTime) {
                nextProgressTime = t + 200;
                monitor.setProgress(IMP3D.I18N.msg("ray.constructing-geometry", primitives.size()), ProgressMonitor.INDETERMINATE_PROGRESS);
            }
        }
        primitives.add(prim);
        setupPrimitive(prim);
    }

    private void addInfinitePrimitive(FluxPrimitive prim) {
        infPrimitives.add(prim);
        setupPrimitive(prim);
    }

    private void addNodeToGroup(Object object) {
        assert lastEnteredIsNode;
        if (grouping == 0) {
            currentGroupIndex = nextGroupIndex++;
            nodeToGroup.put(((Node) object).getId(), currentGroupIndex);
        }
    }

    private void warning(String warning) {
        this.log += "Warning, " + warning + "\n";
    }

    private void log(String log) {
        this.log += "Log, " + log + "\n";
    }

    @Override
    protected void visitImpl(Object object, boolean asNode, Shader s, Path path) {
        @SuppressWarnings("unchecked") Interior i = (Interior) state.getObjectDefault(object, asNode, Attributes.INTERIOR, currentInterior);
        if (i != null) {
            currentInterior = i;
        } else {
            i = currentInterior;
        }
        int rgb = getCurrentShader().getAverageColor();
        if (currentInterior != null) currentIOR = currentInterior.getIndexOfRefraction(new Spectrum3d(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f)); else currentIOR = 1.f;
        int csg = state.getIntDefault(object, asNode, Attributes.CSG_OPERATION, Attributes.CSG_NONE);
        if (csg != Attributes.CSG_NONE) warning("GPUFlux does not support CSG");
        @SuppressWarnings("unchecked") Object shape = state.getObjectDefault(object, asNode, Attributes.SHAPE, null);
        if (shape instanceof SensorNode) {
            if (enableSensors) {
                SensorNode sensor = (SensorNode) shape;
                double radius = sensor.getRadius();
                if (Math.abs(radius) > volumeBuilder.epsilon) {
                    Matrix4d t = getCurrentTransformation();
                    SensorDisc v = new SensorDisc();
                    volumeBuilder.setInvTransformation(v, t, 0);
                    radius = 1 / radius;
                    v.scale(radius, radius, radius);
                    int groupIndex = -1;
                    addNodeToGroup(object);
                    groupIndex = currentGroupIndex;
                    sensors.add(new FluxSensor(sensor, v, groupIndex));
                }
            }
        } else if (shape instanceof Renderable && !(shape instanceof Sky)) {
            currentRenderable = ((Renderable) shape);
            currentObject = object;
            currentShader = s;
            currentRenderable.draw(object, asNode, volumeBuilder);
        }
        if (asNode && (object instanceof LightNode)) {
            Matrix4d t = getCurrentTransformation();
            LightNode lightNode = (LightNode) object;
            lightBuilder.buildLight(lightNode.getLight(), t);
        }
        if (shape instanceof Sky) {
            Matrix4d t = getCurrentTransformation();
            LightNode sky = new LightNode();
            sky.setLight((Sky) shape);
            sky.setTransform(t);
            lightBuilder.buildLight(sky.getLight(), t);
        }
    }

    class TransformVisitor3D extends Visitor3D {

        protected void init(GraphState gs) {
            Matrix4d m = new Matrix4d();
            m.setIdentity();
            init(gs, gs.getGraph().getTreePattern(), m);
        }

        @Override
        protected void visitEnterImpl(Object object, boolean asNode, Path path) {
        }

        @Override
        protected void visitLeaveImpl(Object object, boolean asNode, Path path) {
        }

        public Matrix4d getGlobalTransformation(Node node) {
            Matrix4d mtrx = new Matrix4d();
            mtrx.setIdentity();
            if (node == null) return mtrx;
            Stack<Node> stack = new Stack<Node>();
            while (node != null) {
                stack.push(node);
                node = node.getSource();
            }
            ;
            Node root = stack.pop();
            ArrayPath placeInPath = new ArrayPath(graph);
            return getGlobalTransformation(root, stack, placeInPath);
        }

        private Matrix4d getGlobalTransformation(Node v, Stack<Node> stack, ArrayPath path) {
            Matrix4d mtrx;
            path.pushNode(v, v.getId() >= 0 ? v.getId() : v.hashCode());
            visitEnter(path, true);
            Instantiator i;
            if ((i = v.getInstantiator()) != null) {
                state.beginInstancing(v, path.getObjectId(-1));
                i.instantiate(path, this);
            }
            if (!stack.empty()) {
                Node child = stack.pop();
                path.pushEdgeSet(v.getEdgeTo(child), -1, false);
                mtrx = getGlobalTransformation(child, stack, path);
                path.popEdgeSet();
            } else {
                mtrx = (Matrix4d) getCurrentTransformation().clone();
            }
            if ((i = v.getInstantiator()) != null) {
                state.endInstancing();
            }
            visitLeave(v, path, true);
            path.popNode();
            return mtrx;
        }
    }

    ;

    @Override
    protected void visitEnterImpl(Object object, boolean asNode, Path path) {
        if (measureFilter == null) measureFilter.enter(object);
        interiorStack.push(currentInterior);
        super.visitEnterImpl(object, asNode, path);
    }

    @Override
    public Object visitInstanceEnter() {
        assert lastEnteredIsNode;
        if (grouping == 0) {
            currentGroupIndex = nodeToGroup.get(((Node) lastEntered).getId());
            if (currentGroupIndex == 0) {
                currentGroupIndex = nextGroupIndex++;
                nodeToGroup.put(((Node) lastEntered).getId(), currentGroupIndex);
            }
        }
        grouping++;
        return super.visitInstanceEnter();
    }

    @Override
    public boolean visitInstanceLeave(Object o) {
        if (measureFilter == null) measureFilter.leave(o);
        if (--grouping < 0) {
            throw new IllegalStateException();
        }
        return super.visitInstanceLeave(o);
    }

    @Override
    protected void visitLeaveImpl(Object object, boolean asNode, Path path) {
        super.visitLeaveImpl(object, asNode, path);
        currentInterior = interiorStack.pop();
    }

    public int getGroupCount() {
        return currentGroupIndex + 1;
    }

    public Vector<FluxChannelMap> getChannels() {
        return shaderBuilder.getChannelBuilder().getChannels();
    }

    public Vector<FluxShader> getShaders() {
        return shaderBuilder.getShaders();
    }

    public String getLog() {
        return log;
    }

    public FluxLightBuilder getLightBuilder() {
        return lightBuilder;
    }
}
