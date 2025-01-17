package pck_tap.beerxml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import pck_tap.alg.Util;
import pck_tap.beerxml.recipe.Fermentable;
import pck_tap.beerxml.recipe.Fermentation;
import pck_tap.beerxml.recipe.Fermentation_Step;
import pck_tap.beerxml.recipe.Hop;
import pck_tap.beerxml.recipe.Mash;
import pck_tap.beerxml.recipe.Mash_Step;
import pck_tap.beerxml.recipe.Misc;
import pck_tap.beerxml.recipe.Water;
import pck_tap.beerxml.recipe.Yeast;
import pck_tap.current.CurrentObjectMemory;

public class XMLWriterStax {

    private CurrentObjectMemory com;

    int read = 0;

    int close = 0;

    String offset = "";

    boolean isNext = false;

    boolean isParentElement = true;

    public static final String CRLF = "\r\n";

    public void Save(String fileName, BeerXml beerxml, CurrentObjectMemory com) throws XMLStreamException, IOException {
        this.com = com;
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xtw = null;
        String ext = fileName.substring(fileName.indexOf("."));
        String newName = fileName.replace(ext, ".old");
        doRename(fileName, newName);
        FileWriter fw = new FileWriter(fileName);
        xtw = xof.createXMLStreamWriter(fw);
        fw.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        xtw.writeCharacters(CRLF);
        String comm = "BeerXML Format 1.0 - Generated by " + com.getSettings().getApplicationName().concat(" ").concat(com.getSettings().getApplicationVersion()) + "  - see www.bier-brouwen.nl";
        xtw.writeComment(comm);
        xtw.writeCharacters(CRLF);
        String PItext = "type='text/xsl' href='beerxml.xsl'";
        xtw.writeProcessingInstruction("xml-stylesheet", PItext);
        xtw.writeCharacters(CRLF);
        writeStartElement(xtw, "RECIPES", true);
        writeStartElement(xtw, "TAP_VERSION", com.getSettings().getApplicationVersion(), false);
        writeStartElement(xtw, "RECIPE", true);
        recipe(xtw, beerxml);
        writeStartElement(xtw, "EQUIPMENT", true);
        equipment(xtw, beerxml);
        writeEndElement(xtw);
        close--;
        writeStartElement(xtw, "STYLE", true);
        style(xtw, beerxml);
        writeEndElement(xtw);
        close--;
        hops(xtw, beerxml);
        fermentables(xtw, beerxml);
        miscs(xtw, beerxml);
        waters(xtw, beerxml);
        yeasts(xtw, beerxml);
        mashs(xtw, beerxml);
        fermentations(xtw, beerxml);
        try {
            for (int i = 0; i < close; i++) {
                writeEndElement(xtw);
            }
        } catch (XMLStreamException e) {
            e = null;
        }
        xtw.writeEndDocument();
        xtw.flush();
        xtw.close();
        fw.close();
    }

    public void writeStartElement(XMLStreamWriter xtw, String s, String value, Boolean event) throws XMLStreamException {
        if (!Util.isNull(value)) {
            read++;
            xtw.writeCharacters(offset);
            xtw.writeStartElement(s);
            {
                isNext = true;
                if (event) {
                    xtw.writeCharacters(CRLF);
                    offset += "  ";
                } else {
                    isParentElement = false;
                }
            }
            if (!Util.isNull(value)) {
                xtw.writeCharacters(value);
                writeEndElement(xtw);
            } else {
                close++;
            }
        }
    }

    public void writeStartElement(XMLStreamWriter xtw, String s, Boolean event) throws XMLStreamException {
        read++;
        xtw.writeCharacters(offset);
        xtw.writeStartElement(s);
        {
            isNext = true;
            if (event) {
                xtw.writeCharacters(CRLF);
                offset += "  ";
            } else {
                isParentElement = false;
            }
        }
        close++;
    }

