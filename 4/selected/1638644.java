package de.grogra.gpuflux.scene.shading;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import de.grogra.gpuflux.jocl.compute.ComputeByteBuffer;
import de.grogra.gpuflux.scene.shading.channel.FluxChannelMapBuilder;
import de.grogra.imp3d.shading.AlgorithmSwitchShader;
import de.grogra.imp3d.shading.IORShader;
import de.grogra.imp3d.shading.Phong;
import de.grogra.imp3d.shading.RGBAShader;
import de.grogra.imp3d.shading.Shader;
import de.grogra.imp3d.shading.ShaderRef;
import de.grogra.imp3d.shading.ShaderVisitor;
import de.grogra.imp3d.shading.SideSwitchShader;
import de.grogra.imp3d.shading.SunSkyLight;

public abstract class FluxShaderBuilder implements ShaderVisitor {

    FluxChannelMapBuilder channelMapBuilder = new FluxChannelMapBuilder() {

        protected void warning(String warning) {
            FluxShaderBuilder.this.warning(warning);
        }
    };

    private FluxShader currentFluxShader;

    private Hashtable<Shader, FluxShader> shaderCache = new Hashtable<Shader, FluxShader>();

    private Vector<FluxShader> shaders = new Vector<FluxShader>();

    private FluxRGBAShader defaultShader;

    {
        defaultShader = new FluxRGBAShader(new RGBAShader());
        shaders.add(defaultShader);
        currentFluxShader = defaultShader;
    }

    public void visit(ShaderRef shaderRef) {
        shaderRef.resolve().accept(this);
    }

    protected abstract void warning(String warning);

    public void visit(Phong phong) {
        addShader(new FluxPhongShader(phong, channelMapBuilder), phong);
    }

    public void visit(RGBAShader rgbaShader) {
        addShader(new FluxRGBAShader(rgbaShader), rgbaShader);
    }

    boolean switched = false;

    boolean iored = false;

    public void visit(SideSwitchShader sideSwitchShader) {
        Shader front = sideSwitchShader.getFrontShader();
        Shader back = sideSwitchShader.getBackShader();
        if (switched == false) {
            switched = true;
            FluxShader fluxFront = buildShader(front);
            FluxShader fluxBack = buildShader(back);
            switched = false;
            addShader(new FluxSwitchShader(fluxFront, fluxBack), sideSwitchShader);
        } else {
            warning("GPUFlux: nested switch shader has no effect.");
            buildShader(front);
        }
    }

    public void visit(SunSkyLight sunSkyLight) {
        visit((Shader) sunSkyLight);
    }

    public void visit(AlgorithmSwitchShader algorithmSwitchShader) {
        algorithmSwitchShader.getRaytracerShader().accept(this);
    }

    public void visit(Shader shader) {
        if (shader instanceof IORShader) {
            IORShader iorShader = (IORShader) shader;
            if (iored) {
                warning("GPUFlux: nested switch shader has no effect.");
                buildShader(iorShader.getInputshader());
            } else {
                boolean oldswitch = switched;
                switched = true;
                iored = true;
                FluxShader fluxInput = buildShader(iorShader.getInputshader());
                iored = false;
                switched = oldswitch;
                addShader(new FluxIORShader(iorShader, fluxInput), iorShader);
            }
        } else {
            warning("GPUFlux: Unkown shader " + shader.getClass().getName() + ", average color is used instead.");
            RGBAShader rgbaShader = new RGBAShader(shader.getAverageColor());
            addShader(new FluxRGBAShader(rgbaShader), rgbaShader);
        }
    }

    public FluxShader buildShader(Shader s) {
        currentFluxShader = defaultShader;
        if (s != null) {
            FluxShader result = shaderCache.get(s);
            if (result == null) s.accept(this); else currentFluxShader = result;
        }
        return currentFluxShader;
    }

    private void addShader(FluxShader fluxShader, Shader shader) {
        currentFluxShader = fluxShader;
        shaderCache.put(shader, currentFluxShader);
        shaders.add(fluxShader);
    }

    public void serialize(ComputeByteBuffer channel_out, ComputeByteBuffer shader_out) throws IOException {
        channelMapBuilder.serialize(channel_out);
        Iterator<FluxShader> shr_itr = shaders.iterator();
        while (shr_itr.hasNext()) {
            FluxShader shader = shr_itr.next();
            shader.setOffset(shader_out.size());
            shader.serialize(shader_out);
        }
        ;
    }

    public int getShaderCount() {
        return shaders.size();
    }

    public FluxChannelMapBuilder getChannelBuilder() {
        return channelMapBuilder;
    }

    public Vector<FluxShader> getShaders() {
        return shaders;
    }
}
