package v4view.web.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.IOUtils;
import v4view.web.IPageElementCallback;
import v4view.web.Script;

/**
 * Creates a script tag block and fills its contents with the content of the specified resource.
 * 
 * @author J Patrick Davenport
 *
 */
public class ResourceBackedScript extends Script {

    private static Map<String, String> loadedResources = new ConcurrentHashMap<String, String>();

    private boolean hasRendered;

    private String resourceLocation;

    {
        this.addBeforeRenderCallBack(new IPageElementCallback<ResourceBackedScript>() {

            @Override
            public void execute(final ResourceBackedScript _elementBeingLoaded) {
                ResourceBackedScript.this.loadMe(_elementBeingLoaded);
            }
        });
    }

    public ResourceBackedScript() {
    }

    /**
	 * Sets the location of the resource. If this is called after the instance has rendered
	 * the set is ignored.
	 * 
	 * @param _scriptBodyLocation a location found within either a classpath relative spot,
	 * or a hard coded file location.
	 */
    public ResourceBackedScript(final String _scriptBodyLocation) {
        this.resourceLocation = _scriptBodyLocation;
    }

    /**
	 * @throws UnsupportedOperationException since you're loading from a file, you shouldn't
	 * set the source.
	 */
    @Override
    public Script setSrc(final String _value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Since this a backed element, you shouldn't set the source.");
    }

    /**
	 * @throws UnsupportedOperationException since you're loading from a file, you shouldn't
	 * set the source.
	 */
    @Override
    public String getSrc() throws UnsupportedOperationException {
        return null;
    }

    public ResourceBackedScript setScriptBodyLocation(final String _scriptBodyLocation) {
        if (!this.hasRendered) {
            this.resourceLocation = _scriptBodyLocation;
        }
        return this;
    }

    public String getScriptBodyLocation() {
        return this.resourceLocation;
    }

    public static void clearResourceCache() {
        loadedResources.clear();
    }

    private void loadMe(final ResourceBackedScript e) {
        if (!loadedResources.containsKey(this.resourceLocation)) {
            final InputStream resourceAsStream = this.getClass().getResourceAsStream(this.resourceLocation);
            final StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(resourceAsStream, writer);
            } catch (final IOException ex) {
                throw new IllegalStateException("Resource not read-able", ex);
            }
            final String loadedResource = writer.toString();
            loadedResources.put(this.resourceLocation, loadedResource);
        }
        this.setScriptBody(loadedResources.get(this.resourceLocation));
        this.hasRendered = true;
    }
}