    public void writeStartElement(XMLStreamWriter xtw, String s, Double value, Boolean event) throws XMLStreamException {
        if (!Util.isNull(value)) {
            read++;
            xtw.writeCharacters(offset);
            xtw.writeStartElement(s);
            {
                isNext = true;
                if (event) {
                    xtw.writeCharacters(CRLF);
                    offset += "  ";
                } else {
                    isParentElement = false;
                }
            }
            if (!Util.isNull(value)) {
                if (Util.isNull(value)) {
                    xtw.writeCharacters("");
                } else {
                    xtw.writeCharacters(Util.dbl2str(value));
                }
                writeEndElement(xtw);
            } else {
                close++;
            }
        }
    }

    public void writeStartElement(XMLStreamWriter xtw, String s, Boolean value, Boolean event) throws XMLStreamException {
        if (!Util.isNull(value)) {
            read++;
            xtw.writeCharacters(offset);
            xtw.writeStartElement(s);
            {
                isNext = true;
                if (event) {
                    xtw.writeCharacters(CRLF);
                    offset += "  ";
                } else {
                    isParentElement = false;
                }
            }
            xtw.writeCharacters(Util.bln2str(value));
            this.writeEndElement(xtw);
            close++;
        }
    }

    public void writeEndElement(XMLStreamWriter pXMLStreamWriter) throws XMLStreamException {
        {
            if (isParentElement) {
                if (offset.length() > 1) offset = offset.substring(0, offset.length() - 2);
                pXMLStreamWriter.writeCharacters(offset);
            }
            isParentElement = true;
        }
        pXMLStreamWriter.writeEndElement();
        pXMLStreamWriter.writeCharacters(CRLF);
        read--;
    }

    private void recipe(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(beerxml.getRecipe().getName(), "<unknown>"), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(beerxml.getRecipe().getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "TAP_NUMBER", Util.format0(Util.nvl(beerxml.getRecipe().getTapNumber())), false);
        writeStartElement(xtw, "TYPE", Util.nvl(beerxml.getRecipe().getType()), false);
        writeStartElement(xtw, "BREWER", Util.nvl(beerxml.getRecipe().getBrewer(), "<unknown>"), false);
        writeStartElement(xtw, "ASST_BREWER", beerxml.getRecipe().getAsst_Brewer(), false);
        writeStartElement(xtw, "BATCH_SIZE", Util.nvl(beerxml.getRecipe().getBatch_Size()), false);
        writeStartElement(xtw, "BOIL_SIZE", Util.nvl(beerxml.getRecipe().getBoil_Size()), false);
        writeStartElement(xtw, "BOIL_TIME", Util.nvl(beerxml.getRecipe().getBoil_Time()), false);
        writeStartElement(xtw, "EFFICIENCY", Util.nvl(beerxml.getRecipe().getEfficiency()), false);
        writeStartElement(xtw, "NOTES", beerxml.getRecipe().getNotes(), false);
        writeStartElement(xtw, "TASTE_NOTES", beerxml.getRecipe().getTaste_Notes(), false);
        writeStartElement(xtw, "TASTE_RATING", beerxml.getRecipe().getTaste_Rating(), false);
        writeStartElement(xtw, "OG", Util.format0x000(beerxml.getRecipe().getOg()), false);
        Double fg = Util.nin(beerxml.getRecipe().getFg());
        Double fgc = Util.nvl(com.getBeerXml().getRecipe().getTapCalcJacquesBertens().getEindSG() / 1000);
        writeStartElement(xtw, "FG", Util.format0x000(Util.nvl(fg, fgc)), false);
        writeStartElement(xtw, "FERMENTATION_STAGES", beerxml.getRecipe().getFermentation_Stages(), false);
        writeStartElement(xtw, "PRIMARY_AGE", beerxml.getRecipe().getPrimary_Age(), false);
        writeStartElement(xtw, "PRIMARY_TEMP", beerxml.getRecipe().getPrimary_Temp(), false);
        writeStartElement(xtw, "SECONDARY_AGE", beerxml.getRecipe().getSecondary_Age(), false);
        writeStartElement(xtw, "SECONDARY_TEMP", beerxml.getRecipe().getSecondary_Temp(), false);
        writeStartElement(xtw, "TERTIARY_AGE", beerxml.getRecipe().getTertiary_Age(), false);
        writeStartElement(xtw, "TERTIARY_TEMP", beerxml.getRecipe().getTertiary_Temp(), false);
        writeStartElement(xtw, "AGE", beerxml.getRecipe().getAge(), false);
        writeStartElement(xtw, "AGE_TEMP", beerxml.getRecipe().getAge_Temp(), false);
        writeStartElement(xtw, "DATE", beerxml.getRecipe().getDate(), false);
        writeStartElement(xtw, "CARBONATION", beerxml.getRecipe().getCarbonation(), false);
        writeStartElement(xtw, "FORCED_CARBONATION", beerxml.getRecipe().getForced_Carbonation(), false);
        writeStartElement(xtw, "PRIMING_SUGAR_NAME", beerxml.getRecipe().getPriming_Sugar_Name(), false);
        writeStartElement(xtw, "CARBONATION_TEMP", beerxml.getRecipe().getCarbonation_Temp(), false);
        writeStartElement(xtw, "PRIMING_SUGAR_EQUIV", beerxml.getRecipe().getPriming_Sugar_Equiv(), false);
        writeStartElement(xtw, "KEG_PRIMING_FACTOR", beerxml.getRecipe().getKeg_Priming_Factor(), false);
        writeStartElement(xtw, "IBU", beerxml.getRecipe().getIbu(), false);
        writeStartElement(xtw, "IBU_METHOD", beerxml.getRecipe().getIbu_Method(), false);
    }

