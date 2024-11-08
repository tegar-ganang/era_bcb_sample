package com.thoughtworks.blipit.persistence;

import com.google.appengine.api.datastore.Category;
import com.google.appengine.api.datastore.Key;
import com.thoughtworks.blipit.Utils;
import com.thoughtworks.blipit.domain.Blip;
import com.thoughtworks.blipit.domain.Channel;
import com.thoughtworks.blipit.domain.Filter;
import javax.jdo.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlipItRepository {

    public List<Channel> retrieveChannelsByCategories(final Set<Category> channelCategorySet) {
        return DataStoreHelper.retrieveAll(Channel.class, new Utils.QueryHandler() {

            public void prepare(Query query) {
                query.declareParameters("java.util.Set channelCategorySet");
                query.setFilter("channelCategorySet.contains(this.category)");
            }

            public Object[] parameters() {
                return new Object[] { channelCategorySet };
            }
        });
    }

    public List<Blip> retrieveBlipsByCategories(Set<Category> blipCategories) {
        final Set<Key> channelKeys = getChannelKeys(blipCategories);
        return DataStoreHelper.retrieveAll(Blip.class, getQueryHandlerForChannels(channelKeys));
    }

    public List<Blip> retrieveBlipsByChannels(final Set<Key> channelKeys) {
        return DataStoreHelper.retrieveAll(Blip.class, getQueryHandlerForChannels(channelKeys));
    }

    private Utils.QueryHandler getQueryHandlerForChannels(final Set<Key> channelKeys) {
        return new Utils.QueryHandler() {

            public void prepare(Query query) {
                query.declareParameters("java.util.Set channelKeys");
                query.setFilter("channelKeys.contains(this.channelKeys)");
            }

            public Object[] parameters() {
                return new Object[] { channelKeys };
            }
        };
    }

    public List<Filter> retrieveFiltersByCategories(Set<Category> filterCategories) {
        final Set<Key> channelKeys = getChannelKeys(filterCategories);
        return DataStoreHelper.retrieveAll(Filter.class, getQueryHandlerForChannels(channelKeys));
    }

    private Set<Key> getChannelKeys(Set<Category> blipCategories) {
        List<Channel> channels = retrieveChannelsByCategories(blipCategories);
        final Set<Key> channelKeys = new HashSet<Key>();
        for (Channel channel : channels) {
            channelKeys.add(channel.getKey());
        }
        return channelKeys;
    }
}
