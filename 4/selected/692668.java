package de.grogra.gpuflux.scene;

import java.util.Vector;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import de.grogra.gpuflux.FluxSettings;
import de.grogra.gpuflux.scene.BVH.BVHAnalyzer;
import de.grogra.gpuflux.scene.BVH.BVHBuilder;
import de.grogra.gpuflux.scene.BVH.BVHBuilderMiddle;
import de.grogra.gpuflux.scene.BVH.BVHBuilderSAH;
import de.grogra.gpuflux.scene.BVH.BVHTree;
import de.grogra.gpuflux.scene.BVH.ThreadedBVHBuilderMiddle;
import de.grogra.gpuflux.scene.BVH.ThreadedBVHBuilderSAH;
import de.grogra.gpuflux.scene.filter.ObjectFilter;
import de.grogra.gpuflux.scene.light.FluxLight;
import de.grogra.gpuflux.scene.light.FluxLightBuilder;
import de.grogra.gpuflux.scene.shading.FluxShader;
import de.grogra.gpuflux.scene.shading.channel.FluxChannelMap;
import de.grogra.gpuflux.scene.volume.FluxPrimitive;
import de.grogra.gpuflux.scene.volume.FluxSensor;
import de.grogra.graph.Graph;
import de.grogra.graph.GraphState;
import de.grogra.imp3d.Camera;
import de.grogra.imp3d.ViewConfig3D;
import de.grogra.ray.physics.Environment;
import de.grogra.ray.physics.Spectrum3f;
import de.grogra.ray2.ProgressMonitor;
import de.grogra.vecmath.BoundingBox3d;
import de.grogra.vecmath.geom.BoundingBox;
import de.grogra.vecmath.geom.Variables;
import de.grogra.xl.util.LongToIntHashMap;

public class FluxScene {

    private Vector<FluxPrimitive> primitives;

    private Vector<FluxPrimitive> infPrimitives;

    private Vector<FluxSensor> sensors;

    private Vector<FluxChannelMap> channels;

    private Vector<FluxShader> shaders;

    private LongToIntHashMap nodeToGroup;

    private BVHTree bvh;

    private BVHTree sensorBvh;

    private BoundingBox3d bounds;

    private float[] cumulativeLightPower;

    private int sampleCount;

    private FluxLight sky;

    private Vector<FluxLight> lights;

    private Vector<FluxChannelMap> lightChannels;

    private Vector<FluxShader> lightShaders;

    private int groupCount;

    private String log = "";

    private Graph graph;

    private ViewConfig3D view;

    private void setLights(FluxLightBuilder lightBuilder) {
        sampleCount = lightBuilder.getSampleCount();
        cumulativeLightPower = (lightBuilder.getCummulativePowerBuffer(getEnvironment()));
        sky = (lightBuilder.getSky());
        lights = (lightBuilder.getLights());
        lightShaders = (lightBuilder.getLightShaders());
        lightChannels = (lightBuilder.getLightChannels());
    }

    public void buildLightsFromGraph(Graph graph, ViewConfig3D view, ObjectFilter measureFilter, boolean enableSensors, ProgressMonitor monitor, boolean sampleExplicit) {
        Matrix4d m = new Matrix4d();
        m.setIdentity();
        FluxLightVisitor lightVisitor = new FluxLightVisitor();
        lightVisitor.init(GraphState.current(graph), m, view, view != null);
        lightVisitor.visitScene(graph, view, measureFilter, enableSensors, sampleExplicit, monitor);
        setLights(lightVisitor.getLightBuilder());
    }

    public void buildSceneFromGraph(Graph graph, ViewConfig3D view, ObjectFilter measureFilter, boolean enableSensors, ProgressMonitor monitor, boolean sampleExplicit, float flatness) {
        this.graph = graph;
        this.view = view;
        Matrix4d m = new Matrix4d();
        m.setIdentity();
        long gatheringTime = System.currentTimeMillis();
        log("<b>Build Profile</b>");
        FluxSceneVisitor sceneVisitor = new FluxSceneVisitor();
        sceneVisitor.init(GraphState.current(graph), m, view, view != null);
        sceneVisitor.visitScene(graph, view, measureFilter, enableSensors, sampleExplicit, monitor, flatness, null);
        log(sceneVisitor.getLog());
        gatheringTime = System.currentTimeMillis() - gatheringTime;
        log("    Object gather time:    " + gatheringTime + " ms");
        primitives = sceneVisitor.getPrimitives();
        infPrimitives = sceneVisitor.getInfPrimitives();
        sensors = sceneVisitor.getSensors();
        channels = sceneVisitor.getChannels();
        shaders = sceneVisitor.getShaders();
        groupCount = sceneVisitor.getGroupCount();
        nodeToGroup = sceneVisitor.getNodeToGroup();
        long computeBoundingBoxesTime = System.currentTimeMillis();
        Variables temp = new Variables();
        for (FluxPrimitive prim : primitives) prim.computeExtent(temp);
        for (FluxPrimitive prim : infPrimitives) prim.computeExtent(temp);
        for (FluxSensor prim : sensors) prim.computeExtent(temp);
        computeBoundingBoxesTime = System.currentTimeMillis() - computeBoundingBoxesTime;
        log("    Extend compute time:   " + computeBoundingBoxesTime + " ms");
        int performance = FluxSettings.getOCLPerformance();
        BVHBuilder bvhBuilder = null;
        if (performance == FluxSettings.PERFORMANCE_TRACE) bvhBuilder = new ThreadedBVHBuilderSAH(); else bvhBuilder = new ThreadedBVHBuilderMiddle();
        monitor.setProgress("Build BVH", -1);
        long bvhConstructionTime = System.currentTimeMillis();
        bvh = bvhBuilder.construct(primitives);
        bvhConstructionTime = System.currentTimeMillis() - bvhConstructionTime;
        log("    BVH build time:        " + bvhConstructionTime + " ms");
        log(bvhBuilder.getLog());
        bounds = new BoundingBox3d();
        bounds.extent(bvh.getBounds());
        long sensorBvhConstructionTime = 0;
        if (enableSensors) {
            monitor.setProgress("Build Sensor BVH", -1);
            sensorBvhConstructionTime = System.currentTimeMillis();
            sensorBvh = bvhBuilder.construct(sensors);
            sensorBvhConstructionTime = System.currentTimeMillis() - sensorBvhConstructionTime;
            log("    Sensor BVH build time: " + sensorBvhConstructionTime + " ms");
            log(bvhBuilder.getLog());
            bounds.extent(sensorBvh.getBounds());
        }
        if (bounds.isEmpty()) {
            bounds.getMin().set(-1, -1, -1);
            bounds.getMax().set(1, 1, 1);
        }
        setLights(sceneVisitor.getLightBuilder());
    }

