package de.grogra.gpuflux.scene;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector4d;
import de.grogra.gpuflux.jocl.compute.ComputeByteBuffer;
import de.grogra.gpuflux.scene.experiment.MeasuringSetup;
import de.grogra.gpuflux.scene.experiment.MeasuringSetup.FluxDetector;
import de.grogra.gpuflux.scene.light.FluxLight;
import de.grogra.gpuflux.scene.shading.FluxShader;
import de.grogra.gpuflux.scene.shading.channel.FluxChannelMap;
import de.grogra.gpuflux.scene.volume.FluxPrimitive;
import de.grogra.gpuflux.scene.volume.FluxSensor;
import de.grogra.imp3d.Camera;
import de.grogra.imp3d.PerspectiveProjection;
import de.grogra.imp3d.Projection;
import de.grogra.vecmath.BoundingBox3d;
import de.grogra.vecmath.Math2;

public class FluxSceneSerializer {

    private static final int CAMERA_PROJECT = 0x1;

    private static final int CAMERA_PARALLEL = 0x2;

    private FluxScene scene;

    private MeasuringSetup measuringSetup;

    public void serializeScene(FluxScene scene) {
        this.scene = scene;
    }

    public void serializeMeasureSetup(MeasuringSetup measuringSetup) {
        this.measuringSetup = measuringSetup;
    }

    public void serializeDetectors(ComputeByteBuffer computeByteBuffer) throws IOException {
        Vector<FluxDetector> detectors = measuringSetup.getDetectors();
        for (int i = 0; i < detectors.size(); i++) {
            FluxDetector detector = detectors.get(i);
            computeByteBuffer.writeInt(detector.getOffset());
            computeByteBuffer.writeInt(detector.getMeasurements());
        }
    }

    public void serializeBVH(ComputeByteBuffer computeByteBuffer, boolean asBIH) throws IOException {
        if (asBIH) scene.getBVH().serializeBIH(computeByteBuffer, scene.getInfPrimitives().size()); else scene.getBVH().serializeBVH(computeByteBuffer, scene.getInfPrimitives().size());
    }

    public void serializeSensorBVH(ComputeByteBuffer computeByteBuffer, boolean asBIH) throws IOException {
        if (scene.getSensorBVH() != null) {
            if (asBIH) scene.getSensorBVH().serializeBIH(computeByteBuffer, 0); else scene.getSensorBVH().serializeBVH(computeByteBuffer, 0);
        }
    }

    public void serializeSensors(ComputeByteBuffer computeByteBuffer) throws IOException {
        if (scene.getSensorBVH() != null) {
            Integer sensorIdx[] = scene.getSensorBVH().getVolumeOrdering();
            for (int i = 0; i < sensorIdx.length; i++) {
                int idx = sensorIdx[i].intValue();
                FluxSensor sensor = scene.getSensors().get(idx);
                sensor.serialize(computeByteBuffer);
            }
        }
    }

    public void serializeShaders(ComputeByteBuffer computeByteBuffer) throws IOException {
        Vector<FluxShader> shaders = scene.getShaders();
        serializeShaders(shaders, computeByteBuffer);
    }

    private void serializeShaders(Vector<FluxShader> shaders, ComputeByteBuffer computeByteBuffer) throws IOException {
        Iterator<FluxShader> shr_itr = shaders.iterator();
        while (shr_itr.hasNext()) {
            FluxShader shader = shr_itr.next();
            shader.setOffset(computeByteBuffer.size());
            shader.serialize(computeByteBuffer);
        }
        ;
    }

    public void serializeChannels(ComputeByteBuffer computeByteBuffer) throws IOException {
        Vector<FluxChannelMap> channels = scene.getChannels();
        serializeChannels(channels, computeByteBuffer);
    }

    private void serializeChannels(Vector<FluxChannelMap> channels, ComputeByteBuffer computeByteBuffer) throws IOException {
        Iterator<FluxChannelMap> chl_itr = channels.iterator();
        while (chl_itr.hasNext()) {
            FluxChannelMap channel = chl_itr.next();
            channel.setOffset(computeByteBuffer.size());
            channel.serialize(computeByteBuffer);
        }
        ;
    }

