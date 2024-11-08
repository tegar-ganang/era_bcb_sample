package blue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.apache.commons.lang.text.StrBuilder;
import blue.automation.Automatable;
import blue.automation.AutomatableCollectionListener;
import blue.mixer.Mixer;
import blue.orchestra.Instrument;
import blue.udo.OpcodeList;
import blue.utility.ObjectUtilities;
import blue.utility.TextUtilities;
import electric.xml.Element;
import electric.xml.Elements;
import java.util.regex.Pattern;

/**
 * @author steven
 *
 * Selected instruments from instrument library to use for CSD generation.
 * Instruments are held in a TreeMap.
 */
public class Arrangement implements Cloneable, Serializable, TableModel {

    private static final Pattern NEW_LINES = Pattern.compile("\\n");

    private ArrayList<InstrumentAssignment> arrangement;

    private transient Vector<TableModelListener> listeners = null;

    private transient Vector<ArrangementListener> arrangementListeners = null;

    private transient Vector<AutomatableCollectionListener> automatableCollectionListeners = null;

    private transient HashMap compilationVariables;

    private transient StrBuilder preGenerationCache = null;

    private transient ArrayList<InstrumentAssignment> preGenList = new ArrayList<InstrumentAssignment>();

    public Arrangement() {
        arrangement = new ArrayList<InstrumentAssignment>();
    }

    public int addInstrument(Instrument instrument) {
        return this.addInstrument("0", instrument);
    }

    /**
     * Should be only called to add Mixer instrument
     *
     * @param instrument
     */
    public int addInstrumentAtEnd(Instrument instrument) {
        int max = 0;
        for (int i = 0; i < arrangement.size(); i++) {
            InstrumentAssignment ia = arrangement.get(i);
            try {
                int instrNum = Integer.parseInt(ia.arrangementId);
                if (instrNum > max) {
                    max = instrNum;
                }
            } catch (Exception e) {
                continue;
            }
        }
        String newIndex = Integer.toString(max + 1);
        InstrumentAssignment ia = new InstrumentAssignment();
        ia.arrangementId = newIndex;
        ia.instr = instrument;
        arrangement.add(ia);
        if (instrument instanceof Automatable) {
            fireAutomatableAdded((Automatable) ia.instr);
        }
        return max + 1;
    }

