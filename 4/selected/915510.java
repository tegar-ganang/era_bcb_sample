package icamodel.framework;

import icamodel.bits.ModelRoot;
import icamodel.utils.Doer;
import icamodel.utils.ModelException;
import icamodel.utils.ModelNode;
import icamodel.utils.ModelParameter;
import icamodel.utils.PaymentPattern;
import icamodel.utils.ResultIndex;
import icamodel.utils.UpdateException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Basic model element. A node in the tree. This just provides the basic
 * mechanism. Concrete subclasses must override the abstract methods.
 * Subclasses must be in package icamodel.bits (else they won't be found when we parse the parameter file).
 * they must also have a  constructor that takes a single string argument (the name).
 *
 * @author Louise
 */
public abstract class ModelBit implements ModelNode {

    /**
     * the logger object
     */
    protected static Logger logger = Logger.getLogger("ICAmodel");

    private static ModelBit theModelRoot;

    private static HashMap<String, ModelBit> allNames = new HashMap<String, ModelBit>();

    private String myName;

    private String myFullName;

    private ModelBit myParent;

    private HashMap<String, ModelParameter> myParams = new HashMap<String, ModelParameter>();

    private HashMap<String, String> myValidParams = new HashMap<String, String>();

    private List<ModelNode> otherChildren = new ArrayList<ModelNode>();

    private List<ModelBit> myChildren = new ArrayList<ModelBit>();

    private List<String> initProblems;

    private int lastUpdated;

    private int lastReset;

    private HashMap<ResultIndex, Double> doubleResults;

    private HashMap<ResultIndex, Integer> intResults;

    private HashMap<ResultIndex, String> stringResults;

    /**
     * Creates a new instance of ModelBit
     * @param name The (unique) name to be assigned to this bit
     */
    public ModelBit(String name) {
        myName = name;
    }

    /**
     * Add a child to this bit of the model
     * @param child the ModelBit to be added as a child
     * @throws icamodel.utils.ModelException if something goes wrong
     */
    public final void addChild(ModelBit child) throws ModelException {
        if (myParent == null && !isRoot()) {
            String msg = "Can't add a child (" + child.getName() + ") to " + this.getName() + "because it isn't in the tree";
            throw new ModelException(msg);
        }
        if (allNames.containsKey(child.getName())) {
            ModelBit other = allNames.get(child.getName());
            String msg = "Can't add " + child.getName() + " to " + this.getName() + " because the name is not unique. " + allNames.get(child.getName()).getFullName() + " already exists.";
            throw new ModelException(msg);
        }
        if (doSpecialAddThings(child)) {
            reallyAddChild(child);
        }
    }

    /**
     * Do special things before actually adding the child. Can be overridden.
     * @param child the ModelBit to be added as a child
     * @throws icamodel.utils.ModelException if something goes wrong
     * @return true if it's OK to go ahead and add the child, false otherwise
     * (though if it's not OK, probably should throw an exception)
     */
    protected boolean doSpecialAddThings(ModelBit child) throws ModelException {
        return true;
    }

    /**
     * Do the actual adding of a child to the parent. Can't be overriddent
     * @param child the ModelBit to be added as a child
     * @throws icamodel.utils.ModelException if something has gone wrong
     */
    private void reallyAddChild(final ModelBit child) throws ModelException {
        allNames.put(child.getName(), child);
        if (myChildren == null) {
            myChildren = new ArrayList<ModelBit>();
        }
        myChildren.add(child);
        child.setParent(this);
    }

    /**
     * Set this bit's parent
     * @param parent The ModelBit that is to be the parent of this one
     * @throws icamodel.utils.ModelException if something goes wrong
     */
    public final void setParent(ModelBit parent) throws ModelException {
        if (isRoot()) {
            throw new ModelException("Can't set parent of the root");
        }
        myParent = parent;
    }

    /**
     * Who is the parent of this bit?
     * @return the ModelBit that is the parent of this one
     */
    public final ModelBit getParent() {
        return myParent;
    }

    /**
     * Is this bit the root of the model?
     * @return true if this is the root, false otherwise
     */
    public final boolean isRoot() {
        return this == theModelRoot;
    }

    /**
     * Make a bit the root of the model
     * @param model the bit that is to be the root
     * @throws icamodel.utils.ModelException if there is already a root
     */
    public static void setRoot(ModelBit model) throws ModelException {
        if (theModelRoot != null) {
            throw new ModelException("Root has already been set; can't overwrite it");
        }
        theModelRoot = model;
        allNames.put(model.getName(), model);
    }

    public static void clearNames() {
        allNames.clear();
    }

    /**
     * Get the name of this bit.
     * @return the short, unique, name of this ModelBit
     */
    public final String getName() {
        return myName;
    }

    /**
     * Get the full name of this bit. The full name includes the names of
     * ancestors.
     * @return the fully qualified name for this model element, formed
     * from the concatenated names of all its ancestors
     */
    public final String getFullName() {
        if (myFullName == null) {
            if (isRoot()) {
                myFullName = myName;
            } else {
                myFullName = getParent().getFullName() + "." + myName;
            }
        }
        return myFullName;
    }

    /**
     * Note that this ModelBit hasn't been properly initialised
     * @param msg A description of the problem
     */
    protected final void addInitProblem(String msg) {
        if (initProblems == null) {
            initProblems = new ArrayList<String>();
        }
        initProblems.add(msg);
    }

    /**
     * Has this ModelBit been initialised properly?
     * @return true if there are no problems
     */
    protected final boolean isInitOK() {
        return initProblems == null;
    }

    /**
     * Get a list of all the initialisation problems that there are
     * with this ModelBit and all its descendants
     * @return the list of problems, each preceded by the name of the ModelBit
     * that it applies to
     */
    public final List<String> getAllInitProblems() {
        List<String> newList = new ArrayList<String>();
        return reallyGetAllInitProblems(newList);
    }

    private List<String> reallyGetAllInitProblems(List<String> sofar) {
        checkForInitProblems();
        if (!isInitOK()) {
            for (Iterator<String> it = initProblems.iterator(); it.hasNext(); ) {
                String msg = getName() + ": " + it.next();
                sofar.add(msg);
            }
        }
        if (myChildren != null) {
            for (Iterator<ModelBit> it = myChildren.iterator(); it.hasNext(); ) {
                ModelBit kid = it.next();
                sofar = kid.reallyGetAllInitProblems(sofar);
            }
        }
        return sofar;
    }

    /**
     * Check this ModelBit for initialisation problems. Any problems should
     * be noted using addInitProblem
     * @see addInitProblem
     */
    protected void checkForInitProblems() {
    }

    /**
     * Get full names of all the descendants of this ModelBit
     * @return A list of the full names of all the descendants, in depth first order,
     * including this ModelBit
     * @throws icamodel.utils.ModelException If there's something wrong with the model
     */
    public List<String> getAllModelBits() throws ModelException {
        return reallyGetAllModelBits(new ArrayList<String>());
    }

    private List<String> reallyGetAllModelBits(List<String> sofar) throws ModelException {
        sofar.add(getFullName());
        if (myChildren != null) {
            for (Iterator<ModelBit> it = myChildren.iterator(); it.hasNext(); ) {
                ModelBit kid = it.next();
                sofar = kid.reallyGetAllModelBits(sofar);
            }
        }
        return sofar;
    }

    /**
     * Get the root of the model
     * @return The bit that is the root of the model
     * @throws icamodel.utils.ModelException if there is no root
     */
    public static ModelBit getRoot() throws ModelException {
        if (theModelRoot == null) {
            throw new ModelException("Root hasn't been set");
        }
        return theModelRoot;
    }

    /**
     * Reset this bit for the start of a realisation.
     * @param rnum the realisation number
     */
    public void clear() {
        this.resetResults(0);
        this.myParams.clear();
        this.otherChildren.clear();
        this.clearChildren();
        if (!this.isRoot()) {
            myChildren.clear();
        }
        this.clearSpecial();
    }

    public abstract void clearSpecial();

    /**
     * clear this bit's children
     **/
    private void clearChildren() {
        if (myChildren != null) {
            Iterator<ModelBit> it = myChildren.iterator();
            while (it.hasNext()) {
                it.next().clear();
            }
        }
    }

    /**
     * Reset this bit for the start of a realisation.
     * @param rnum the realisation number
     */
    public final void reset(int rnum) {
        resetResults(rnum);
        startReset(rnum);
        this.resetChildren(rnum);
        endReset(rnum);
        setLastReset(rnum);
        setLastUpdated(0);
    }

    /**
     * Do whatever has to be done before this bit's children are reset
     * for the start of a realisation
     * @param rnum the realisation number
     */
    protected abstract void startReset(int rnum);

    /**
     * reset this bit's children for the start of a realisation
     * @param rnum the realisation number
     **/
    private void resetChildren(int rnum) {
        if (myChildren != null) {
            Iterator<ModelBit> it = myChildren.iterator();
            while (it.hasNext()) {
                it.next().reset(rnum);
            }
        }
    }

    /**
     * Do whatever has to be done after this bit's children are reset
     * for the start of a realisation
     * @param rnum the realisation number
     */
    protected abstract void endReset(int rnum);

    /**
     * Get the number of the most recent realisation for which this ModelBit has been reset
     * @return the number of the most recent realisation for which this ModelBit has been reset
     */
    protected int getLastReset() {
        return lastReset;
    }

    private void setLastReset(int lastReset) {
        this.lastReset = lastReset;
    }

    /**
     * update this bit for a time period in a realisation
     * @param rnum the realisation number
     * @param tnum the time period number
     */
    public final void update(int rnum, int tnum) {
        startUpdate(rnum, tnum);
        updateChildren(rnum, tnum);
        endUpdate(rnum, tnum);
        setLastUpdated(tnum);
    }

    /**
     * do things that have to be done before the children are updated
     * @param rnum realisation number
     * @param tnum time period number
     */
    protected abstract void startUpdate(int rnum, int tnum);

    /**
     * Describe <code>endUpdate</code> method here.
     *
     * @param rnum realisation number
     * @param tnum time period number
     */
    protected abstract void endUpdate(int rnum, int tnum);

    private void updateChildren(int rnum, int tnum) {
        if (myChildren != null) {
            Iterator<ModelBit> it = myChildren.iterator();
            while (it.hasNext()) {
                it.next().update(rnum, tnum);
            }
        }
    }

    /**
     * get the number of the most recent time period for which this ModelBit has been updated
     * @return the number of the most recent time period for which this ModelBit has been updated
     */
    protected int getLastUpdated() {
        return lastUpdated;
    }

    private void setLastUpdated(int lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * wipe out all recorded results
     */
    private void resetResults(int rnum) {
        intResults = new HashMap<ResultIndex, Integer>();
        doubleResults = new HashMap<ResultIndex, Double>();
        stringResults = new HashMap<ResultIndex, String>();
    }

    /**
     * Get an integer result from this Modelbit for the specifed time period
     * @param rName The name of the result that is required
     * @param tPeriod the time period (in the current realisation) for which the result is required
     * @return the specified result.
     */
    public int getIntegerResult(ResultName rName, int tPeriod) {
        if (tPeriod < lastUpdated) {
            String msg = getFullName() + " hasn't been updated for time period " + tPeriod + " so integer result " + rName + " isn't available";
            throw new UpdateException(msg);
        }
        ResultIndex key = new ResultIndex(rName, tPeriod);
        if (!intResults.containsKey(key)) {
            String msg = getFullName() + " doesn't have an integer result " + rName + " for time period " + tPeriod + ".";
            throw new UpdateException(msg);
        }
        return intResults.get(key);
    }

    /**
     * Get a double result from this Modelbit for the specifed time period
     * @param rName The name of the result that is required
     * @param tPeriod the time period (in the current realisation) for which the result is required
     * @return the specified result.
     */
    public double getDoubleResult(ResultName rName, int tPeriod) {
        if (tPeriod > lastUpdated) {
            String msg = getFullName() + " hasn't been updated for time period " + tPeriod + " so double result " + rName + " isn't available";
            throw new UpdateException(msg);
        }
        ResultIndex key = new ResultIndex(rName, tPeriod);
        if (!doubleResults.containsKey(key)) {
            String msg = getFullName() + " doesn't have a double result " + rName + " for time period " + tPeriod + ".";
            throw new UpdateException(msg);
        }
        return doubleResults.get(key);
    }

    /**
     * Get a string result from this Modelbit for the specifed time period
     * @param rName The name of the result that is required
     * @param tPeriod the time period (in the current realisation) for which the result is required
     * @return the specified result.
     */
    public String getStringResult(ResultName rName, int tPeriod) {
        if (tPeriod < lastUpdated) {
            String msg = getFullName() + " hasn't been updated for time period " + tPeriod + " so string result " + rName + " isn't available";
            throw new UpdateException(msg);
        }
        ResultIndex key = new ResultIndex(rName, tPeriod);
        if (!stringResults.containsKey(key)) {
            String msg = getFullName() + " doesn't have a string result " + rName + " for time period " + tPeriod + ".";
            throw new UpdateException(msg);
        }
        return stringResults.get(key);
    }

    /**
     * Set a result for the specified time period in this realisation
     * @param rName the name of the result
     * @param tPeriod the number of the time period
     * @param result the value of the result
     */
    protected void setResult(ResultName rName, int tPeriod, int result) {
        ResultIndex key = new ResultIndex(rName, tPeriod);
        intResults.put(key, result);
    }

    /**
     * Set a result for the specified time period in this realisation
     * @param rName the name of the result
     * @param tPeriod the number of the time period
     * @param result the value of the result
     */
    protected void setResult(ResultName rName, int tPeriod, double result) {
        ResultIndex key = new ResultIndex(rName, tPeriod);
        doubleResults.put(key, result);
    }

    /**
     * Set a result for the specified time period in this realisation
     * @param rName the name of the result
     * @param tPeriod the number of the time period
     * @param result the value of the result
     */
    protected void setResult(ResultName rName, int tPeriod, String result) {
        ResultIndex key = new ResultIndex(rName, tPeriod);
        stringResults.put(key, result);
    }

    /**
     * Write out all results to a file
     * @param bw the destination
     * @throws java.io.IOException if there's a problem
     */
    public void writeResults(PrintWriter bw) throws IOException {
        String msg;
        if (doubleResults != null) {
            for (Map.Entry<ResultIndex, Double> pair : doubleResults.entrySet()) {
                msg = getFullName() + ", " + pair.getKey().getResultName() + ", " + pair.getKey().getTimePeriod() + ", " + pair.getValue();
                bw.println(msg);
            }
        }
        if (stringResults != null) {
            for (Map.Entry<ResultIndex, String> pair : stringResults.entrySet()) {
                msg = getFullName() + ", " + pair.getKey().getResultName() + ", " + pair.getKey().getTimePeriod() + ", " + pair.getValue();
                bw.println(msg);
            }
        }
        if (intResults != null) {
            for (Map.Entry<ResultIndex, Integer> pair : intResults.entrySet()) {
                msg = getFullName() + ", " + pair.getKey().getResultName() + ", " + pair.getKey().getTimePeriod() + ", " + pair.getValue();
                bw.println(msg);
            }
        }
        if (myChildren != null) {
            Iterator<ModelBit> it = myChildren.iterator();
            while (it.hasNext()) {
                it.next().writeResults(bw);
            }
        }
    }

    /*********************************************************************************
     * Parameter things
     *********************************************************************************/
    public boolean isParameter(String pname) {
        return myParams.containsKey(pname);
    }

    public int countParams() {
        return myParams.size();
    }

    public ModelParameter getParameter(String pname) {
        return myParams.get(pname);
    }

    public Object getParameterValue(String pname) {
        return myParams.get(pname).getValue();
    }

    /**
     * @return descrption of problem, null if OK
     */
    public String setParameter(ModelParameter param) {
        String prob = null;
        if (isValidParameter(param)) {
            prob = checkParameterValue(param);
        } else {
            prob = "Invalid parameter: " + param.getName() + " of type: " + param.getType();
        }
        if (null == prob) {
            myParams.put(param.getName(), param);
            otherChildren.add(param);
        }
        return prob;
    }

    protected abstract String checkParameterValue(ModelParameter param);

    protected void addValidParameter(String pname, String ptype) {
        myValidParams.put(pname, ptype);
    }

    public boolean isValidParameter(String pname) {
        return myValidParams.containsKey(pname);
    }

    public boolean isValidParameter(String pname, String ptype) {
        return myValidParams.containsKey(pname) && ptype.equals(myValidParams.get(pname));
    }

    public boolean isValidParameter(ModelParameter param) {
        return myValidParams.containsKey(param.getName()) && param.getType().equals(myValidParams.get(param.getName())) && param.isValueValidType();
    }

    /*********************************************************************************
     * things to show this in a tree
     *********************************************************************************/
    public int index(ModelNode child) {
        int ans = otherChildren.indexOf(child);
        if (ans == -1) {
            ans = myChildren.indexOf(child);
            if (ans > 0) {
                ans += otherChildren.size();
            }
        }
        return ans;
    }

    public ModelNode child(int searchIndex) {
        ModelNode ans = null;
        if (searchIndex < otherChildren.size()) {
            ans = otherChildren.get(searchIndex);
        } else {
            searchIndex = searchIndex - otherChildren.size();
            ans = myChildren.get(searchIndex);
        }
        return ans;
    }

    public int childCount() {
        return otherChildren.size() + myChildren.size();
    }

    /**
     * Is this time period the first period in a year?
     * @param tnum the time period we are interested in
     * @return true if the period is the first period in a year
     */
    protected static boolean isFirstPeriodInYear(int tnum) {
        int inYear = ModelRoot.getInstance().getTimePeriodsPerYear();
        return tnum % inYear == 1;
    }

    /**
     * Is this time period the last period in a year?
     * @param tnum the time period we are interested in
     * @return true if the period is the last period in a year
     */
    protected static boolean isLastPeriodInYear(int tnum) {
        int inYear = ModelRoot.getInstance().getTimePeriodsPerYear();
        return tnum % inYear == 0;
    }

    /**
     * Get the number of the year that the specified time period is part of
     * @param tnum the time period we are interested in
     * @return the year number
     */
    protected static int getYearFromTimePeriod(int tnum) {
        int inYear = ModelRoot.getInstance().getTimePeriodsPerYear();
        return (tnum / inYear) + 1;
    }

    /**
     * convert a per time period interest rate to an annual rate
     * @param rate the interest rate per time period
     * @return the annual rate
     */
    protected static double toAnnualRate(double rate) {
        int inYear = ModelRoot.getInstance().getTimePeriodsPerYear();
        double arate = Math.pow((1 + rate), inYear) - 1;
        return arate;
    }

    /**
     * convert an annual period interest rate to a rate per time period
     * @param arate the annual interest rate
     * @return the rate per time period
     */
    protected static double fromAnnualRate(double arate) {
        double inYear = (double) ModelRoot.getInstance().getTimePeriodsPerYear();
        double rate = Math.pow((1 + arate), 1 / inYear) - 1;
        return rate;
    }

    /**
     * Do something to all the children of this ModelBit
     * @param thingToDo The Doer that specifies the thing to be done.
     * Its <CODE>dealWithChild</CODE> method will be used on each child of this ModelBit
     * that is accepted by its <CODE>isAccepted</CODE> method.
     * @see Doer
     */
    protected void doToChildren(Doer thingToDo) {
        if (myChildren != null && !myChildren.isEmpty()) {
            for (ModelBit kid : myChildren) {
                if (thingToDo.isAccepted(kid)) {
                    thingToDo.dealWithChild(kid);
                }
            }
        }
    }

    /**
     * Get a total from all the children of this ModelBit
     * @see Doer
     * @param thingToDo The Doer that specifies the thing to be done.
     * Its <CODE>getDoubleFromChild</CODE> method will be used on each child of this ModelBit
     * that is accepted by its <CODE>isAccepted</CODE> method.
     * @return the accumulated total from the children
     */
    protected double totalFromChildren(Doer thingToDo) {
        double total = 0;
        if (myChildren != null && !myChildren.isEmpty()) {
            for (ModelBit kid : myChildren) {
                if (thingToDo.isAccepted(kid)) {
                    total += thingToDo.getDoubleFromChild(kid);
                }
            }
        }
        return total;
    }

    /**
     * Get a total result from all the children of this ModelBit
     * @see Doer
     * @param result The name of the result whose total we want
     * @param tnum the time period for which we want the total
     * @param thingToDo The Doer that specifies the thing to be done.
     * Its <CODE>isAccepted</CODE> method will be used to decide which children to
     * include in the total.
     * @return the calculated total
     */
    protected double totalFromChildren(Doer thingToDo, ResultName result, int tnum) {
        double total = 0;
        if (myChildren != null && !myChildren.isEmpty()) {
            for (ModelBit kid : myChildren) {
                if (thingToDo.isAccepted(kid)) {
                    total += kid.getDoubleResult(result, tnum);
                }
            }
        }
        return total;
    }

    /**
     * count the number of children meeting a condition
     * @see Doer
     * @param thingToDo The Doer that specifies the condition.
     * Its <CODE>isAccepted</CODE> method will be used to decide which children to
     * include in the count.
     * @return the calculated count
     */
    protected int countChildren(Doer thingToDo) {
        int total = 0;
        if (myChildren != null && !myChildren.isEmpty()) {
            for (ModelBit kid : myChildren) {
                if (thingToDo.isAccepted(kid)) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Get the child with the specified name
     */
    public ModelBit getNamedChild(String name) {
        ModelBit ans = null;
        for (ModelBit kid : myChildren) {
            if (kid.getName().equals(name)) {
                ans = kid;
                break;
            }
        }
        return ans;
    }

    public String toString() {
        return "ModelBit: " + getName() + " ( " + this.getClass().getSimpleName() + " ) ";
    }
}