    public void serializePrimitives(ComputeByteBuffer computeByteBuffer) throws IOException {
        Integer primIdx[] = scene.getBVH().getVolumeOrdering();
        Vector<FluxPrimitive> infPrimitives = scene.getInfPrimitives();
        Vector<FluxPrimitive> primitives = scene.getPrimitives();
        for (int i = 0; i < infPrimitives.size(); i++) {
            infPrimitives.get(i).setOffset(computeByteBuffer.size());
            infPrimitives.get(i).serialize(computeByteBuffer);
        }
        for (int i = 0; i < primIdx.length; i++) {
            int idx = primIdx[i];
            primitives.get(idx).setOffset(computeByteBuffer.size());
            primitives.get(idx).serialize(computeByteBuffer);
        }
    }

    public void serializePrimitiveOffsets(ComputeByteBuffer computeByteBuffer) throws IOException {
        Integer primIdx[] = scene.getBVH().getVolumeOrdering();
        Vector<FluxPrimitive> infPrimitives = scene.getInfPrimitives();
        Vector<FluxPrimitive> primitives = scene.getPrimitives();
        for (int i = 0; i < infPrimitives.size(); i++) {
            computeByteBuffer.writeInt(infPrimitives.get(i).getOffset());
        }
        for (int i = 0; i < primIdx.length; i++) {
            int idx = primIdx[i];
            computeByteBuffer.writeInt(primitives.get(idx).getOffset());
        }
    }

    public void serializeLights(ComputeByteBuffer computeByteBuffer) throws IOException {
        Vector<FluxLight> lights = scene.getLights();
        serializeChannels(scene.getLightChannels(), computeByteBuffer);
        serializeShaders(scene.getLightShaders(), computeByteBuffer);
        for (int i = 0; i < lights.size(); i++) {
            FluxLight light = lights.elementAt(i);
            int offset = computeByteBuffer.size();
            light.setOffset(offset);
            light.serialize(computeByteBuffer);
        }
        ;
    }

    public void serializeLightOffsets(ComputeByteBuffer computeByteBuffer) throws IOException {
        Vector<FluxLight> lights = scene.getLights();
        for (int i = 0; i < lights.size(); i++) {
            computeByteBuffer.writeInt(lights.elementAt(i).getOffset());
        }
    }

    public void serializeCumulativeLightPowerDistribution(ComputeByteBuffer computeByteBuffer) throws IOException {
        float[] cumPowers = scene.getCumulativeLightPower();
        for (int i = 0; i < cumPowers.length; i++) {
            computeByteBuffer.writeFloat(cumPowers[i]);
        }
    }

    public void serializeBoundingSphere(ComputeByteBuffer boundsStream) {
        BoundingBox3d bounds = scene.getBounds();
        Point3d c = new Point3d();
        bounds.getCenter(c);
        float radius = (float) bounds.getRadius();
        try {
            boundsStream.write(c);
            boundsStream.writeFloat(radius);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serializeCamera(ComputeByteBuffer computeByteBuffer, int width, int height) {
        Camera camera = scene.getCamera();
        Matrix4d m = new Matrix4d();
        Math2.invertAffine(camera.getWorldToViewTransformation(), m);
        Vector4d left = new Vector4d();
        Vector4d up = new Vector4d();
        Vector4d to = new Vector4d();
        Vector4d at = new Vector4d();
        m.getColumn(0, left);
        m.getColumn(1, up);
        m.getColumn(2, to);
        m.getColumn(3, at);
        Projection p = camera.getProjection();
        left.scale(1.0 / p.getScaleX());
        up.scale(-1.0 / p.getScaleY());
        left.scale(1.0);
        up.scale((double) height / (double) width);
        to.negate();
        try {
            if (p instanceof PerspectiveProjection) computeByteBuffer.writeInt(CAMERA_PROJECT); else computeByteBuffer.writeInt(CAMERA_PARALLEL);
            computeByteBuffer.write(left);
            computeByteBuffer.write(up);
            computeByteBuffer.write(to);
            computeByteBuffer.write(at);
            computeByteBuffer.writeInt(width);
            computeByteBuffer.writeInt(height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FluxScene getScene() {
        return scene;
    }

    public int getSkyOffset() {
        if (scene.getSky() == null) return -1;
        return scene.getSky().getOffset();
    }
}