    public int addInstrument(String instrumentId, Instrument instrument) {
        int retVal = -1;
        try {
            int currentInstrumentNum = (instrumentId == null) ? 0 : Integer.parseInt(instrumentId);
            int counter = currentInstrumentNum + 1;
            for (int i = 0; i < arrangement.size(); i++) {
                InstrumentAssignment ia = arrangement.get(i);
                try {
                    if (counter == Integer.parseInt(ia.arrangementId)) {
                        counter++;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            String newIndex = Integer.toString(counter);
            InstrumentAssignment ia = new InstrumentAssignment();
            ia.arrangementId = newIndex;
            ia.instr = instrument;
            arrangement.add(ia);
            retVal = counter;
        } catch (NumberFormatException nfe) {
            String instrID = instrumentId;
            if (containsInstrumentId(instrID)) {
                int counter = 0;
                while (containsInstrumentId(instrumentId + counter)) {
                    counter++;
                }
                instrID = instrumentId + counter;
            }
            InstrumentAssignment ia = new InstrumentAssignment();
            ia.arrangementId = instrID;
            ia.instr = instrument;
            arrangement.add(ia);
        }
        Collections.<InstrumentAssignment>sort(arrangement);
        fireTableDataChanged();
        if (instrument instanceof Automatable) {
            fireAutomatableAdded((Automatable) instrument);
        }
        return retVal;
    }

    public void addInstrumentWithId(Instrument instr, String instrId) {
        InstrumentAssignment ia = new InstrumentAssignment();
        ia.arrangementId = instrId;
        ia.instr = instr;
        arrangement.add(ia);
        Collections.<InstrumentAssignment>sort(arrangement);
        if (instr instanceof Automatable) {
            fireAutomatableAdded((Automatable) instr);
        }
        fireTableDataChanged();
    }

    public boolean containsInstrumentId(String instrId) {
        for (int i = 0; i < arrangement.size(); i++) {
            InstrumentAssignment ia = arrangement.get(i);
            if (ia.arrangementId.equals(instrId)) {
                return true;
            }
        }
        return false;
    }

    public void insertInstrument(String arrangementId, Instrument instrument) {
        InstrumentAssignment ia = new InstrumentAssignment();
        ia.arrangementId = arrangementId;
        ia.instr = instrument;
        arrangement.add(ia);
        Collections.<InstrumentAssignment>sort(arrangement);
    }

    public void replaceInstrument(String instrumentId, Instrument instr) {
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (ia.arrangementId.equals(instrumentId)) {
                ia.instr = instr;
            }
        }
        fireTableDataChanged();
        if (instr instanceof Automatable) {
            fireAutomatableAdded((Automatable) instr);
        }
    }

    public Instrument removeInstrument(String instrumentId) {
        for (int i = 0; i < arrangement.size(); i++) {
            InstrumentAssignment ia = arrangement.get(i);
            if (ia.arrangementId.equals(instrumentId)) {
                return removeInstrument(i);
            }
        }
        return null;
    }

    public int size() {
        return arrangement.size();
    }

    public String getInstrumentId(int index) {
        InstrumentAssignment ia = arrangement.get(index);
        return ia.arrangementId;
    }

    public String getInstrumentId(Instrument instr) {
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (ia.instr == instr) {
                return ia.arrangementId;
            }
        }
        return null;
    }

    public String[] getInstrumentIds() {
        String[] ids = new String[arrangement.size()];
        for (int i = 0; i < arrangement.size(); i++) {
            ids[i] = getInstrumentId(i);
        }
        return ids;
    }

    public Instrument getInstrument(int index) {
        InstrumentAssignment ia = arrangement.get(index);
        return ia.instr;
    }

    public Instrument getInstrument(String arrangementId) {
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (ia.arrangementId.equals(arrangementId)) {
                return ia.instr;
            }
        }
        return null;
    }

