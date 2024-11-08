package yarfraw.mapping.forward.impl;

import java.util.List;
import yarfraw.core.datamodel.ChannelFeed;
import yarfraw.core.datamodel.ItemEntry;
import yarfraw.core.datamodel.YarfrawException;
import yarfraw.generated.rss10.elements.ObjectFactory;
import yarfraw.generated.rss10.elements.RDF;
import yarfraw.mapping.forward.ToRss10Channel;

/**
 * Util methods for mapping Yarfraw core model to Rss10 Jaxb model
 * @author jliang
 *
 */
public class ToRss10ChannelImpl implements ToRss10Channel {

    private static ToRss10Channel _instance = new ToRss10ChannelImpl();

    private static final ObjectFactory FACTORY = new ObjectFactory();

    public static final ToRss10Channel getInstance() {
        return _instance;
    }

    private ToRss10ChannelImpl() {
    }

    /**
   * Model: (channel, image?, item+, textinput?)
   */
    public RDF execute(ChannelFeed ch) throws YarfrawException {
        RDF rdf = FACTORY.createRDF();
        List<Object> elementList = rdf.getChannelOrImageOrItem();
        elementList.add(Rss10MappingUtils.toChannel(ch));
        if (ch.getImageOrIcon() != null) {
            elementList.add(Rss10MappingUtils.toRss10Image(ch.getImageOrIcon()));
        }
        if (ch.getItems() != null) {
            for (ItemEntry item : ch.getItems()) {
                if (item != null) {
                    elementList.add(ToRss10ChannelItemImpl.getInstance().execute(item));
                }
            }
        }
        if (ch.getTexInput() != null) {
            elementList.add(Rss10MappingUtils.toRss10TextInput(ch.getTexInput()));
        }
        return rdf;
    }
}
