package de.grogra.imp3d.gl20;

import java.util.Stack;
import de.grogra.imp3d.shading.ChannelBlend;
import de.grogra.imp3d.shading.ChannelMapNode;
import de.grogra.imp3d.shading.Material;
import de.grogra.imp3d.shading.Phong;
import de.grogra.imp3d.shading.RGBAShader;
import de.grogra.imp3d.shading.Shader;
import de.grogra.imp3d.shading.SwitchShader;
import de.grogra.math.ChannelMap;
import de.grogra.math.ColorMap;
import de.grogra.math.Graytone;
import de.grogra.math.RGBColor;

public class GL20ShaderServer {

    private static GL20ShaderServer singleton = null;

    private Stack<GL20ResourceShader> shaderStack = new Stack<GL20ResourceShader>();

    private Stack<GL20ResourceShaderFragment> fragmentStack = new Stack<GL20ResourceShaderFragment>();

    private GL20ShaderServer() {
        if (singleton == null) {
            singleton = this;
        }
    }

    /**
	 * get the equivalent <code>GL20ResourceShader</code> by a given <code>shader</code>
	 * 
	 * @param shader the <code>shader</code>
	 * @return the 
	 */
    public static GL20ResourceShader getShader(Shader shader) {
        if (singleton == null) new GL20ShaderServer();
        GL20ResourceShader returnValue = null;
        final int shaderCount = singleton.shaderStack.size();
        int shaderIndex = 0;
        while (shaderIndex < shaderCount) {
            if (singleton.shaderStack.get(shaderIndex).getShader() == shader) break; else shaderIndex++;
        }
        if (shaderIndex >= shaderCount) {
            GL20ResourceShader newShader = null;
            if (shader instanceof Material) {
                if (shader instanceof Phong) {
                    newShader = new GL20ResourceShaderPhong();
                }
            } else if (shader instanceof RGBAShader) {
                newShader = new GL20ResourceShaderRGBA();
            } else if (shader instanceof SwitchShader) {
            }
            if (newShader != null) {
                newShader.setShader(shader);
                newShader.update();
                returnValue = newShader;
                singleton.shaderStack.add(newShader);
            }
        } else returnValue = singleton.shaderStack.get(shaderIndex);
        return returnValue;
    }

    public static GL20ResourceShaderFragment getShaderFragment(ChannelMap channelMap) {
        if (channelMap == null) return null;
        if (singleton == null) new GL20ShaderServer();
        GL20ResourceShaderFragment returnValue = null;
        final int fragmentCount = singleton.fragmentStack.size();
        int fragmentIndex = 0;
        while (fragmentIndex < fragmentCount) {
            if (singleton.fragmentStack.get(fragmentIndex).getChannelMap() == channelMap) break; else fragmentIndex++;
        }
        if (fragmentIndex >= fragmentCount) {
            GL20ResourceShaderFragment newFragment = null;
            if (channelMap instanceof ColorMap) {
                if (channelMap instanceof Graytone) newFragment = new GL20ResourceShaderFragmentGraytone(); else if (channelMap instanceof RGBColor) newFragment = new GL20ResourceShaderFragmentRGBColor();
            } else if (channelMap instanceof ChannelMapNode) {
                if (channelMap instanceof ChannelBlend) newFragment = new GL20ResourceShaderFragmentBlend();
            }
            if (newFragment != null) {
                newFragment.setChannelMap(channelMap);
                newFragment.update();
                returnValue = newFragment;
                singleton.fragmentStack.add(newFragment);
            }
        } else returnValue = singleton.fragmentStack.get(fragmentIndex);
        return returnValue;
    }

    public static void removeShader(GL20ResourceShader shader) {
        if ((singleton != null) && (shader != null)) {
            final int shaderCount = singleton.shaderStack.size();
            int shaderIndex = 0;
            while (shaderIndex < shaderCount) {
                if (singleton.shaderStack.get(shaderIndex) == shader) break; else shaderIndex++;
            }
            if (shaderIndex < shaderCount) {
                singleton.shaderStack.remove(shaderIndex);
            }
        }
    }
}