    public void changeInstrumentId(Instrument instr, String newId) {
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (ia.instr == instr) {
                String oldId = ia.arrangementId;
                ia.arrangementId = newId;
                Collections.<InstrumentAssignment>sort(arrangement);
                if (arrangementListeners != null) {
                    fireArrangementChanged(new ArrangementEvent(ArrangementEvent.INSTRUMENT_ID_CHANGED, oldId, newId));
                }
                return;
            }
        }
    }

    public Object clone() {
        return ObjectUtilities.clone(this);
    }

    public ArrayList<InstrumentAssignment> getArrangement() {
        return arrangement;
    }

    public void setArrangement(ArrayList arrangement) {
        this.arrangement = arrangement;
    }

    public String generateGlobalOrc() {
        StrBuilder retVal = new StrBuilder();
        ArrayList<Instrument> instruments = new ArrayList<Instrument>();
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (!ia.enabled) {
                continue;
            }
            Instrument instr = ia.instr;
            if (!instruments.contains(instr)) {
                String globalOrc = instr.generateGlobalOrc();
                if (globalOrc != null) {
                    String transformed = replaceInstrumentId(ia, globalOrc);
                    retVal.append(transformed);
                    retVal.append("\n");
                }
                instruments.add(instr);
            }
        }
        return retVal.toString();
    }

    public String generateGlobalSco() {
        StrBuilder retVal = new StrBuilder();
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (!ia.enabled) {
                continue;
            }
            Instrument instr = ia.instr;
            String globalSco = instr.generateGlobalSco();
            if (globalSco != null) {
                String transformed = replaceInstrumentId(ia, globalSco);
                retVal.append(transformed);
                retVal.append("\n");
            }
        }
        return retVal.toString();
    }

    /**
     * Called before generating Mixer instrument in CSD Render. This needs to be
     * called earlier as it will go and find out what subchannels have
     * non-channel dependencies which result from SoundObject's that contain
     * blueMixerOut code that uses subchannels.
     *
     * @param mixer
     * @param nchnls
     */
    public void preGenerateOrchestra(Mixer mixer, int nchnls) {
        if (preGenerationCache == null) {
            preGenerationCache = new StrBuilder();
            preGenList = new ArrayList<InstrumentAssignment>();
        }
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            preGenList.add(ia);
            if (!ia.enabled) {
                continue;
            }
            appendInstrumentText(preGenerationCache, ia, mixer, nchnls);
        }
    }

    public String generateOrchestra(Mixer mixer, int nchnls) {
        StrBuilder buffer;
        if (preGenerationCache == null) {
            buffer = new StrBuilder();
        } else {
            buffer = preGenerationCache;
        }
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (!preGenList.contains(ia)) {
                if (!ia.enabled) {
                    continue;
                }
                appendInstrumentText(buffer, ia, mixer, nchnls);
            }
        }
        String retVal = buffer.toString();
        preGenerationCache = null;
        preGenList = null;
        return retVal;
    }

    private void appendInstrumentText(StrBuilder buffer, InstrumentAssignment ia, Mixer mixer, int nchnls) {
        Instrument instr = ia.instr;
        buffer.append("\tinstr ").append(ia.arrangementId);
        buffer.append("\t;").append(instr.getName()).append("\n");
        String instrumentText = instr.generateInstrument();
        String transformed = replaceInstrumentId(ia, instrumentText);
        transformed = convertBlueMixerOut(mixer, ia.arrangementId, transformed, nchnls);
        buffer.append(transformed).append("\n");
        buffer.append("\tendin\n\n");
    }

    public void clearUnusedInstrAssignments() {
        Iterator<InstrumentAssignment> iter = arrangement.iterator();
        while (iter.hasNext()) {
            InstrumentAssignment ia = iter.next();
            if (!ia.enabled) {
                iter.remove();
            }
        }
    }

    public void generateUserDefinedOpcodes(OpcodeList udos) {
        Iterator<InstrumentAssignment> iter = arrangement.iterator();
        while (iter.hasNext()) {
            InstrumentAssignment ia = iter.next();
            if (!ia.enabled) {
                continue;
            }
            ia.instr.generateUserDefinedOpcodes(udos);
        }
    }

    public void generateFTables(Tables tables) {
        Iterator<InstrumentAssignment> iter = arrangement.iterator();
        while (iter.hasNext()) {
            InstrumentAssignment ia = iter.next();
            if (!ia.enabled) {
                continue;
            }
            ia.instr.generateFTables(tables);
        }
    }

    private String replaceInstrumentId(InstrumentAssignment ia, String input) {
        String replacementId = "";
        try {
            replacementId += Integer.parseInt(ia.arrangementId);
        } catch (NumberFormatException nfe) {
            replacementId = "\"" + ia.arrangementId + "\"";
        }
        String transformed = TextUtilities.replaceAll(input, "<INSTR_ID>", replacementId);
        transformed = TextUtilities.replaceAll(transformed, "<INSTR_NAME>", ia.arrangementId);
        return transformed;
    }

    private String convertBlueMixerOut(Mixer mixer, String arrangementId, String input, int nchnls) {
        if (input.indexOf("blueMixerOut") < 0) {
            return input;
        }
        StrBuilder buffer = new StrBuilder();
        String[] lines = NEW_LINES.split(input);
        for (String line : lines) {
            if (line.trim().startsWith("blueMixerOut")) {
                String argText = line.trim().substring(12);
                String[] args = argText.split(",");
                if (args[0].trim().matches("\".*\"")) {
                    if (mixer == null || !mixer.isEnabled()) {
                        buffer.append("outc ");
                        for (int i = 1; i < args.length; i++) {
                            if (i > 1) {
                                buffer.append(",");
                            }
                            buffer.append(args[i]);
                        }
                        buffer.append("\n");
                    } else {
                        String subChannelName = args[0].trim();
                        subChannelName = subChannelName.substring(1, subChannelName.length() - 1);
                        mixer.addSubChannelDependency(subChannelName);
                        for (int i = 1; i < nchnls + 1 && i < args.length; i++) {
                            String arg = args[i];
                            String var = Mixer.getSubChannelVar(subChannelName, i - 1);
                            buffer.append(var).append(" = ");
                            buffer.append(var).append(" + ").append(arg).append("\n");
                        }
                    }
                } else {
                    if (mixer == null || !mixer.isEnabled()) {
                        buffer.append(line.replaceAll("blueMixerOut", "outc"));
                        buffer.append("\n");
                    } else {
                        for (int i = 0; i < nchnls && i < args.length; i++) {
                            String arg = args[i];
                            String var = Mixer.getChannelVar(arrangementId, i);
                            buffer.append(var).append(" = ");
                            buffer.append(var).append(" + ").append(arg).append("\n");
                        }
                    }
                }
            } else {
                buffer.append(line).append("\n");
            }
        }
        return buffer.toString();
    }

    /**
     * Gets compilation variable; should only be called by plugins when
     * compiling a CSD is happening. Plugins can check variables that are set,
     * useful for caching ID's, instruments, etc.
     */
    public Object getCompilationVariable(Object key) {
        if (compilationVariables == null) {
            compilationVariables = new HashMap();
            return null;
        }
        return compilationVariables.get(key);
    }

    /**
     * Sets compilation variable; should only be called by plugins when
     * compiling a CSD is happening. Plugins can set variables, useful for
     * caching ID's, instruments, etc.
     */
    public void setCompilationVariable(Object key, Object value) {
        if (compilationVariables == null) {
            compilationVariables = new HashMap();
        }
        compilationVariables.put(key, value);
    }

    public static Arrangement loadFromXML(Element data) throws Exception {
        Arrangement arr = new Arrangement();
        Elements items = data.getElements("instrumentAssignment");
        while (items.hasMoreElements()) {
            Element elem = items.next();
            InstrumentAssignment ia = InstrumentAssignment.loadFromXML(elem);
            arr.getArrangement().add(ia);
        }
        return arr;
    }

    public Element saveAsXML() {
        Element retVal = new Element("arrangement");
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            retVal.addElement(ia.saveAsXML());
        }
        return retVal;
    }

    /**
     * Used by old pre 0.95.0 code before instrument libraries removed from
     * project and user instrument library was implemented.
     *
     * This code is here to maintain compatibility with projects that still
     * contain InstrumentLibraries and is used when migrating to new format.
     *
     * @deprecated
     */
    @Deprecated
    public static Arrangement loadFromXML(Element data, InstrumentLibrary iLibrary) {
        Arrangement arr = new Arrangement();
        Elements items = data.getElements("instrumentAssignment");
        while (items.hasMoreElements()) {
            Element elem = items.next();
            InstrumentAssignment ia = InstrumentAssignment.loadFromXML(elem, iLibrary);
            arr.getArrangement().add(ia);
        }
        return arr;
    }

    /**
     * Used by old pre 0.95.0 code before instrument libraries removed from
     * project and user instrument library was implemented.
     *
     * This code is here to maintain compatibility with projects that still
     * contain InstrumentLibraries and is used when migrating to new format.
     *
     * @deprecated
     */
    @Deprecated
    public Element saveAsXML(InstrumentLibrary iLibrary) {
        Element retVal = new Element("arrangement");
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            retVal.addElement(ia.saveAsXML(iLibrary));
        }
        return retVal;
    }

    /**
     * @param instr
     * @return
     */
    public boolean containsInstrument(Instrument instr) {
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            if (ia.instr == instr) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param index
     */
    public Instrument removeInstrument(int index) {
        InstrumentAssignment ia = arrangement.remove(index);
        fireTableDataChanged();
        if (ia.instr instanceof Automatable) {
            fireAutomatableRemoved((Automatable) ia.instr);
        }
        return ia.instr;
    }

    /**
     * @param rowIndex
     * @return
     */
    public InstrumentAssignment getInstrumentAssignment(int rowIndex) {
        return arrangement.get(rowIndex);
    }

    /**
     *
     */
    public void normalize() {
        for (Iterator<InstrumentAssignment> iter = arrangement.iterator(); iter.hasNext(); ) {
            InstrumentAssignment ia = iter.next();
            ia.normalize();
        }
    }

    public int getRowCount() {
        return this.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public String getColumnName(int columnIndex) {
        switch(columnIndex) {
            case 0:
                return "[X]";
            case 1:
                return BlueSystem.getString("arrangement.instrID");
            case 2:
                return BlueSystem.getString("arrangement.instrName");
            default:
                return "";
        }
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        }
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        InstrumentAssignment ia = this.getInstrumentAssignment(rowIndex);
        switch(columnIndex) {
            case 0:
                return Boolean.valueOf(ia.enabled);
            case 1:
                return ia.arrangementId;
            case 2:
                return ia.instr.getName();
            default:
                return null;
        }
    }

    public void setValueAt(Object value, int row, int col) {
        if (col == 0) {
            InstrumentAssignment ia = this.getInstrumentAssignment(row);
            ia.enabled = ((Boolean) value).booleanValue();
        } else if (col == 1) {
            try {
                String newId = ((String) value);
                Instrument instr = this.getInstrument(row);
                this.changeInstrumentId(instr, newId);
            } catch (Exception e) {
                System.out.println("error in OrchestraTableModel: setValueAt");
                e.printStackTrace();
            }
        } else if (col == 2) {
            InstrumentAssignment ia = this.getInstrumentAssignment(row);
            ia.instr.setName((String) value);
        }
        fireTableDataChanged();
    }

    public void addTableModelListener(TableModelListener l) {
        if (listeners == null) {
            listeners = new Vector<TableModelListener>();
        }
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        if (listeners == null) {
            return;
        }
        listeners.remove(l);
    }

    private void fireTableDataChanged() {
        if (listeners == null) {
            return;
        }
        TableModelEvent tme = new TableModelEvent(this);
        for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
            TableModelListener listener = (TableModelListener) iter.next();
            listener.tableChanged(tme);
        }
        if (arrangementListeners != null) {
            fireArrangementChanged(new ArrangementEvent(ArrangementEvent.UPDATE, null, null));
        }
    }

    public void addArrangementListener(ArrangementListener l) {
        if (arrangementListeners == null) {
            arrangementListeners = new Vector<ArrangementListener>();
        }
        arrangementListeners.add(l);
    }

    public void removeArrangementListener(ArrangementListener l) {
        if (arrangementListeners == null) {
            return;
        }
        arrangementListeners.remove(l);
    }

    private void fireArrangementChanged(ArrangementEvent arrEvt) {
        if (arrangementListeners == null) {
            return;
        }
        for (Iterator iter = arrangementListeners.iterator(); iter.hasNext(); ) {
            ArrangementListener listener = (ArrangementListener) iter.next();
            listener.arrangementChanged(arrEvt);
        }
    }

    public void addAutomatableCollectionListener(AutomatableCollectionListener listener) {
        if (automatableCollectionListeners == null) {
            automatableCollectionListeners = new Vector<AutomatableCollectionListener>();
        }
        automatableCollectionListeners.add(listener);
    }

    public void removeAutomatableCollectionListener(AutomatableCollectionListener listener) {
        if (automatableCollectionListeners != null) {
            automatableCollectionListeners.remove(listener);
        }
    }

    private void fireAutomatableAdded(Automatable automatable) {
        if (automatableCollectionListeners != null) {
            Iterator iter = new Vector(automatableCollectionListeners).iterator();
            while (iter.hasNext()) {
                AutomatableCollectionListener listener = (AutomatableCollectionListener) iter.next();
                listener.automatableAdded(automatable);
            }
        }
    }

    private void fireAutomatableRemoved(Automatable automatable) {
        if (automatableCollectionListeners != null) {
            Iterator iter = new Vector(automatableCollectionListeners).iterator();
            while (iter.hasNext()) {
                AutomatableCollectionListener listener = (AutomatableCollectionListener) iter.next();
                listener.automatableRemoved(automatable);
            }
        }
    }
}
