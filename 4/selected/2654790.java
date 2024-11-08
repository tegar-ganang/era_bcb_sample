package de.grogra.gpuflux.scene.shading.channel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import de.grogra.gpuflux.FluxSettings;
import de.grogra.gpuflux.jocl.compute.ComputeByteBuffer;
import de.grogra.imp3d.objects.LightDistribution;
import de.grogra.imp3d.shading.AffineUVTransformation;
import de.grogra.imp3d.shading.BlendItem;
import de.grogra.imp3d.shading.BumpMap;
import de.grogra.imp3d.shading.Carpenter;
import de.grogra.imp3d.shading.ChannelBlend;
import de.grogra.imp3d.shading.ChannelMapNode;
import de.grogra.imp3d.shading.ChannelMapNodeVisitor;
import de.grogra.imp3d.shading.ChannelSPD;
import de.grogra.imp3d.shading.Checker;
import de.grogra.imp3d.shading.Filter;
import de.grogra.imp3d.shading.Gradient;
import de.grogra.imp3d.shading.Granite;
import de.grogra.imp3d.shading.ImageMap;
import de.grogra.imp3d.shading.Julia;
import de.grogra.imp3d.shading.Laplace3D;
import de.grogra.imp3d.shading.Leopard;
import de.grogra.imp3d.shading.Mandel;
import de.grogra.imp3d.shading.Smooth3D;
import de.grogra.imp3d.shading.SunSkyLight;
import de.grogra.imp3d.shading.Turbulence;
import de.grogra.imp3d.shading.VolumeChecker;
import de.grogra.imp3d.shading.VolumeFunction;
import de.grogra.imp3d.shading.VolumeTurbulence;
import de.grogra.imp3d.shading.Wood;
import de.grogra.imp3d.shading.XYZTransformation;
import de.grogra.math.Channel;
import de.grogra.math.ChannelData;
import de.grogra.math.ChannelMap;
import de.grogra.math.ChannelMapInput;
import de.grogra.math.ColorMap;
import de.grogra.math.Graytone;
import de.grogra.math.RGBColor;
import de.grogra.pf.ui.Workbench;
import de.grogra.ray.physics.Environment;

public abstract class FluxChannelMapBuilder implements ChannelMapNodeVisitor {

    private boolean blendChannel = false;

    private FluxChannelMap currentChannelNode;

    private Hashtable<Object, FluxChannelMap> channelCache = new Hashtable<Object, FluxChannelMap>();

    private Vector<FluxChannelMap> channels = new Vector<FluxChannelMap>();

    private Matrix3f uvTransformation = new Matrix3f();

    {
        uvTransformation.setIdentity();
    }

    public void visit(AffineUVTransformation affineUVTransformation) {
        Matrix3f prevMtrx = uvTransformation;
        Matrix3f mtrx = (Matrix3f) uvTransformation.clone();
        mtrx.mul(affineUVTransformation.getTransform());
        uvTransformation = mtrx;
        ChannelMap input = affineUVTransformation.getInput();
        buildChannelMap(input);
        uvTransformation = prevMtrx;
    }

    public void visit(BumpMap bumpMap) {
        visit((ChannelMapNode) bumpMap);
    }

    public void visit(Carpenter carpenter) {
        visit((ChannelMapNode) carpenter);
    }

    public void visit(ChannelBlend channelBlend) {
        if (blendChannel) {
            Workbench.current().logGUIInfo("GPUFlux does not support nested blend channels, inner channel is discretized");
            visit((ChannelMapNode) channelBlend);
        } else {
            blendChannel = true;
            Vector<FluxBlendItem> blendItems = new Vector<FluxBlendItem>();
            BlendItem item = channelBlend.getBlend();
            while (item != null) {
                ChannelMap channel = item.getInput();
                FluxChannelMap fluxChannel = buildChannelMap(channel, null);
                blendItems.add(new FluxBlendItem(fluxChannel, item));
                item = item.getNext();
            }
            ;
            addChannelMap(new FluxBlend(blendItems), channelBlend);
            blendChannel = false;
        }
    }

    public void visit(Checker checker) {
        visit((ChannelMapNode) checker);
    }

    private Matrix3f getUVTransformation(ChannelMap channelMap) {
        Matrix3f mtrx = (Matrix3f) uvTransformation.clone();
        if (channelMap instanceof ChannelMapNode) {
            channelMap = ((ChannelMapNode) channelMap).getInput();
            if (channelMap instanceof AffineUVTransformation) {
                mtrx.mul(((AffineUVTransformation) channelMap).getTransform());
            }
        }
        return mtrx;
    }

    public void visit(ImageMap imageMap) {
        BufferedImage image = imageMap.getImageAdapter().getNativeImage();
        addCachedChannelMap(new FluxImageMap(image), imageMap, image);
    }

    public void visit(SunSkyLight sunSkyLight) {
        visit((ChannelMapNode) sunSkyLight);
    }

    public void visit(XYZTransformation xyzTransformation) {
        visit((ChannelMapNode) xyzTransformation);
    }

    protected FluxChannelMap getCachedChannelMap(Object key) {
        return channelCache.get(key);
    }

    protected void addCachedChannelMap(FluxChannelMap fluxCm, ChannelMap channelMapNode, Object key) {
        FluxChannelMap result = channelCache.get(key);
        if (result != null) {
            currentChannelNode = result;
        } else {
            addChannelMap(fluxCm, channelMapNode);
            channelCache.put(key, fluxCm);
        }
    }

