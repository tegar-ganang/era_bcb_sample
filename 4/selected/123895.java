package consciouscode.bonsai.tags;

import consciouscode.bonsai.channels.BasicChannel;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelProvider;
import consciouscode.bonsai.components.GenericPanel;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.lang.StringUtils;

/**
    A Jelly tag that creates a Bonsai {@link Channel}.  This tag must be nested
    within a tag defining a {@link GenericPanel} (for example,
    {@link PanelTag}).

    <p>
    For example:
    *<pre>&lt;channel name="message" value="Welcome, ${username}!" /&gt;
    *&lt;channel name="sameName" provider="${anAction}" /&gt;
    *&lt;channel name="newName" provider="${anAction}"
    *         nameInProvider="oldName" /&gt;
    *</pre>

    This tag supports the following attributes:
    <table border="1" cellpadding="3" cellspacing="0">
      <tr class="TableHeadingColor">
        <th align="center"><strong>Attribute</strong></th>
        <th align="center"><strong>Default</strong></th>
        <th><strong>Description</strong></th>
      </tr>
      <tr>
        <td><code>name</code></td>
        <td><em>Required</em></td>
        <td>The name under which this channel will be defined in the enclosing <code>ChannelProvider</code>.</td>
      </tr>
      <tr>
        <td><code>value</code></td>
        <td><code>null</code></td>
        <td>The initial value, stored in a new {@link BasicChannel}.
        It is not valid to specify both <code>value</code> and
        <code>provider</code>.</td>
      </tr>
      <tr>
        <td><code>provider</code></td>
        <td><em>Optional</em></td>
        <td>A {@link ChannelProvider} from which to get an existing channel,
        instead of creating a new one.
        The name of the existing channel is specified via
        <code>nameInProvider</code> (if renaming the channel) or
        <code>name</code> (if the same name is desired).
        It is not valid to specify both <code>value</code> and
        <code>provider</code>.</td>
      </tr>
      <tr>
        <td><code>nameInProvider</code></td>
        <td><em>Optional</em></td>
        <td>The name of an existing channel in the <code>provider</code>.
        Using this tag allows a channel to be given a new name.
        It is not valid to specify this unless <code>provider</code> is also
        given.</td>
      </tr>
    </table>
    <p>
    The body of this tag is evaluated, but has no particular purpose.
*/
public class ChannelTag extends TagSupport {

    public void setVar(String var) {
        myVar = var;
    }

    public void setName(String name) {
        myName = name;
    }

    public void setValue(Object value) {
        myValue = value;
    }

    public void setProvider(ChannelProvider provider) {
        myProvider = provider;
    }

    public void setNameInProvider(String name) {
        myNameInProvider = name;
    }

    @Override
    public void doTag(final XMLOutput output) throws Exception {
        if ((myName == null) || (myName.length() == 0)) {
            throw new MissingAttributeException("name");
        }
        Channel channel;
        if (myProvider != null) {
            if (myValue != null) {
                throw new JellyException("Cannot specify both value and " + "provider attributes.");
            }
            String oldName = myNameInProvider;
            if ((oldName == null) || (oldName.length() == 0)) {
                oldName = myName;
            }
            try {
                channel = myProvider.getChannel(oldName);
            } catch (IllegalArgumentException e) {
                throw new JellyException("Provider does not define channel '" + oldName + "'.");
            }
        } else {
            channel = new BasicChannel(myValue);
        }
        PanelTag panelTag = (PanelTag) findAncestorWithClass(PanelTag.class);
        if (panelTag == null) {
            throw new JellyException("This tag must be nested within a " + "<panel> tag");
        }
        GenericPanel panel = (GenericPanel) panelTag.getComponent();
        panel.defineChannel(myName, channel);
        invokeBody(output);
        if (StringUtils.isNotEmpty(myVar)) {
            context.setVariable(myVar, channel);
        }
    }

    private String myVar;

    private String myName;

    private Object myValue;

    private ChannelProvider myProvider;

    private String myNameInProvider;
}
