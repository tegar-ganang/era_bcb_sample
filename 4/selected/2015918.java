package de.grogra.gpuflux;

import de.grogra.pf.registry.Item;
import de.grogra.pf.ui.Workbench;
import de.grogra.util.EnumerationType;
import de.grogra.util.Utils;

public class FluxSettings {

    public static int PERFORMANCE_BUILD = 0;

    public static int PERFORMANCE_TRACE = 1;

    public static final EnumerationType PERFORMANCE_TYPE = new EnumerationType("performance", GPUFlux.I18N, PERFORMANCE_TRACE + 1);

    private static final int DEFAULT_MIN_MEASUREMENTS = 1024 * 8;

    private static final int DEFAULT_TRACE_DEPTH = 5;

    private static final int DEFAULT_PREFERRED_DURATION = 500;

    private static final float DEFAULT_MIN_POWER = 0.01f;

    private static final int INITIAL_SAMPLE_COUNT = 10000000;

    private static final int MAXIMUM_SAMPLE_COUNT = 100000000;

    private static final int INITIAL_SPECTRAL_RESOLUTION = 1;

    private static final float DEFAULT_FLATNESS = 10;

    private static final int DEFAULT_SKYRESOLUTION = 512;

    private static final int DEFAULT_CHANNELRESOLUTION = 128;

    private static Item general = null;

    private static Item ocl = null;

    private static Item advanced = null;

    private static Item renderer = null;

    private static void getItems() {
        if (general == null) general = Item.resolveItem(Workbench.current(), "/renderers/3d/GPUFluxRenderer/general");
        if (ocl == null) ocl = Item.resolveItem(Workbench.current(), "/renderers/3d/GPUFluxRenderer/opencl");
        if (advanced == null) advanced = Item.resolveItem(Workbench.current(), "/renderers/3d/GPUFluxRenderer/opencl/advanced");
        if (renderer == null) renderer = Item.resolveItem(Workbench.current(), "/renderers/3d/GPUFluxRenderer/renderer");
    }

    public static int getModelSpectralLambdaStep() {
        getItems();
        return Utils.getInt(general, "rayprocessor.spectralresolution", INITIAL_SPECTRAL_RESOLUTION);
    }

    public static float getModelFlatness() {
        getItems();
        return Utils.getFloat(general, "rayprocessor.flatness", DEFAULT_FLATNESS);
    }

    public static int getSkyResolution() {
        getItems();
        return Utils.getInt(general, "rayprocessor.skyresolution", DEFAULT_SKYRESOLUTION);
    }

    public static int getChannelResolution() {
        getItems();
        return Utils.getInt(general, "rayprocessor.channelresolution", DEFAULT_CHANNELRESOLUTION);
    }

    public static String getLightModelSettingsLog() {
        String log = "";
        log += "    <b>LightModel settings</b>\n";
        log += "        Spectral Resolution: " + getModelSpectralLambdaStep() + " nm\n";
        log += "        Tessellation flatness: " + getModelFlatness() + "\n";
        return log;
    }

    public static int getOCLInitialSampleCount() {
        getItems();
        return Utils.getInt(advanced, "rayprocessor.initialsamplecount", INITIAL_SAMPLE_COUNT);
    }

    public static int getOCLMaximumSampleCount() {
        return MAXIMUM_SAMPLE_COUNT;
    }

    public static double getOCLPreferredDuration() {
        getItems();
        return Utils.getInt(advanced, "rayprocessor.iterationtime", DEFAULT_PREFERRED_DURATION) / 1000.0;
    }

    public static int getOCLMinMeasurements() {
        getItems();
        return Utils.getInt(advanced, "rayprocessor.minimalmeasurements", DEFAULT_MIN_MEASUREMENTS);
    }

    public static boolean getOCLPrecompile() {
        getItems();
        return Utils.getBoolean(advanced, "precompile", true);
    }

    public static boolean getOCLGPU() {
        getItems();
        return Utils.getBoolean(ocl, "gpu", true);
    }

    public static boolean getOCLMultiGPU() {
        getItems();
        return Utils.getBoolean(ocl, "multigpu", true);
    }

    public static int getOCLPerformance() {
        getItems();
        return ((Integer) Utils.getObject(advanced, "performance", new Integer(PERFORMANCE_TRACE))).intValue();
    }

    public static String getOpenCLSettingsLog() {
        String log = "";
        log += "    <b>OpenCL settings</b>\n";
        log += "        Minimum batch size: " + getOCLInitialSampleCount() + "\n";
        log += "        Maximum batch size: " + getOCLMaximumSampleCount() + "\n";
        log += "        Prefered iteration duration: " + getOCLPreferredDuration() + "\n";
        log += "        Precompile: " + (getOCLPrecompile() ? "Enabled" : "Disabled") + "\n";
        log += "        GPU: " + (getOCLGPU() ? "Enabled" : "Disabled") + "\n";
        log += "        Multiple Devices: " + (getOCLMultiGPU() ? "Enabled" : "Disabled") + "\n";
        log += "        Performance Devices: " + (getOCLPerformance() == 0 ? "Fast Build" : "Fast Trace") + "\n";
        return log;
    }

    public static int getRenderDepth() {
        getItems();
        return Utils.getInt(renderer, "rayprocessor.depth", DEFAULT_TRACE_DEPTH);
    }

    public static float getRenderMinPower() {
        getItems();
        return Utils.getFloat(renderer, "rayprocessor.minpower", DEFAULT_MIN_POWER);
    }

    public static boolean getRenderSpectral() {
        getItems();
        return Utils.getBoolean(renderer, "spectral", false);
    }

    public static boolean getRenderDispersion() {
        getItems();
        return Utils.getBoolean(renderer, "dispersion", false);
    }

    public static String getRenderSettingsLog() {
        String log = "";
        log += "    <b>Render settings</b>\n";
        log += "        Depth: " + getRenderDepth() + "\n";
        log += "        Minimum Power: " + getRenderMinPower() + "\n";
        log += "        Spectral: " + (getRenderSpectral() ? "Enabled" : "Disabled") + "\n";
        log += "        Dispersian: " + (getRenderDispersion() ? "Enabled" : "Disabled") + "\n";
        return log;
    }

    public static String getTracerLog() {
        return getLightModelSettingsLog() + getOpenCLSettingsLog() + getRenderSettingsLog();
    }
}
