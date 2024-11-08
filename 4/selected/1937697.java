package com.google.code.guidatv.client.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.code.guidatv.model.Channel;
import com.google.code.guidatv.model.Region;
import com.google.code.guidatv.client.service.ChannelService;

public class ChannelServiceImpl implements ChannelService {

    private List<Region> regions;

    private Map<String, List<String>> region2networks;

    private Map<String, List<Channel>> network2channels;

    private Map<String, Channel> code2channel;

    private Set<String> defaultSelectedChannels;

    public ChannelServiceImpl() {
        regions = new ArrayList<Region>();
        regions.add(new Region("it_IT", "Italia"));
        region2networks = new LinkedHashMap<String, List<String>>();
        List<String> networks = new ArrayList<String>();
        network2channels = new LinkedHashMap<String, List<Channel>>();
        region2networks.put("it_IT", networks);
        addRaiChannels(networks);
        addMediasetChannels(networks);
        addTelecomChannels(networks);
        addSkyChannels(networks);
        addDNEChannels(networks);
        code2channel = new LinkedHashMap<String, Channel>();
        for (List<Channel> chns : network2channels.values()) {
            for (Channel channel : chns) {
                code2channel.put(channel.getCode(), channel);
            }
        }
        defaultSelectedChannels = new HashSet<String>();
        defaultSelectedChannels.add("RaiUno");
        defaultSelectedChannels.add("RaiDue");
        defaultSelectedChannels.add("RaiTre");
        defaultSelectedChannels.add("C5");
        defaultSelectedChannels.add("I1");
        defaultSelectedChannels.add("R4");
        defaultSelectedChannels.add("La7");
    }

    @Override
    public List<Region> getRegions() {
        return regions;
    }

    @Override
    public List<String> getNetworks(String regionCode) {
        return region2networks.get(regionCode);
    }

    @Override
    public Collection<Channel> getChannels() {
        return code2channel.values();
    }

    @Override
    public List<Channel> getChannels(String network) {
        return network2channels.get(network);
    }

    @Override
    public Channel getChannelByCode(String code) {
        return code2channel.get(code);
    }

    @Override
    public Set<String> getDefaultSelectedChannels() {
        return defaultSelectedChannels;
    }

    private void addRaiChannels(List<String> networks) {
        networks.add("Rai");
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("RaiUno", "Rai 1", "generalistic", "it_IT", "Rai"));
        channels.add(new Channel("RaiDue", "Rai 2", "generalistic", "it_IT", "Rai"));
        channels.add(new Channel("RaiTre", "Rai 3", "generalistic", "it_IT", "Rai"));
        channels.add(new Channel("Rai4", "Rai 4", "movies/series", "it_IT", "Rai"));
        channels.add(new Channel("Extra", "Rai 5", "retakes", "it_IT", "Rai"));
        channels.add(new Channel("RaiNews", "Rai News 24", "news", "it_IT", "Rai"));
        channels.add(new Channel("RaiSport1", "Rai Sport 1", "sport", "it_IT", "Rai"));
        channels.add(new Channel("RaiSport2", "Rai Sport 2", "sport", "it_IT", "Rai"));
        channels.add(new Channel("RaiEducational", "Rai Scuola", "education", "it_IT", "Rai"));
        channels.add(new Channel("RaiEDU2", "Rai Storia", "education", "it_IT", "Rai"));
        channels.add(new Channel("Premium", "Rai Premium", "retakes", "it_IT", "Rai"));
        channels.add(new Channel("RaiMovie", "Rai Movie", "movies", "it_IT", "Rai"));
        channels.add(new Channel("RaiGulp", "Rai Gulp", "children", "it_IT", "Rai"));
        channels.add(new Channel("Yoyo", "Rai Yoyo", "children", "it_IT", "Rai"));
        channels.add(new Channel("EuroNews", "Euronews", "news", "it_IT", "Rai"));
        network2channels.put("Rai", channels);
    }

    private void addMediasetChannels(List<String> networks) {
        networks.add("Mediaset");
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("C5", "Canale 5", "generalistic", "it_IT", "Mediaset"));
        channels.add(new Channel("I1", "Italia 1", "generalistic", "it_IT", "Mediaset"));
        channels.add(new Channel("R4", "Rete 4", "generalistic", "it_IT", "Mediaset"));
        channels.add(new Channel("Iris", "Iris", "movies", "it_IT", "Mediaset"));
        channels.add(new Channel("Boing", "Boing", "children", "it_IT", "Mediaset"));
        channels.add(new Channel("KA", "La 5", "women", "it_IT", "Mediaset"));
        channels.add(new Channel("KQ", "Mediaset Extra", "retakes", "it_IT", "Mediaset"));
        channels.add(new Channel("I2", "Italia 2", "young", "it_IT", "Mediaset"));
        network2channels.put("Mediaset", channels);
    }

    private void addTelecomChannels(List<String> networks) {
        networks.add("Telecom Italia Media");
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("La7", "La7", "generalistic", "it_IT", "Telecom Italia Media"));
        channels.add(new Channel("La7d", "La7d", "generalistic", "it_IT", "Telecom Italia Media"));
        network2channels.put("Telecom Italia Media", channels);
    }

    private void addSkyChannels(List<String> networks) {
        networks.add("Sky");
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("6280", "Cielo", "generalistic", "it_IT", "Sky"));
        network2channels.put("Sky", channels);
    }

    private void addDNEChannels(List<String> networks) {
        networks.add("Discovery Networks Europe");
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("DNERealTime", "Real Time", "generalistic", "it_IT", "Discovery Networks Europe"));
        network2channels.put("Discovery Networks Europe", channels);
    }
}
