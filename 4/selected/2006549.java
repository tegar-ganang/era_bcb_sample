package sun.tools.native2ascii.resources;

import java.util.ListResourceBundle;

public class MsgNative2ascii_ja extends ListResourceBundle {

    public Object[][] getContents() {
        return new Object[][] { { "err.bad.arg", "-encoding には、引数が必要です。" }, { "err.cannot.read", "{0} を読み込むことができません。" }, { "err.cannot.write", "{0} に書き込むことができません。" }, { "usage", "使い方: native2ascii" + " [-reverse] [-encoding encoding] [inputfile [outputfile]]" } };
    }
}
