package example;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.opencl.CLCommandQueue;
import javax.opencl.CLContext;
import javax.opencl.CLDevice;
import javax.opencl.CLImageFormat;
import javax.opencl.CLKernel;
import javax.opencl.CLMem;
import javax.opencl.CLProgram;
import javax.opencl.OpenCL;

public class ExVolumeRender {

    CLContext clc;

    File volumeFilename = new File("example/Bucky.raw");

    int[] volumeSize = new int[] { 32, 32, 32 };

    int width = 512, height = 512;

    int[] gridSize = new int[] { width, height };

    float density = 0.05f;

    float brightness = 1.0f;

    float transferOffset = 0.0f;

    float transferScale = 1.0f;

    boolean linearFiltering = true;

    byte[] hVolume;

    CLKernel kernel;

    float transferFunc[] = { 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f };

    public static byte[] readFileRaw(File file) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileInputStream is = new FileInputStream(file);
        byte[] buf = new byte[1024];
        int ret;
        while ((ret = is.read(buf)) != -1) os.write(buf, 0, ret);
        return os.toByteArray();
    }

    public void foo() {
        try {
            clc = OpenCL.createContext(OpenCL.CL_DEVICE_TYPE_GPU);
            CLDevice device = Common.getFastestDevice(clc);
            CLCommandQueue cq = clc.createCommandQueue(device);
            CLProgram prog = clc.createProgram(Common.readFile(new File("example/volumeRenderer.cl")));
            prog.build();
            kernel = prog.createKernel("d_render");
            hVolume = readFileRaw(volumeFilename);
            initOpenCL(hVolume);
            initPixelBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    }

    public void initPixelBuffer() {
    }

    public void initOpenCL(byte[] hVolume) {
        CLImageFormat volumeFormat = new CLImageFormat(OpenCL.CL_R, OpenCL.CL_UNORM_INT8);
        CLMem d_volumeArray = clc.createImage3D(OpenCL.CL_MEM_READ_ONLY | OpenCL.CL_MEM_COPY_HOST_PTR, volumeFormat, volumeSize[0], volumeSize[1], volumeSize[2], volumeSize[0], volumeSize[0] * volumeSize[1], hVolume);
        CLImageFormat transferFunc_format = new CLImageFormat(OpenCL.CL_RGBA, OpenCL.CL_FLOAT);
        CLMem d_transferFuncArray = clc.createImage2D(OpenCL.CL_MEM_READ_ONLY | OpenCL.CL_MEM_COPY_HOST_PTR, transferFunc_format, 9, 1, 9 * 4, transferFunc);
        kernel.setKernelArg(8, d_volumeArray);
        kernel.setKernelArg(9, d_transferFuncArray);
        CLMem d_invViewMatrix = clc.createBuffer(OpenCL.CL_MEM_READ_ONLY, Float.class, 12);
        kernel.setKernelArg(8, d_invViewMatrix);
    }
}