    private void equipment(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(beerxml.getRecipe().getEquipment().getName(), "<unknown>"), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(beerxml.getRecipe().getEquipment().getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "BOIL_SIZE", Util.nvl(beerxml.getRecipe().getEquipment().getBoil_Size()), false);
        writeStartElement(xtw, "BATCH_SIZE", Util.nvl(beerxml.getRecipe().getEquipment().getBatch_Size()), false);
        writeStartElement(xtw, "TUN_VOLUME", beerxml.getRecipe().getEquipment().getTun_Volume(), false);
        writeStartElement(xtw, "TUN_WEIGHT", beerxml.getRecipe().getEquipment().getTun_Weight(), false);
        writeStartElement(xtw, "TUN_SPECIFIC_HEAT", beerxml.getRecipe().getEquipment().getTun_Specific_Heat(), false);
        writeStartElement(xtw, "TOP_UP_WATER", beerxml.getRecipe().getEquipment().getTop_Up_Water(), false);
        writeStartElement(xtw, "TRUB_CHILLER_LOSS", beerxml.getRecipe().getEquipment().getTrub_Chiller_Loss(), false);
        writeStartElement(xtw, "EVAP_RATE", beerxml.getRecipe().getEquipment().getEvap_Rate(), false);
        writeStartElement(xtw, "BOIL_TIME", beerxml.getRecipe().getEquipment().getBoil_Time(), false);
        writeStartElement(xtw, "CALC_BOIL_VOLUME", beerxml.getRecipe().getEquipment().getCalc_Boil_Volume(), false);
        writeStartElement(xtw, "LAUTER_DEADSPACE", beerxml.getRecipe().getEquipment().getLauter_Deadspace(), false);
        writeStartElement(xtw, "TOP_UP_KETTLE", beerxml.getRecipe().getEquipment().getTop_Up_Kettle(), false);
        writeStartElement(xtw, "HOP_UTILIZATION", beerxml.getRecipe().getEquipment().getHop_Utilization(), false);
        writeStartElement(xtw, "NOTES", beerxml.getRecipe().getEquipment().getNotes(), false);
    }

