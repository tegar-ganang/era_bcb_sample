package coffeeviewer.source.main;

public class LLVersionInfo {

    private String ChannelName;

    private String llversionmajor;

    private String llversionminor;

    private String llversionpatch;

    private String llversionbuild;

    public LLVersionInfo() {
        ChannelName = "CoffeeViewer";
        llversionmajor = "0";
        llversionminor = "0";
        llversionpatch = "0";
        llversionbuild = "0";
    }

    public String getChannelName() {
        return ChannelName;
    }

    public String getLlversionmajor() {
        return llversionmajor;
    }

    public String getLlversionminor() {
        return llversionminor;
    }

    public String getLlversionpatch() {
        return llversionpatch;
    }

    public String getLlversionbuild() {
        return llversionbuild;
    }
}
