package sun.tools.native2ascii.resources;

import java.util.ListResourceBundle;

public class MsgNative2ascii_zh_CN extends ListResourceBundle {

    public Object[][] getContents() {
        return new Object[][] { { "err.bad.arg", "-encoding 需要参数" }, { "err.cannot.read", "无法读取 {0}。" }, { "err.cannot.write", "无法写入 {0}。" }, { "usage", "用法：native2ascii" + " [-reverse] [-encoding 编码] [输入文件 [输出文件]]" } };
    }
}
