package org.rsbot.script.methods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rsbot.script.wrappers.GEItemInfo;

/**
 * Obtains information on tradeable items from the Grand Exchange website.
 * 
 * @author Aion
 */
@SuppressWarnings("deprecation")
public class GrandExchange extends MethodProvider {

    private static final String HOST = "http://services.runescape.com";

    private static final String GET = "/m=itemdb_rs/viewitem.ws?obj=";

    private static final Pattern PATTERN = Pattern.compile("(?i)<td><img src=\".+obj_sprite\\.gif\\?id=(\\d+)\" alt=\"(.+)\"");

    GrandExchange() {
        super(null);
    }

    /**
	 * Gets the name of the given item ID. Should not be used.
	 * 
	 * @param itemID
	 *            The item ID to look for.
	 * @return The name of the given item ID or an empty String if unavailable.
	 * @see GrandExchange#lookup(int)
	 */
    public String getItemName(final int itemID) {
        GEItem geItem = lookup(itemID);
        if (geItem != null) {
            return geItem.getName();
        }
        return "";
    }

    /**
	 * Gets the ID of the given item name. Should not be used.
	 * 
	 * @param itemName
	 *            The name of the item to look for.
	 * @return The ID of the given item name or -1 if unavailable.
	 * @see GrandExchange#lookup(java.lang.String)
	 */
    public int getItemID(final String itemName) {
        GEItem geItem = lookup(itemName);
        if (geItem != null) {
            return geItem.getID();
        }
        return -1;
    }

    /**
	 * Collects data for a given item ID from the Grand Exchange website.
	 * 
	 * @param itemID
	 *            The item ID.
	 * @return An instance of GrandExchange.GEItem; <code>null</code> if unable
	 *         to fetch data.
	 */
    public GEItem lookup(final int itemID) {
        try {
            URL url = new URL(GrandExchange.HOST + GrandExchange.GET + itemID);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String input;
            boolean exists = false;
            int i = 0;
            double[] values = new double[4];
            String name = "", examine = "";
            while ((input = br.readLine()) != null) {
                if (input.contains("<div class=\"brown_box main_ge_page") && !exists) {
                    if (!input.contains("vertically_spaced")) {
                        return null;
                    }
                    exists = true;
                    br.readLine();
                    br.readLine();
                    name = br.readLine();
                } else if (input.contains("<img id=\"item_image\" src=\"")) {
                    examine = br.readLine();
                } else if (input.matches("(?i).+ (price|days):</b> .+")) {
                    values[i] = parse(input);
                    i++;
                } else if (input.matches("<div id=\"legend\">")) break;
            }
            return new GEItem(name, examine, itemID, values);
        } catch (IOException ignore) {
        }
        return null;
    }

    /**
	 * Collects data for a given item name from the Grand Exchange website.
	 * 
	 * @param itemName
	 *            The name of the item.
	 * @return An instance of GrandExchange.GEItem; <code>null</code> if unable
	 *         to fetch data.
	 */
    public GEItem lookup(final String itemName) {
        try {
            URL url = new URL(GrandExchange.HOST + "/m=itemdb_rs/results.ws?query=" + itemName + "&price=all&members=");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String input;
            while ((input = br.readLine()) != null) {
                if (input.contains("<div id=\"search_results_text\">")) {
                    input = br.readLine();
                    if (input.contains("Your search for")) {
                        return null;
                    }
                } else if (input.startsWith("<td><img src=")) {
                    Matcher matcher = GrandExchange.PATTERN.matcher(input);
                    if (matcher.find()) {
                        if (matcher.group(2).contains(itemName)) {
                            return lookup(Integer.parseInt(matcher.group(1)));
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private double parse(String str) {
        if (str != null && !str.isEmpty()) {
            str = stripFormatting(str);
            str = str.substring(str.indexOf(58) + 2, str.length());
            str = str.replace(",", "");
            if (!str.endsWith("%")) {
                if (!str.endsWith("k") && !str.endsWith("m")) {
                    return Double.parseDouble(str);
                }
                return Double.parseDouble(str.substring(0, str.length() - 1)) * (str.endsWith("m") ? 1000000 : 1000);
            }
            int k = str.startsWith("+") ? 1 : -1;
            str = str.substring(1);
            return Double.parseDouble(str.substring(0, str.length() - 1)) * k;
        }
        return -1D;
    }

    private String stripFormatting(final String str) {
        if (str != null && !str.isEmpty()) return str.replaceAll("(^[^<]+>|<[^>]+>|<[^>]+$)", "");
        return "";
    }

    public static class GEItem {

        private final String name;

        private final String examine;

        private final int id;

        private final int guidePrice;

        private final double change30;

        private final double change90;

        private final double change180;

        GEItem(String name, String examine, int id, double[] values) {
            this.name = name;
            this.examine = examine;
            this.id = id;
            this.guidePrice = (int) values[0];
            this.change30 = values[1];
            this.change90 = values[2];
            this.change180 = values[3];
        }

        /**
		 * Gets the change in price for the last 30 days of this item.
		 * 
		 * @return The change in price for the last 30 days of this item.
		 */
        public double getChange30Days() {
            return this.change30;
        }

        /**
		 * Gets the change in price for the last 90 days of this item.
		 * 
		 * @return The change in price for the last 90 days of this item.
		 */
        public double getChange90Days() {
            return this.change90;
        }

        /**
		 * Gets the change in price for the last 180 days of this item.
		 * 
		 * @return The change in price for the last 180 days of this item.
		 */
        public double getChange180Days() {
            return this.change180;
        }

        /**
		 * Gets the ID of this item.
		 * 
		 * @return The ID of this item.
		 */
        public int getID() {
            return this.id;
        }

        /**
		 * Gets the market price of this item.
		 * 
		 * @return The market price of this item.
		 */
        public int getGuidePrice() {
            return guidePrice;
        }

        /**
		 * Gets the name of this item.
		 * 
		 * @return The name of this item.
		 */
        public String getName() {
            return this.name;
        }

        /**
		 * Gets the description of this item.
		 * 
		 * @return The description of this item.
		 */
        public String getDescription() {
            return this.examine;
        }

        @Deprecated
        public int getMarketPrice() {
            return guidePrice;
        }

        @Deprecated
        public int getMaxPrice() {
            return guidePrice;
        }

        @Deprecated
        public int getMinPrice() {
            return guidePrice;
        }
    }

    @Deprecated
    public GEItemInfo loadItemInfo(int i) {
        return null;
    }
}
