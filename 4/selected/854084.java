package consciouscode.bonsai.tags;

import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelProvider;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;

/**
    A Jelly tag that stores a value into a Bonsai {@link Channel}.
    This tag must be nested within a tag defining a {@link ChannelProvider}
    (for example, {@link PanelTag}).

    <p>
    For example:
    *<pre>&lt;setChannel name="actionInvoked" value="${true}" /&gt;
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
        <td>The name of the channel whose value is to be set. The channel must
        be defined in the enclosing <code>ChannelProvider</code>.</td>
      </tr>
      <tr>
        <td><code>value</code></td>
        <td><code>null</code></td>
        <td>The new value to be stored in the channel.</td>
      </tr>
      <tr>
        <td><code>provider</code></td>
        <td><em>Optional</em></td>
        <td>A {@link ChannelProvider} from which to get the channel.
        If no provider is given, the provider is taken from the nearest
        appropriate parent tag.</td>
      </tr>
    </table>
    <p>
    The body of this tag is evaluated, but has no particular purpose.
*/
public class SetChannelTag extends TagSupport {

    public void setName(String name) {
        myName = name;
    }

    public void setValue(Object value) {
        myValue = value;
    }

    public void setProvider(ChannelProvider provider) {
        myProvider = provider;
    }

    @Override
    public void doTag(final XMLOutput output) throws Exception {
        if ((myName == null) || (myName.length() == 0)) {
            throw new MissingAttributeException("name");
        }
        Channel channel;
        if (myProvider != null) {
            channel = myProvider.getChannel(myName);
        } else {
            channel = BonsaiTagUtils.findChannelInParentTags(myName, this);
        }
        channel.setValue(myValue);
        invokeBody(output);
    }

    private String myName;

    private Object myValue;

    private ChannelProvider myProvider;
}
