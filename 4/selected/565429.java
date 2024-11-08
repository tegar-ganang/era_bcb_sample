package be.lassi.domain;

import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;

public class ShowBuilder {

    public static Show example() {
        return build(new DirtyStub(), 60, 12, 60, "");
    }

    public static Show build(final int numberOfChannels, final int numberOfSubmasters, final int numberOfDimmers, final String name) {
        return build(new DirtyStub(), numberOfChannels, numberOfSubmasters, numberOfDimmers, name);
    }

    public static Show build() {
        return build(new DirtyStub(), 0, 0, 0, "");
    }

    public static Show build(final Dirty dirty) {
        return build(dirty, 0, 0, 0, "");
    }

    public static Show build(final Dirty dirty, final int numberOfChannels, final int numberOfSubmasters, final int numberOfDimmers, final String name) {
        Show show = new Show(dirty, name);
        buildChannels(show, numberOfChannels);
        buildDimmers(show, numberOfDimmers);
        buildSubmasters(show, numberOfSubmasters, numberOfChannels);
        show.contructPart2();
        return show;
    }

    private static void buildChannels(final Show show, final int numberOfChannels) {
        for (int i = 0; i < numberOfChannels; i++) {
            Channel channel = new Channel(show.getDirty(), i, "Channel " + (i + 1));
            show.getChannels().add(channel);
        }
    }

    private static void buildDimmers(final Show show, final int numberOfDimmers) {
        for (int i = 0; i < numberOfDimmers; i++) {
            String name = Dimmer.getDefaultName(i);
            Dimmer dimmer = new Dimmer(show.getDirty(), i, name);
            show.getDimmers().add(dimmer);
        }
    }

    private static void buildSubmasters(final Show show, final int numberOfSubmasters, final int numberOfChannels) {
        for (int i = 0; i < numberOfSubmasters; i++) {
            final String name = Submaster.getDefaultName(i);
            final Submaster submaster = new Submaster(show.getDirty(), i, numberOfChannels, name);
            show.getSubmasters().add(submaster);
        }
    }
}
