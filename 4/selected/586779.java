package ijaux.compat;

import ij.ImagePlus;
import ijaux.hypergeom.index.BaseIndex;
import ijaux.hypergeom.index.Indexing;

public class ImagePlusIndex extends BaseIndex {

    /**
	 * 
	 */
    private static final long serialVersionUID = -6361514738049276412L;

    private ImagePlusCoords impc;

    public ImagePlusIndex(ImagePlus imp) {
        super(imp.getDimensions());
        impc = new ImagePlusCoords(imp);
    }

    @Override
    public int translate(int[] x) {
        impc.translate(x);
        return super.translate(x);
    }

    @Override
    public int translateTo(int[] x) {
        impc.translateTo(x);
        return super.translateTo(x);
    }

    public void setPositionGUI() {
        impc.setPositionGUI();
    }

    class ImagePlusCoords {

        final int width = 0, height = 1, channel = 2, frame = 3, slice = 4;

        int ndim = 5;

        ImagePlus imp;

        public ImagePlusCoords(ImagePlus imp) {
            ndim = imp.getNDimensions();
            this.imp = imp;
        }

        void translateTo(int[] x) {
            imp.setPositionWithoutUpdate(x[channel]++, x[frame]++, x[slice]++);
        }

        void translate(int[] x) {
            final int aslice = imp.getCurrentSlice() - 1 + x[slice];
            final int aframe = imp.getFrame() + x[frame];
            final int achannel = imp.getChannel() + x[channel];
            imp.setPositionWithoutUpdate(achannel, aframe, aslice);
        }

        public void setPositionGUI() {
            imp.setPosition(coords[channel]++, coords[frame]++, coords[slice]++);
        }
    }
}