    private void style(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(beerxml.getRecipe().getStyle().getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(beerxml.getRecipe().getStyle().getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "CATEGORY", Util.nvl(beerxml.getRecipe().getStyle().getCategory()), false);
        writeStartElement(xtw, "CATEGORY_NUMBER", Util.nvl(beerxml.getRecipe().getStyle().getCategory_Number()), false);
        writeStartElement(xtw, "STYLE_LETTER", Util.nvl(beerxml.getRecipe().getStyle().getStyle_Letter()), false);
        writeStartElement(xtw, "STYLE_GUIDE", Util.nvl(beerxml.getRecipe().getStyle().getStyle_Guide()), false);
        writeStartElement(xtw, "TYPE", Util.nvl(beerxml.getRecipe().getStyle().getType()), false);
        writeStartElement(xtw, "OG_MIN", Util.format0x000(Util.nvl(beerxml.getRecipe().getStyle().getOg_Min())), false);
        writeStartElement(xtw, "OG_MAX", Util.format0x000(Util.nvl(beerxml.getRecipe().getStyle().getOg_Max())), false);
        writeStartElement(xtw, "FG_MIN", Util.format0x000(Util.nvl(beerxml.getRecipe().getStyle().getFg_Min())), false);
        writeStartElement(xtw, "FG_MAX", Util.format0x000(Util.nvl(beerxml.getRecipe().getStyle().getFg_Max())), false);
        writeStartElement(xtw, "IBU_MIN", Util.nvl(beerxml.getRecipe().getStyle().getIbu_Min()), false);
        writeStartElement(xtw, "IBU_MAX", Util.nvl(beerxml.getRecipe().getStyle().getIbu_Max()), false);
        writeStartElement(xtw, "COLOR_MIN", Util.nvl(beerxml.getRecipe().getStyle().getColor_Min()), false);
        writeStartElement(xtw, "COLOR_MAX", Util.nvl(beerxml.getRecipe().getStyle().getColor_Max()), false);
        writeStartElement(xtw, "CARB_MIN", beerxml.getRecipe().getStyle().getCarb_Min(), false);
        writeStartElement(xtw, "CARB_MAX", beerxml.getRecipe().getStyle().getCarb_Max(), false);
        writeStartElement(xtw, "ABV_MIN", beerxml.getRecipe().getStyle().getAbv_Min(), false);
        writeStartElement(xtw, "ABV_MAX", beerxml.getRecipe().getStyle().getAbv_Max(), false);
        writeStartElement(xtw, "NOTES", beerxml.getRecipe().getStyle().getNotes(), false);
        writeStartElement(xtw, "PROFILE", beerxml.getRecipe().getStyle().getProfile(), false);
        writeStartElement(xtw, "INGREDIENTS", beerxml.getRecipe().getStyle().getIngredients(), false);
        writeStartElement(xtw, "EXAMPLES", beerxml.getRecipe().getStyle().getExamples(), false);
    }

    private void hops(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "HOPS", true);
        for (int i = 0; i < beerxml.getRecipe().getHops().size(); i++) {
            writeStartElement(xtw, "HOP", true);
            hop(xtw, beerxml, i);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
    }

    private void hop(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "ALPHA", Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getAlpha()), false);
        writeStartElement(xtw, "AMOUNT", Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getAmount()), false);
        writeStartElement(xtw, "USE", Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getUse()), false);
        writeStartElement(xtw, "TIME", Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getTime()), false);
        writeStartElement(xtw, "NOTES", ((Hop) beerxml.getRecipe().getHops().get(h)).getNotes(), false);
        writeStartElement(xtw, "TYPE", Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getType()), false);
        writeStartElement(xtw, "FORM", Util.nvl(Util.nvl(((Hop) beerxml.getRecipe().getHops().get(h)).getForm()).replace("Bloem", "Leaf")), false);
        writeStartElement(xtw, "BETA", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getBeta()), false);
        writeStartElement(xtw, "HSI", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getHsi()), false);
        writeStartElement(xtw, "ORIGIN", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getOrigin()), false);
        writeStartElement(xtw, "SUBSTITUTES", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getSubstitutes()), false);
        writeStartElement(xtw, "HUMULENE", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getHumulene()), false);
        writeStartElement(xtw, "CARYOPHYLLENE", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getCaryophyllene()), false);
        writeStartElement(xtw, "COHUMULONE", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getCohumulone()), false);
        writeStartElement(xtw, "MYRCENE", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getMyrcene()), false);
        writeStartElement(xtw, "TAP_HOPBAG", ((Hop) beerxml.getRecipe().getHops().get(h)).getTap_HopBag(), false);
        writeStartElement(xtw, "TAP_IBU", Util.nin(((Hop) beerxml.getRecipe().getHops().get(h)).getTap_Ibu()), false);
    }

    private void fermentables(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "FERMENTABLES", true);
        for (int i = 0; i < beerxml.getRecipe().getFermentables().size(); i++) {
            writeStartElement(xtw, "FERMENTABLE", true);
            fermentable(xtw, beerxml, i);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
    }

    private void fermentable(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "NAME", (Util.nvl(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getName())), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "TYPE", (Util.nvl(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getType())), false);
        writeStartElement(xtw, "AMOUNT", (Util.nvl(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getAmount())), false);
        writeStartElement(xtw, "YIELD", (Util.nvl(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getYield())), false);
        writeStartElement(xtw, "POTENTIAL", Util.nin(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getPotential()), false);
        writeStartElement(xtw, "COLOR", (Util.nvl(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getColor())), false);
        writeStartElement(xtw, "ADD_AFTER_BOIL", ((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getAdd_After_Boil(), false);
        writeStartElement(xtw, "ORIGIN", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getOrigin())), false);
        writeStartElement(xtw, "SUPPLIER", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getSupplier())), false);
        writeStartElement(xtw, "NOTES", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getNotes())), false);
        writeStartElement(xtw, "COARSE_FINE_DIFF", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getCoarse_Fine_Diff())), false);
        writeStartElement(xtw, "MOISTURE", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getMoisture())), false);
        writeStartElement(xtw, "DIASTATIC_POWER", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getDiastatic_Power())), false);
        writeStartElement(xtw, "PROTEIN", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getProtein())), false);
        writeStartElement(xtw, "MAX_IN_BATCH", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getMax_In_Batch())), false);
        writeStartElement(xtw, "RECOMMEND_MASH", (((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getRecommend_Mash()), false);
        writeStartElement(xtw, "IBU_GAL_PER_LB", Util.nin((((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getIbu_Gal_Per_Lb())), false);
        writeStartElement(xtw, "TAP_PERCENT", Util.nin(((Fermentable) beerxml.getRecipe().getFermentables().get(h)).getTap_Percent()), false);
    }

    private void miscs(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "MISCS", true);
        for (int i = 0; i < beerxml.getRecipe().getMiscs().size(); i++) {
            writeStartElement(xtw, "MISC", true);
            misc(xtw, beerxml, i);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
    }

    private void misc(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(((Misc) beerxml.getRecipe().getMiscs().get(h)).getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Misc) beerxml.getRecipe().getMiscs().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "TYPE", Util.nvl(((Misc) beerxml.getRecipe().getMiscs().get(h)).getType()), false);
        writeStartElement(xtw, "USE", Util.nvl(((Misc) beerxml.getRecipe().getMiscs().get(h)).getUse()), false);
        writeStartElement(xtw, "TIME", Util.nvl(((Misc) beerxml.getRecipe().getMiscs().get(h)).getTime()), false);
        writeStartElement(xtw, "AMOUNT", Util.nvl(((Misc) beerxml.getRecipe().getMiscs().get(h)).getAmount()), false);
        writeStartElement(xtw, "AMOUNT_IS_WEIGHT", ((Misc) beerxml.getRecipe().getMiscs().get(h)).getAmount_Is_Weight(), false);
        writeStartElement(xtw, "USE_FOR", ((Misc) beerxml.getRecipe().getMiscs().get(h)).getUse_For(), false);
        writeStartElement(xtw, "NOTES", ((Misc) beerxml.getRecipe().getMiscs().get(h)).getNotes(), false);
    }

    private void waters(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "WATERS", true);
        for (int i = 0; i < beerxml.getRecipe().getWaters().size(); i++) {
            writeStartElement(xtw, "WATER", true);
            water(xtw, beerxml, i);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
    }

    private void water(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "AMOUNT", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getAmount()), false);
        writeStartElement(xtw, "CALCIUM", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getCalcium()), false);
        writeStartElement(xtw, "BICARBONATE", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getBicarbonate()), false);
        writeStartElement(xtw, "SULFATE", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getSulfate()), false);
        writeStartElement(xtw, "CHLORIDE", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getChloride()), false);
        writeStartElement(xtw, "SODIUM", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getSodium()), false);
        writeStartElement(xtw, "MAGNESIUM", Util.nvl(((Water) beerxml.getRecipe().getWaters().get(h)).getMagnesium()), false);
        writeStartElement(xtw, "PH", Util.nin(((Water) beerxml.getRecipe().getWaters().get(h)).getPh()), false);
        writeStartElement(xtw, "NOTES", Util.nin(((Water) beerxml.getRecipe().getWaters().get(h)).getNotes()), false);
    }

    private void yeasts(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        writeStartElement(xtw, "YEASTS", true);
        for (int i = 0; i < beerxml.getRecipe().getYeasts().size(); i++) {
            writeStartElement(xtw, "YEAST", true);
            yeast(xtw, beerxml, i);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
    }

    private void yeast(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "NAME", Util.nvl(((Yeast) beerxml.getRecipe().getYeasts().get(h)).getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Yeast) beerxml.getRecipe().getYeasts().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "TYPE", Util.nvl(((Yeast) beerxml.getRecipe().getYeasts().get(h)).getType()), false);
        writeStartElement(xtw, "FORM", Util.nvl(((Yeast) beerxml.getRecipe().getYeasts().get(h)).getForm()), false);
        writeStartElement(xtw, "AMOUNT", Util.nvl(((Yeast) beerxml.getRecipe().getYeasts().get(h)).getAmount()), false);
        writeStartElement(xtw, "AMOUNT_IS_WEIGHT", (((Yeast) beerxml.getRecipe().getYeasts().get(h)).getAmount_Is_Weight()), false);
        writeStartElement(xtw, "LABORATORY", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getLaboratory())), false);
        writeStartElement(xtw, "PRODUCT_ID", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getProduct_Id())), false);
        writeStartElement(xtw, "MIN_TEMPERATURE", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getMin_Temperature())), false);
        writeStartElement(xtw, "MAX_TEMPERATURE", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getMax_Temperature())), false);
        writeStartElement(xtw, "FLOCCULATION", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getFlocculation())), false);
        writeStartElement(xtw, "ATTENUATION", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getAttenuation())), false);
        writeStartElement(xtw, "NOTES", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getNotes())), false);
        writeStartElement(xtw, "BEST_FOR", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getBest_For())), false);
        writeStartElement(xtw, "TIMES_CULTURED", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getTimes_Cultured())), false);
        writeStartElement(xtw, "MAX_REUSE", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getMax_Reuse())), false);
        writeStartElement(xtw, "ADD_TO_SECONDARY", (((Yeast) beerxml.getRecipe().getYeasts().get(h)).getAdd_To_Secondary()), false);
        writeStartElement(xtw, "TAP_USE", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getTap_Use())), false);
        writeStartElement(xtw, "TAP_ATTENUATION_MAX", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getTaAttenuationMax())), false);
        writeStartElement(xtw, "TAP_ATTENUATION_MIN", Util.nin((((Yeast) beerxml.getRecipe().getYeasts().get(h)).getTaAttenuationMin())), false);
    }

    private void mashs(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        for (int i = 0; i < beerxml.getRecipe().getMash().size(); i++) {
            mash(xtw, beerxml, i);
        }
    }

    private void mash(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "MASH", true);
        writeStartElement(xtw, "NAME", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "GRAIN_TEMP", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getGrain_Temp()), false);
        writeStartElement(xtw, "NOTES", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getNotes()), false);
        writeStartElement(xtw, "TUN_TEMP", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getTun_Temp()), false);
        writeStartElement(xtw, "SPARGE_TEMP", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getSparge_Temp()), false);
        writeStartElement(xtw, "PH", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getPh()), false);
        writeStartElement(xtw, "TUN_WEIGHT", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getTun_Weight()), false);
        writeStartElement(xtw, "TUN_SPECIFIC_HEAT", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getTun_Specific_Heat()), false);
        writeStartElement(xtw, "EQUIP_ADJUST", Util.nvl(((Mash) beerxml.getRecipe().getMash().get(h)).getEquip_Adjust()), false);
        writeStartElement(xtw, "MASH_STEPS", true);
        for (int i = 0; i < ((Mash) beerxml.getRecipe().getMash().get(h)).getMash_Steps().size(); i++) {
            writeStartElement(xtw, "MASH_STEP", true);
            mash_step(xtw, beerxml, i, h);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
        writeEndElement(xtw);
    }

    private void mash_step(XMLStreamWriter xtw, BeerXml beerxml, int h, int CurrentSelectedMashIndex) throws XMLStreamException {
        Mash m = ((Mash) beerxml.getRecipe().getMash().get(CurrentSelectedMashIndex));
        Mash_Step ms = (Mash_Step) m.getMash_Steps().get(h);
        writeStartElement(xtw, "NAME", Util.nvl(ms.getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(ms.getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "TYPE", Util.nvl(ms.getType()), false);
        writeStartElement(xtw, "INFUSE_AMOUNT", Util.nvl(ms.getInfuse_Amount()), false);
        writeStartElement(xtw, "STEP_TEMP", Util.nvl(ms.getStep_Temp()), false);
        writeStartElement(xtw, "STEP_TIME", Util.nvl(ms.getStep_Time()), false);
        writeStartElement(xtw, "RAMP_TIME", (ms.getRamp_Time()), false);
        writeStartElement(xtw, "END_TEMP", (ms.getEnd_Temp()), false);
    }

    private void fermentations(XMLStreamWriter xtw, BeerXml beerxml) throws XMLStreamException {
        for (int i = 0; i < beerxml.getRecipe().getFermentation().size(); i++) {
            try {
                try {
                    if (!Util.isNull(((Fermentation) beerxml.getRecipe().getFermentation().get(i)).getName())) {
                        fermentation(xtw, beerxml, i);
                    }
                } catch (XMLStreamException e) {
                }
            } catch (Exception e) {
            }
        }
    }

    private void fermentation(XMLStreamWriter xtw, BeerXml beerxml, int h) throws XMLStreamException {
        writeStartElement(xtw, "FERMENTATION", true);
        writeStartElement(xtw, "NAME", Util.nvl(((Fermentation) beerxml.getRecipe().getFermentation().get(h)).getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(((Fermentation) beerxml.getRecipe().getFermentation().get(h)).getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "NOTES", Util.nvl(((Fermentation) beerxml.getRecipe().getFermentation().get(h)).getNotes()), false);
        writeStartElement(xtw, "FERMENTATION_STEPS", true);
        for (int i = 0; i < ((Fermentation) beerxml.getRecipe().getFermentation().get(h)).getFermentation_Steps().size(); i++) {
            writeStartElement(xtw, "FERMENTATION_STEP", true);
            fermentation_step(xtw, beerxml, i, h);
            writeEndElement(xtw);
        }
        writeEndElement(xtw);
        writeEndElement(xtw);
    }

    private void fermentation_step(XMLStreamWriter xtw, BeerXml beerxml, int h, int CurrentSelectedFermentationIndex) throws XMLStreamException {
        Fermentation m = ((Fermentation) beerxml.getRecipe().getFermentation().get(CurrentSelectedFermentationIndex));
        Fermentation_Step ms = (Fermentation_Step) m.getFermentation_Steps().get(h);
        writeStartElement(xtw, "NAME", Util.nvl(ms.getName()), false);
        writeStartElement(xtw, "VERSION", Util.format0(Util.nvl(ms.getVersion(), Double.valueOf(1))), false);
        writeStartElement(xtw, "STEP_TEMP", Util.nvl(ms.getStep_Temp()), false);
        writeStartElement(xtw, "STEP_TIME", Util.nvl(ms.getStep_Time()), false);
    }

    private static void doRename(String a, String b) {
        File file1 = new File(a);
        System.out.println("File1 = " + file1);
        File file2 = new File(b);
        System.out.println("File2 = " + file2);
        boolean success = file1.renameTo(file2);
        if (success) {
            System.out.println("File was successfully renamed.\n");
        } else {
            System.out.println("File was not successfully renamed.\n");
        }
    }

    public static void copy(String a, String b) throws IOException {
        File inputFile = new File(a);
        File outputFile = new File(b);
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }
}