    protected void addChannelMap(FluxChannelMap fluxCm, ChannelMap channelMapNode) {
        assert fluxCm != null;
        currentChannelNode = fluxCm;
        channels.add(fluxCm);
        fluxCm.setUVTransformation(getUVTransformation(channelMapNode));
    }

    public FluxChannelMap buildChannelMap(ChannelMap channelMap) {
        return buildChannelMap(channelMap, null);
    }

    public FluxChannelMap buildChannelMap(ChannelMap channelMap, ColorMap defaultChannel) {
        currentChannelNode = null;
        if (channelMap == null) channelMap = defaultChannel;
        if (channelMap != null && channelMap instanceof ChannelMap) ((ChannelMap) channelMap).accept(this);
        return currentChannelNode;
    }

    public void serialize(ComputeByteBuffer channel_out) throws IOException {
        Iterator<FluxChannelMap> chl_itr = channels.iterator();
        while (chl_itr.hasNext()) {
            FluxChannelMap channel = chl_itr.next();
            channel.setOffset(channel_out.size());
            channel.serialize(channel_out);
        }
        ;
    }

    public void visit(Graytone graytone) {
        addChannelMap(new FluxRGB(new RGBColor(graytone.getValue(), graytone.getValue(), graytone.getValue())), graytone);
    }

    public void visit(RGBColor rgbColor) {
        addChannelMap(new FluxRGB(rgbColor), rgbColor);
    }

    private void discretizeChannel(ChannelMap map) {
        int channelResolution = FluxSettings.getChannelResolution();
        FluxChannelMap chachedMap = getCachedChannelMap(map);
        if (chachedMap == null) {
            Environment env = new Environment();
            if (!(env.userObject instanceof ChannelMapInput)) {
                env.userObjectOwner = map;
                env.userObject = new ChannelMapInput();
            }
            ChannelData src = ((ChannelMapInput) env.userObject).getUserData(map, null), sink = src.createSink(map);
            env.normal.set(0, 0, 1);
            env.dpdu.set(1, 0, 0);
            env.dpdv.set(0, 1, 0);
            env.solid = true;
            Point3f image[][] = new Point3f[channelResolution][channelResolution];
            ChannelData cd = sink.getData(map);
            src.setFloat(Channel.IOR, env.iorRatio);
            src.setTuple3f(Channel.DPXDU, env.dpdu);
            src.setTuple3f(Channel.DPXDV, env.dpdv);
            for (int x = 0; x < channelResolution; x++) for (int y = 0; y < channelResolution; y++) {
                float u = x / (float) channelResolution;
                float v = y / (float) channelResolution;
                LightDistribution.map2cartesian(env.point, u, v);
                env.localPoint.set(env.point);
                env.normal.set(env.point);
                env.uv.set(u, v);
                src.setTuple3f(Channel.PX, env.point);
                src.setTuple3f(Channel.X, env.localPoint);
                src.setTuple3f(Channel.NX, env.normal);
                src.setTuple2f(Channel.U, env.uv);
                image[y][x] = new Point3f();
                cd.getTuple3f(image[y][x], sink, Channel.R);
            }
            addCachedChannelMap(new FluxHDRImageMap(image), map, map);
        } else {
            currentChannelNode = chachedMap;
        }
    }

    public void visit(ChannelMap channelMap) {
        if (channelMap instanceof ChannelSPD) {
            addChannelMap(new FluxSpectralChannel((ChannelSPD) (channelMap)), channelMap);
        } else {
            warning("GPUFlux does not support channel " + channelMap.getClass().getName() + ", discretized channel used instead.");
            discretizeChannel(channelMap);
        }
    }

    public void visit(ChannelMapNode map) {
        if (map instanceof ChannelMap) {
            visit((ChannelMap) map);
        } else {
            warning("GPUFlux does not support channel node " + map.getClass().getName() + ", discretized channel used instead.");
            discretizeChannel(map);
        }
    }

    public void visit(Filter filter) {
        visit((ChannelMapNode) filter);
    }

    public void visit(VolumeFunction volumeFunction) {
        visit((ChannelMapNode) volumeFunction);
    }

    public void visit(Gradient gradient) {
        visit((ChannelMapNode) gradient);
    }

    public void visit(Granite granite) {
        visit((ChannelMapNode) granite);
    }

    public void visit(Julia julia) {
        visit((ChannelMapNode) julia);
    }

    public void visit(Laplace3D laplace3d) {
        visit((ChannelMapNode) laplace3d);
    }

    public void visit(Leopard leopard) {
        visit((ChannelMapNode) leopard);
    }

    public void visit(Mandel mandel) {
        visit((ChannelMapNode) mandel);
    }

    public void visit(Smooth3D smooth3d) {
        visit((ChannelMapNode) smooth3d);
    }

    public void visit(Turbulence turbulence) {
        visit((ChannelMapNode) turbulence);
    }

    public void visit(VolumeChecker volumeChecker) {
        visit((ChannelMapNode) volumeChecker);
    }

    public void visit(VolumeTurbulence volumeTurbulence) {
        visit((ChannelMapNode) volumeTurbulence);
    }

    public void visit(Wood wood) {
        visit((ChannelMapNode) wood);
    }

    protected abstract void warning(String warning);

    public int getChannelCount() {
        return channels.size();
    }

    public Vector<FluxChannelMap> getChannels() {
        return channels;
    }
}