    public String getSceneStats() {
        String stats = "";
        stats += "<b>Scene Stats</b>\n";
        stats += "    <b>Scene</b>\n";
        if (primitives != null) stats += "    Number primitives: " + primitives.size() + "\n";
        if (infPrimitives != null) stats += "    Number inf prims:  " + infPrimitives.size() + "\n";
        if (sensors != null) stats += "    Number sensors:    " + sensors.size() + "\n";
        if (channels != null) stats += "    Number channels:   " + channels.size() + "\n";
        if (shaders != null) stats += "    Number shaders:    " + shaders.size() + "\n";
        stats += "    Number Groups:     " + groupCount + "\n";
        stats += "\n";
        BVHAnalyzer bvhAnalyzer = new BVHAnalyzer();
        if (bvh != null) {
            stats += "    <b>BVH</b>" + "\n";
            stats += bvhAnalyzer.analyzeBVH(bvh) + "\n";
            stats += "\n";
        }
        if (sensorBvh != null) {
            stats += "    <b>Sensor BVH</b>" + "\n";
            stats += bvhAnalyzer.analyzeBVH(sensorBvh) + "\n";
            stats += "\n";
        }
        stats += "    <b>Lights</b>" + "\n";
        stats += "    Number lights:         " + lights.size() + "\n";
        stats += "    Number light samples:  " + sampleCount + "\n";
        stats += "    Number light shaders:  " + lightShaders.size() + "\n";
        stats += "    Number light channels: " + lightChannels.size() + "\n";
        stats += "    Sky: " + ((sky != null) ? "Enabled" : "Disabled") + "\n";
        stats += "\n";
        return stats;
    }

    private void log(String log) {
        this.log += log + "\n";
    }

    public void cleanLog() {
        log = "";
    }

    public String getLog() {
        return log;
    }

    public BVHTree getBVH() {
        return bvh;
    }

    public BVHTree getSensorBVH() {
        return sensorBvh;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public Vector<FluxPrimitive> getPrimitives() {
        return primitives;
    }

    public Vector<FluxSensor> getSensors() {
        return sensors;
    }

    public Vector<FluxPrimitive> getInfPrimitives() {
        return infPrimitives;
    }

    public Vector<FluxShader> getShaders() {
        return shaders;
    }

    public Vector<FluxChannelMap> getChannels() {
        return channels;
    }

    public BoundingBox3d getBounds() {
        return bounds;
    }

    public Environment getEnvironment() {
        BoundingBox bb = new BoundingBox(new Point3d(bounds.getMin()), new Point3d(bounds.getMax()));
        return new Environment(bb, new Spectrum3f(), Environment.RADIATION_MODEL);
    }

    public int getSensorCount() {
        return sensors.size();
    }

    public int getSensorBVHRoot() {
        return sensorBvh.getRootIdx();
    }

    public int getPrimitiveCount() {
        return primitives.size();
    }

    public int getInfPrimitiveCount() {
        return infPrimitives.size();
    }

    public int getBVHRoot() {
        return bvh.getRootIdx();
    }

    public Graph getGraph() {
        return graph;
    }

    public ViewConfig3D getView() {
        return view;
    }

    public float[] getCumulativeLightPower() {
        return cumulativeLightPower;
    }

    public FluxLight getSky() {
        return sky;
    }

    public Vector<FluxLight> getLights() {
        return lights;
    }

    public Vector<FluxChannelMap> getLightChannels() {
        return lightChannels;
    }

    public Vector<FluxShader> getLightShaders() {
        return lightShaders;
    }

    public int getLightCount() {
        return lights.size();
    }

    public Camera getCamera() {
        return view.getCamera();
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public LongToIntHashMap getNodeToGroup() {
        return nodeToGroup;
    }
}
