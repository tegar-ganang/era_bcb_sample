package utils;

/**
 *
 * @author Bastian Hinterleitner
 */
public class Part {

    /**
     *The parts the xml file has
     */
    public Part[] part = new Part[9999];

    /**
     *number of parts the xml file has
     */
    private int parts = 0;

    /**
         *name of the part
         */
    public String name;

    /**
         *info the part contains (value of the part)
         */
    public String info = "";

    /**
         * level of deepness the part is (how many parts are above this one)
         */
    private int deep;

    /**
         *Constructor
         * @param name name of the part
         * @param d deepness of the part (how many parts are above this one)
         */
    protected Part(String name, int d) {
        deep = d;
        this.name = name;
    }

    /**
         *Constructor
         * @param name name of the part
         * @param info value of the part
         * @param d deepness of the part (how many parts are above this one)
         */
    protected Part(String name, String info, int d) {
        deep = d;
        this.name = name;
        this.info = info;
    }

    /**
         *adds another Part to this one
         * @param name name of the Part to add
         * @return returns the added Part
         */
    public Part addPart(String name) {
        part[parts] = new Part(name, deep + 1);
        Part p = part[parts];
        parts++;
        return p;
    }

    /**
         *dds another Part to this one
         * @param name name of the Part to add
         * @param info value of the Part to add
         * @return returns the added Part
         */
    public Part addPart(String name, String info) {
        part[parts] = new Part(name, info, deep + 1);
        Part p = part[parts];
        parts++;
        return p;
    }

    /**
         *removes a part from the xml
         * @param i number of the part to remove (call getParts() to know which one to remove)
         * @return return whether successful
         */
    public boolean remPart(int i) {
        try {
            for (int j = i; j < parts; j++) {
                part[j] = part[j + 1];
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
        *lists all the Parts added in a String array (without values)
        * @return String array of names of all parts added
        */
    public String[] getParts() {
        String[] str;
        if (parts > 0) {
            str = new String[parts];
            for (int i = 0; i < str.length; i++) {
                str[i] = part[i].name;
            }
        } else {
            str = new String[] { info };
        }
        return str;
    }

    /**
         *used to create a String from all subParts and this one (usually only called by an EasyXml or another Part)
         * @return the String created
         */
    public String save() {
        String str = "";
        for (int v = 0; v < deep; v++) {
            str += "    ";
        }
        str += "<" + name + ">";
        if (parts == 0) {
            str += info;
        } else {
            str += "\n";
            for (int i = 0; i < parts; i++) {
                str += part[i].save();
            }
            for (int v = 0; v < deep; v++) {
                str += "    ";
            }
        }
        str += "</" + name + ">\n";
        return str;
    }
}
