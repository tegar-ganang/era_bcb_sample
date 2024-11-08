package tcl.lang;

/** A hook for outside classes to access the non-publilc fields or methods. */
public class Hook {

    public static Channel getChannel(Interp it_, String chanName_) {
        return TclIO.getChannel(it_, chanName_);
    }

    public static void registerChannel(Interp it_, Channel chan_) {
        TclIO.registerChannel(it_, chan_);
    }

    public static void setScriptFile(Interp it_, String scriptFile_) {
        it_.scriptFile = scriptFile_;
    }

    public static String getScriptFile(Interp it_) {
        return it_.scriptFile;
    }
}
