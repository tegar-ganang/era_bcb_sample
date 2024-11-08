package net.sf.mustang.wm.ctxtool;

import org.webmacro.Context;
import org.webmacro.ContextTool;
import net.sf.mustang.Mustang;

public class Message extends ContextTool {

    public Object init(Context context) {
        return new MessageDelegate(context);
    }

    public class MessageDelegate {

        String channel, lang;

        Context context;

        public MessageDelegate(Context context) {
            this.context = context;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String get(String key) {
            return MessageManager.getInstance().get(context, channel, lang, key);
        }

        public String getChannel() {
            return channel;
        }

        public String getLang() {
            if (lang == null) lang = Mustang.getConf().get("//message/@default-language");
            return lang;
        }
    }
}
