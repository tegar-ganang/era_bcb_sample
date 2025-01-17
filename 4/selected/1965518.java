package com.thoughtworks.blipit.domain;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.thoughtworks.blipit.Utils;
import com.thoughtworks.blipit.filters.BlipsFilter;
import com.thoughtworks.blipit.filters.DistanceFilter;
import com.thoughtworks.blipit.persistence.BlipItRepository;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PersistenceCapable
public class Filter {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private GeoPt geoPoint;

    @Persistent
    private float radius;

    @Persistent
    private Set<Key> channelKeys;

    public Filter() {
        this(null, null, 2, new HashSet<Key>());
    }

    public Filter(Key key, GeoPt geoPoint, float radius, Set<Key> channelKeys) {
        this.key = key;
        this.geoPoint = geoPoint;
        this.radius = radius;
        this.channelKeys = channelKeys;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public GeoPt getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPt geoPoint) {
        this.geoPoint = geoPoint;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Set<Key> getChannelKeys() {
        return channelKeys;
    }

    public void setChannelKeys(Set<Key> channelKeys) {
        this.channelKeys = channelKeys;
    }

    private void prepareChannelKeys() {
        Set<Key> channelKeys = new HashSet<Key>();
        if (Utils.isNotEmpty(this.channelKeys)) {
            for (Key channelKey : this.channelKeys) {
                channelKeys.add(KeyFactory.createKey(Channel.class.getSimpleName(), channelKey.getId()));
            }
        }
        this.channelKeys = channelKeys;
    }

    public void prepareKeys() {
        if (key != null) {
            this.key = KeyFactory.createKey(Filter.class.getSimpleName(), key.getId());
        }
        prepareChannelKeys();
    }

    public List<Blip> filterBlips(BlipItRepository blipItRepository) {
        List<Blip> filteredBlips = blipItRepository.retrieveBlipsByChannels(channelKeys);
        if (filteredBlips != null) {
            List<BlipsFilter> blipsFilters = Arrays.<BlipsFilter>asList(new DistanceFilter(geoPoint, radius));
            for (BlipsFilter blipsFilter : blipsFilters) {
                blipsFilter.doFilter(filteredBlips);
            }
        }
        return filteredBlips;
    }
}
