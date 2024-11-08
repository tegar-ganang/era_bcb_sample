package org.fudaa.dodico.h2d.reflux;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import java.util.*;
import org.nfunk.jep.Variable;
import org.fudaa.ctulu.CtuluAnalyze;
import org.fudaa.ctulu.CtuluCommand;
import org.fudaa.ctulu.CtuluLib;
import org.fudaa.ctulu.CtuluLibArray;
import org.fudaa.ctulu.CtuluLibMessage;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.ctulu.CtuluParser;
import org.fudaa.ctulu.CtuluPermanentList;
import org.fudaa.ctulu.CtuluPermanentTreeMap;
import org.fudaa.dodico.ef.EfFrontierInterface;
import org.fudaa.dodico.ef.EfGridInterface;
import org.fudaa.dodico.ef.EfLib;
import org.fudaa.dodico.h2d.H2dBcFrontierInterface;
import org.fudaa.dodico.h2d.H2dBcFrontierMiddleInterface;
import org.fudaa.dodico.h2d.H2dBcManagerAbstract;
import org.fudaa.dodico.h2d.H2dBcManagerMiddleInterface;
import org.fudaa.dodico.h2d.H2dBoundary;
import org.fudaa.dodico.h2d.H2dBoundaryCondition;
import org.fudaa.dodico.h2d.H2dEvolutionUseCounter;
import org.fudaa.dodico.h2d.resource.H2dResource;
import org.fudaa.dodico.h2d.type.H2dBcType;
import org.fudaa.dodico.h2d.type.H2dBoundaryType;
import org.fudaa.dodico.h2d.type.H2dRefluxBoundaryType;
import org.fudaa.dodico.h2d.type.H2dVariableType;

/**
 * @author deniger
 * @version $Id: H2dRefluxBcManager.java,v 1.42 2007-06-29 15:10:29 deniger Exp $
 */
public class H2dRefluxBcManager extends H2dBcManagerAbstract implements H2dBcManagerMiddleInterface {

    /**
   * Classe decrivant pour un type de bord, le comportement des variable: autorisee, type...
   * 
   * @author Fred Deniger
   * @version $Id: H2dRefluxBcManager.java,v 1.42 2007-06-29 15:10:29 deniger Exp $
   */
    public abstract static class BoundaryBehavior {

        /**
     * @param _t la variable a tester
     * @return la valeur fixee que doit respecte la variable. Si rien d'impose, renvoie null
     */
        public H2dRefluxValue getImposedComportement(final H2dVariableType _t) {
            return null;
        }

        /**
     * @return les variables pouvant etre modifiee
     */
        public abstract H2dVariableType[] getVariableAvailable();

        public H2dVariableType[] getVariableAvailableForSegment() {
            return getVariableAvailable();
        }

        /**
     * @param _t la variable a tester
     * @return true si la variable _t peut-etre affecte au point du bord
     */
        public abstract boolean isVariableAvailable(H2dVariableType _t);

        /**
     * @param _t la variable a tester
     * @return true si la variable _t peut-etre affecte bord
     */
        public boolean isVariableAvailableForSegment(final H2dVariableType _t) {
            return isVariableAvailable(_t);
        }

        /**
     * A utiliser pour valider les points extremes du bord. Les point extremes peuvent appartenir a deux types de bord
     * differents. Donc, il faut valider le necessaires. Si un point extreme appartient a 2 bords de meme type, il vaut
     * mieux utiliser validateStrict
     * 
     * @param _l la condition a verifier
     * @return true si valide.
     */
        public abstract boolean validateLess(H2dRefluxBoundaryCondition _l);

        /**
     * Si la condition n'est pas valide, elle est modifiee. A utiliser pour les points milieux.
     * 
     * @param _l la condition a verifier
     * @return true si valide (pas de changement). false si changement effectue.
     */
        public abstract boolean validateStrict(H2dRefluxBoundaryCondition _l);
    }

    /**
   * Implantation pour reflux.
   * 
   * @author Fred Deniger
   * @version $Id: H2dRefluxBcManager.java,v 1.42 2007-06-29 15:10:29 deniger Exp $
   */
    public class RefluxMiddleFrontier implements H2dBcFrontierMiddleInterface {

        H2dRefluxBoundaryCondition[] bcArray_;

        int gridIdx_;

        protected RefluxMiddleFrontier(final int _maillageindex, final H2dRefluxBoundaryCondition[] _cls) {
            gridIdx_ = _maillageindex;
            bcArray_ = _cls;
        }

        protected void replaceEvol(final Map _evolEquivEvol) {
            for (int i = bcArray_.length - 1; i >= 0; i--) {
                bcArray_[i].replaceEvol(_evolEquivEvol);
            }
        }

        protected void restoreFromSave(final TIntObjectHashMap _sauv, final boolean _structureChanged) {
            final TIntObjectIterator it = _sauv.iterator();
            int idx;
            for (int i = _sauv.size(); i-- > 0; ) {
                it.advance();
                idx = it.key();
                bcArray_[idx].restoreEvolUsed((H2dRefluxBoundaryCondition) it.value());
                bcArray_[idx] = (H2dRefluxBoundaryCondition) it.value();
            }
            if (_structureChanged) {
                fireBcFrontierStructureChanged(this);
            } else {
                fireParametersForBoundaryPtsChange();
            }
        }

        protected TIntObjectHashMap setValuesForPointsIntern(final int[] _idx, final Map _typeValue, final H2dRefluxValue _normal, final boolean _save) {
            if ((_idx == null) || (_idx.length == 0)) {
                return null;
            }
            final int n = _idx.length;
            int temp, idxEnCours;
            TIntObjectHashMap sauv = null;
            if (_save) {
                sauv = new TIntObjectHashMap(n);
            }
            boolean hasToBeSaved;
            boolean modified = false;
            H2dRefluxBoundaryCondition oldBc, bc;
            for (int i = n - 1; i >= 0; i--) {
                hasToBeSaved = false;
                idxEnCours = _idx[i];
                bc = bcArray_[idxEnCours];
                oldBc = bc.createCopy();
                if (_normal != null) {
                    double val = _normal.getValue();
                    if (_normal.getOldExpr() != null) {
                        final Variable oldVar = _normal.getOldExpr().getVar(CtuluParser.getOldVariable());
                        if (oldVar != null) {
                            oldVar.setValue(CtuluLib.getDouble(bc.getNormale()));
                        }
                        val = _normal.getOldExpr().getValue();
                    }
                    hasToBeSaved |= bc.setNormale(val);
                }
                if (_typeValue != null) {
                    hasToBeSaved |= bc.setValues(_typeValue);
                }
                if (bc.isMiddle()) {
                    hasToBeSaved |= !getComportFor(bc.getMiddle().getBoundaryType()).validateStrict(bc);
                } else {
                    temp = idxEnCours + 1;
                    final H2dBoundaryType next = bcArray_[temp].getMiddle().getBoundaryType();
                    temp -= 2;
                    final H2dBoundaryType prec = bcArray_[temp < 0 ? getNbPt() - 1 : temp].getMiddle().getBoundaryType();
                    if (next == prec) {
                        hasToBeSaved |= !getComportFor(next).validateStrict(bc);
                    } else {
                        hasToBeSaved |= (!getComportFor(next).validateLess(bc)) | (!getComportFor(prec).validateLess(bc));
                    }
                }
                if (hasToBeSaved && _save && sauv != null) {
                    sauv.put(idxEnCours, oldBc);
                }
                modified |= hasToBeSaved;
            }
            if (modified) {
                fireParametersForBoundaryPtsChange();
            }
            return sauv;
        }

        protected TIntObjectHashMap setValuesIntern(final int[] _ptMidIdxOnFrontier, final H2dBoundaryType _new, final Map _typeValue, final H2dRefluxValue _normal, final boolean _save) {
            if ((_ptMidIdxOnFrontier == null) || (_ptMidIdxOnFrontier.length == 0)) {
                return null;
            }
            final int n = _ptMidIdxOnFrontier.length;
            TIntObjectHashMap sauv = null;
            if (_save) {
                sauv = new TIntObjectHashMap(n * 2);
            }
            boolean hasToBeSaved;
            H2dRefluxBoundaryCondition oldBc, bc;
            int idxMid, temp;
            final int nbPt = getNbPt();
            boolean majBdType = false;
            boolean modified = false;
            if ((_new != null) && (_new != H2dRefluxBoundaryType.MIXTE)) {
                majBdType = true;
            }
            final Map typeValueExt = getTypeValue(_typeValue);
            H2dBoundaryType currentType;
            BoundaryBehavior currentComport;
            for (int i = n - 1; i >= 0; i--) {
                hasToBeSaved = false;
                idxMid = EfLib.getIdxFromIdxMid(_ptMidIdxOnFrontier[i]);
                bc = bcArray_[idxMid];
                oldBc = bc.createCopy();
                if (_normal != null) {
                    hasToBeSaved |= bc.setNormale(_normal);
                }
                if (majBdType) {
                    final H2dRefluxBoundaryConditionMiddle bcNew = bc.getMiddle().setBoundaryType(_new);
                    if (bcNew != null) {
                        hasToBeSaved = true;
                        if (bcNew != bc) {
                            bc = bcNew;
                            bcArray_[idxMid] = bc;
                        }
                    }
                }
                if (_typeValue != null) {
                    hasToBeSaved |= bc.setValues(_typeValue);
                }
                currentType = bc.getMiddle().getBoundaryType();
                currentComport = getComportFor(currentType);
                hasToBeSaved |= !currentComport.validateStrict(bc);
                if (hasToBeSaved && _save && sauv != null) {
                    sauv.put(idxMid, oldBc);
                }
                modified |= hasToBeSaved;
                hasToBeSaved = false;
                idxMid--;
                bc = bcArray_[idxMid];
                oldBc = bc.createCopy();
                if (_normal != null) {
                    hasToBeSaved |= bc.setNormale(_normal);
                }
                if (_typeValue != null) {
                    hasToBeSaved |= bc.setValues(typeValueExt);
                }
                if (idxMid > 0) {
                    temp = idxMid - 1;
                } else {
                    temp = nbPt - 1;
                }
                H2dBoundaryType precType = bcArray_[temp].getMiddle().getBoundaryType();
                if (precType == currentType) {
                    hasToBeSaved |= !currentComport.validateStrict(bc);
                } else {
                    hasToBeSaved |= (!currentComport.validateLess(bc)) | (!getComportFor(precType).validateLess(bc));
                }
                if (hasToBeSaved && _save && sauv != null) {
                    sauv.put(idxMid, oldBc);
                }
                modified |= hasToBeSaved;
                hasToBeSaved = false;
                idxMid += 2;
                if (idxMid == nbPt) {
                    idxMid = 0;
                }
                bc = bcArray_[idxMid];
                oldBc = bc.createCopy();
                if (_normal != null) {
                    hasToBeSaved |= bc.setNormale(_normal);
                }
                if (_typeValue != null) {
                    hasToBeSaved |= bc.setValues(typeValueExt);
                }
                temp = idxMid + 1;
                precType = bcArray_[temp].getMiddle().getBoundaryType();
                if (precType == currentType) {
                    hasToBeSaved |= !currentComport.validateStrict(bc);
                } else {
                    hasToBeSaved |= (!currentComport.validateLess(bc)) | (!getComportFor(precType).validateLess(bc));
                }
                if (hasToBeSaved && _save && sauv != null) {
                    sauv.put(idxMid, oldBc);
                }
                modified |= hasToBeSaved;
            }
            if (modified) {
                fireBcFrontierStructureChanged(this);
            }
            return sauv;
        }

        private Map getTypeValue(final Map _typeValue) {
            Map typeValueExt = _typeValue;
            if ((_typeValue != null) && (_typeValue.containsKey(H2dVariableType.RUGOSITE))) {
                typeValueExt = new HashMap(_typeValue);
                typeValueExt.remove(H2dVariableType.RUGOSITE);
            }
            return typeValueExt;
        }

        /**
     * @return true si contient des cl avec des variables transitoires
     */
        public boolean containsClTransient() {
            for (int i = bcArray_.length - 1; i >= 0; i--) {
                if (bcArray_[i].containsClTransient()) {
                    return true;
                }
            }
            return false;
        }

        /**
     * @return true si contient des propritete nodales avec des courbes transitoires
     */
        public boolean containsPnTransient() {
            for (int i = bcArray_.length - 1; i >= 0; i--) {
                if (bcArray_[i].containsPnTransient()) {
                    return true;
                }
            }
            return false;
        }

        /**
     * @param _s la collection a remplir avec les type de frontiere utilises.
     */
        public void fillListWithUsedBoundary(final Set _s) {
            for (int i = bcArray_.length - 1; i >= 0; i--) {
                if (bcArray_[i].isMiddle()) {
                    _s.add(bcArray_[i].getMiddle().getBoundaryType());
                }
            }
        }

        /**
     * Remplir la collection avec les variables autorise pour le point d'indice demande.
     * 
     * @param _idxOnFrontier l'indice du point sur la frontiere
     * @param _l la collection a remplir
     */
        public void fillWithAvailableVariablesFor(final int _idxOnFrontier, final Set _l) {
            final H2dRefluxBoundaryCondition bc = bcArray_[_idxOnFrontier];
            if (bc.isMiddle()) {
                _l.addAll(Arrays.asList(getComportFor(bc.getMiddle().getBoundaryType()).getVariableAvailable()));
            } else {
                int temp = _idxOnFrontier + 1;
                _l.addAll(Arrays.asList(getComportFor(bcArray_[temp].getMiddle().getBoundaryType()).getVariableAvailable()));
                temp = _idxOnFrontier - 1;
                if (temp < 0) {
                    temp = getNbPt() - 1;
                }
                _l.addAll(Arrays.asList(getComportFor(bcArray_[temp].getMiddle().getBoundaryType()).getVariableAvailable()));
            }
        }

        /**
     * @param _m le tableau a remplir avec les evolution->Variable
     */
        public void fillWithEvolVar(final H2dEvolutionUseCounter _m) {
            for (int i = bcArray_.length - 1; i >= 0; i--) {
                bcArray_[i].fillWithEvolVar(_m);
            }
        }

        /**
     * @param _r collection a remplir avec les evolutions utilisees.
     */
        public void fillWithUsedEvolution(final Set _r) {
            for (int i = bcArray_.length - 1; i >= 0; i--) {
                bcArray_[i].fillWithUsedEvolution(_r);
            }
        }

        /**
     * @param _idxOnFrontier l'indice du point sur la frontiere
     * @return les variables autorisees (H2DVariableType)
     */
        public Set getAvailablesVariables(final int _idxOnFrontier) {
            final Set r = new HashSet();
            fillWithAvailableVariablesFor(_idxOnFrontier, r);
            return r;
        }

        /**
     * @param _selectedPt les indices des points sur la frontieres
     * @return collection contenant les variables autorisee pour tous les pts selectionnes (l'intersection)
     */
        public Set getAvailablesVariables(final int[] _selectedPt) {
            if ((_selectedPt == null) || (_selectedPt.length == 0)) {
                return Collections.EMPTY_SET;
            }
            final Set init = new HashSet(8);
            final Set temp = new HashSet(8);
            final int n = _selectedPt.length;
            fillWithAvailableVariablesFor(_selectedPt[n - 1], init);
            for (int i = n - 2; i >= 0; i--) {
                temp.clear();
                fillWithAvailableVariablesFor(_selectedPt[i], temp);
                init.retainAll(temp);
                if (init.size() == 0) {
                    return init;
                }
            }
            return init;
        }

        /**
     * Renvoie nill si ce n'est pas un point milieu.
     */
        public H2dBoundaryType getBcType(final int _i) {
            if (_i % 2 == 1) {
                return bcArray_[_i].getMiddle().getBoundaryType();
            }
            return null;
        }

        /**
     * Si _ptIdx, correspond a un point milieu, le type du point milieu est renvoye. Sinon, les points milieux
     * environnants sont testes. S'ils ont le meme type, ce type est renvoye. Sinon mixte est renvoye.
     * 
     * @param _ptIdx l'indice sur la frontiere
     * @return le type du segment correspondant.
     */
        public H2dBoundaryType getSurroundingBoundaryType(final int _ptIdx) {
            final H2dRefluxBoundaryCondition bc = bcArray_[_ptIdx];
            if (bc.isMiddle()) {
                return bc.getMiddle().getBoundaryType();
            }
            final H2dBoundaryType next = bcArray_[_ptIdx + 1].getMiddle().getBoundaryType();
            if (next == bcArray_[_ptIdx == 0 ? getNbPt() - 1 : _ptIdx - 1].getMiddle().getBoundaryType()) {
                return next;
            }
            return H2dRefluxBoundaryType.MIXTE;
        }

        public H2dBoundaryCondition getCl(final int _idxOnThisFrontier) {
            return bcArray_[_idxOnThisFrontier];
        }

        /**
     * @param _selectedPt les indices des points selectionnees.
     * @return le type commun ou MIXTE sur plusieurs type
     */
        public H2dBoundaryType getCommonBoundaryType(final int[] _selectedPt) {
            if ((_selectedPt == null) || (_selectedPt.length == 0)) {
                return null;
            }
            final int n = _selectedPt.length;
            final H2dBoundaryType initType = getSurroundingBoundaryType(_selectedPt[n - 1]);
            H2dBoundaryType test;
            if (initType == H2dRefluxBoundaryType.MIXTE) {
                return initType;
            }
            for (int i = n - 2; i >= 0; i--) {
                test = getSurroundingBoundaryType(_selectedPt[i]);
                if (test != initType) {
                    return H2dRefluxBoundaryType.MIXTE;
                }
            }
            return initType;
        }

        /**
     * Retrouve la valeur commune pour la friction. Attention: les indices sont supposes etre des indices de point
     * milieu et aucun test n'est effectue.
     * 
     * @param _idx des indices de points milieux
     * @return la valeur commune. jamais null.
     */
        public H2dRefluxValue getCommonFrictionValueFromMiddleIdx(final int[] _idx, final H2dRefluxValue _v) {
            if (CtuluLibArray.isEmpty(_idx)) {
                return null;
            }
            int midIdx;
            H2dRefluxValue r = _v;
            final H2dRefluxValue temp = new H2dRefluxValue();
            for (int i = _idx.length - 1; i >= 0; i--) {
                midIdx = EfLib.getIdxFromIdxMid(_idx[i]);
                r = computeFriction(midIdx, r, temp);
            }
            return r == null ? new H2dRefluxValue() : r;
        }

        private H2dRefluxValue computeFriction(final int _midIdx, final H2dRefluxValue _result, final H2dRefluxValue _temp) {
            H2dRefluxValue res = _result;
            if (bcArray_[_midIdx].isMiddleWithFriction()) {
                if (res == null) {
                    res = new H2dRefluxValue();
                    bcArray_[_midIdx].getMiddleFriction().fillWithValueFriction(res);
                } else {
                    bcArray_[_midIdx].getMiddleFriction().fillWithValueFriction(_temp);
                    if (!res.equalsValue(_temp)) {
                        if (res.getType() == _temp.getType()) {
                            if ((_temp.getType() == H2dBcType.PERMANENT) && (res.isDoubleValueConstant()) && (res.getValue() != _temp.getValue())) {
                                res.setDoubleValueConstant(false);
                            } else if ((_temp.getType() == H2dBcType.TRANSITOIRE) && (res.isEvolutionFixed()) && (res.getEvolution() != _temp.getEvolution())) {
                                res.setEvolution(null);
                            }
                        } else {
                            res.setType(H2dBcType.MIXTE);
                        }
                    }
                }
            }
            return res;
        }

        /**
     * @param _idx les indices a tester
     * @return la valeur commune pour la normale. null si pas de valeur commune
     */
        public Double getCommonNormal(final int[] _idx) {
            if ((_idx == null) || (_idx.length == 0)) {
                return null;
            }
            final int n = _idx.length;
            final double temp = bcArray_[_idx[n - 1]].getNormale();
            for (int i = n - 2; i >= 0; i--) {
                if (bcArray_[_idx[i]].getNormale() != temp) {
                    return null;
                }
            }
            return CtuluLib.getDouble(temp);
        }

        /**
     * Pour optimisation, certaines classes utilise un numerotation ne prenant en compte que les points milieux.Soit
     * 0->1 1->3 2->5 ...
     * 
     * @param _idxMiddle les indices des points milieux. Les indices normaux sont recuperes grace a EFLib
     * @return la valeur commune pour la normale. null si pas de valeur commune
     * @see EfLib#getIdxFromIdxMid(int)
     */
        public Double getCommonNormalFromMiddle(final int[] _idxMiddle) {
            if ((_idxMiddle == null) || (_idxMiddle.length == 0)) {
                return null;
            }
            final int nbPt = getNbPt();
            int idx;
            double temp = 0d;
            for (int i = _idxMiddle.length - 1; i >= 0; i--) {
                idx = EfLib.getIdxFromIdxMid(_idxMiddle[i]);
                temp = bcArray_[idx].getNormale();
                if (bcArray_[idx - 1].getNormale() != temp) {
                    return null;
                }
                idx++;
                if (idx == nbPt) {
                    idx = 0;
                }
                if (bcArray_[idx].getNormale() != temp) {
                    return null;
                }
            }
            return CtuluLib.getDouble(temp);
        }

        /**
     * @param _idx les points a tester
     * @param _t la variable concernee
     * @return les valeurs communes. Jamais null.
     */
        public H2dRefluxValue getCommonValue(final int[] _idx, final H2dVariableType _t) {
            if ((_idx == null) || (_idx.length == 0)) {
                return null;
            }
            final H2dRefluxValue r = new H2dRefluxValue();
            final H2dRefluxValue temp = new H2dRefluxValue();
            bcArray_[_idx[_idx.length - 1]].fillWithValue(_t, r);
            for (int i = _idx.length - 2; i >= 0; i--) {
                bcArray_[_idx[i]].fillWithValue(_t, temp);
                if (H2dRefluxValue.compareTwoValueAndIsDiff(r, temp)) {
                    if (r.getType() == H2dBcType.MIXTE) {
                        return r;
                    }
                }
            }
            return r;
        }

        /**
     * Pour optimisation, certaines classes utilise un numerotation ne prenant en compte que les points milieux.Soit
     * 0->1 1->3 2->5 ...Les tests sont effectues sur tous les points du L3 correspondant.
     * 
     * @param _middleIdx les points a tester
     * @param _t la variable concernee
     * @return les valeurs communes. Jamais null.
     */
        public H2dRefluxValue getCommonValueFromMiddle(final int[] _middleIdx, final H2dVariableType _t) {
            return getCommonValueFromMiddle(_middleIdx, _t, null);
        }

        public H2dRefluxValue getCommonValueFromMiddle(final int[] _middleIdx, final H2dVariableType _t, final H2dRefluxValue _v) {
            if ((_middleIdx == null) || (_middleIdx.length == 0)) {
                return null;
            }
            if (_t == H2dVariableType.RUGOSITE) {
                return getCommonFrictionValueFromMiddleIdx(_middleIdx, _v);
            }
            H2dRefluxValue r = _v;
            final H2dRefluxValue temp = new H2dRefluxValue();
            final int nbPt = getNbPt();
            int idx;
            for (int i = _middleIdx.length - 1; i >= 0; i--) {
                idx = EfLib.getIdxFromIdxMid(_middleIdx[i]);
                if (r == null) {
                    r = new H2dRefluxValue();
                    bcArray_[idx].fillWithValue(_t, r);
                } else {
                    bcArray_[idx].fillWithValue(_t, temp);
                    H2dRefluxValue.computeCommonValue(r, temp);
                    if (r.getType() == H2dBcType.MIXTE) {
                        return r;
                    }
                }
                if (r.getType() == H2dBcType.MIXTE) {
                    return r;
                }
                bcArray_[idx - 1].fillWithValue(_t, temp);
                H2dRefluxValue.computeCommonValue(r, temp);
                if (r.getType() == H2dBcType.MIXTE) {
                    return r;
                }
                idx++;
                if (idx == nbPt) {
                    idx = 0;
                }
                bcArray_[idx].fillWithValue(_t, temp);
                H2dRefluxValue.computeCommonValue(r, temp);
                if (r.getType() == H2dBcType.MIXTE) {
                    return r;
                }
            }
            return r;
        }

        public int getNbPt() {
            return bcArray_.length;
        }

        /**
     * @param _i l'indice du point sur cette frontiere
     * @return a condition limite correspondante
     */
        public H2dRefluxBoundaryCondition getRefluxBc(final int _i) {
            return bcArray_[_i];
        }

        /**
     * @return Liste a utiliser pour reecrire les fichier inp.
     */
        public List getRefluxIndexGeneralBords() {
            final List l = new ArrayList();
            final EfGridInterface m = getGrid();
            final EfFrontierInterface frontieres = m.getFrontiers();
            final int n = bcArray_.length;
            final int max = n - 1;
            for (int i = 1; i < n; i += 2) {
                final H2dRefluxBoundaryConditionMiddle cl = bcArray_[i].getMiddle();
                if (isAddedToBord(cl)) {
                    final H2dBoundaryType bType = cl.getBoundaryType();
                    final H2dRefluxBordIndexGeneral bidxGene = new H2dRefluxBordIndexGeneral(bType);
                    final int[] idx = new int[3];
                    idx[0] = frontieres.getIdxGlobal(this.gridIdx_, i - 1);
                    idx[1] = frontieres.getIdxGlobal(this.gridIdx_, i);
                    idx[2] = frontieres.getIdxGlobal(this.gridIdx_, i == max ? 0 : i + 1);
                    bidxGene.setIndex(idx);
                    l.add(bidxGene);
                    if (bType == H2dRefluxBoundaryType.SOLIDE_FROTTEMENT) {
                        final H2dRefluxBoundaryConditionMiddleFriction clFr = cl.getMiddleFriction();
                        if (clFr != null) {
                            bidxGene.setRugositeType(clFr.getFrictionType());
                            bidxGene.setRugosite(clFr.getFriction());
                            bidxGene.setRugositeTransitoireCourbe(clFr.getFrictionEvolution());
                        }
                    }
                }
            }
            return l;
        }

        protected boolean isAddedToBord(final H2dRefluxBoundaryConditionMiddle _cl) {
            final H2dBoundaryType bType = _cl.getBoundaryType();
            if (bType == H2dRefluxBoundaryType.SOLIDE) {
                return false;
            }
            if (bType == H2dRefluxBoundaryType.LIQUIDE && !_cl.containsClTransient() && _cl.getVType() == H2dBcType.LIBRE && _cl.getUType() == H2dBcType.LIBRE) {
                return false;
            }
            return true;
        }

        /**
     * Rempli la valeur si l'index correspond bien a un point milieu avec friction.
     * 
     * @param _idxPtOnThisFrontier
     * @param _v la valeur a modifier
     * @return true si l'index correspond bien a un point milieu avec friction
     */
        public boolean getRugositeValeurs(final int _idxPtOnThisFrontier, final H2dRefluxValue _v) {
            final H2dRefluxBoundaryCondition c = bcArray_[_idxPtOnThisFrontier];
            if (c.isMiddleWithFriction()) {
                c.getMiddleFriction().fillWithValueFriction(_v);
                return true;
            }
            return false;
        }

        /**
     * Pour optimisation, certaines classes utilise un numerotation ne prenant en compte que les points milieux.Soit
     * 0->1 1->3 2->5.
     * 
     * @param _idxMiddleSelected les indices milieux.
     * @return collection contenant les type de bord selectionnees
     */
        public Set getSelectedBoundaryType(final int[] _idxMiddleSelected) {
            if ((_idxMiddleSelected == null) || (_idxMiddleSelected.length == 0)) {
                return null;
            }
            final Set r = new HashSet(H2dRefluxBcManager.getBoundaryTypeComportMap().size());
            addSelectedBoundaryType(_idxMiddleSelected, r);
            return r;
        }

        public void addSelectedBoundaryType(final int[] _idxMiddleSelected, final Set _target) {
            if ((_idxMiddleSelected == null) || (_idxMiddleSelected.length == 0)) {
                return;
            }
            int middleIdx;
            for (int i = _idxMiddleSelected.length - 1; i >= 0; i--) {
                middleIdx = EfLib.getIdxFromIdxMid(_idxMiddleSelected[i]);
                if (middleIdx < bcArray_.length) {
                    _target.add(bcArray_[middleIdx].getMiddle().getBoundaryType());
                }
            }
        }

        /**
     * @param _ptIdx les indices des points a modifier
     * @param _d la nouvelle valeur de la normale
     * @return une comande si modification
     */
        public CtuluCommand setNormalForPoints(final int[] _ptIdx, final double _d) {
            if ((_ptIdx == null) || (_ptIdx.length == 0)) {
                return null;
            }
            final int n = _ptIdx.length;
            final TIntDoubleHashMap save = new TIntDoubleHashMap(n);
            H2dRefluxBoundaryCondition l;
            double old;
            for (int i = n - 1; i >= 0; i--) {
                l = bcArray_[_ptIdx[i]];
                old = l.getNormale();
                if (l.setNormale(_d)) {
                    save.put(_ptIdx[i], old);
                }
            }
            if (save.size() > 0) {
                firePtsNormaleChange();
                return new CtuluCommand() {

                    public void redo() {
                        H2dRefluxBoundaryCondition lredo;
                        for (int i = _ptIdx.length - 1; i >= 0; i--) {
                            lredo = bcArray_[_ptIdx[i]];
                            lredo.setNormale(_d);
                        }
                        firePtsNormaleChange();
                    }

                    public void undo() {
                        final TIntDoubleIterator it = save.iterator();
                        for (int i = save.size(); i-- > 0; ) {
                            bcArray_[it.key()].setNormale(it.value());
                        }
                        firePtsNormaleChange();
                    }
                };
            }
            return null;
        }

        /**
     * @param _ptMidIdxOnFrontier les indices milieux
     * @param _new le nouveau type pour ces points
     * @param _typeValue variable->RefluxValue
     * @param _normal la nouvelle normale (null si pas de modif a faire)
     * @return la commande. null si aucune modification
     */
        public CtuluCommand setValues(final int[] _ptMidIdxOnFrontier, final H2dBoundaryType _new, final Map _typeValue, final H2dRefluxValue _normal) {
            final TIntObjectHashMap sauv = setValuesIntern(_ptMidIdxOnFrontier, _new, _typeValue, _normal, true);
            if ((sauv == null) || (sauv.size() == 0)) {
                return null;
            }
            fireBcFrontierStructureChanged(this);
            return new CtuluCommand() {

                public void redo() {
                    setValuesIntern(_ptMidIdxOnFrontier, _new, _typeValue, _normal, false);
                }

                public void undo() {
                    restoreFromSave(sauv, true);
                }
            };
        }

        /**
     * @param _idx les indices des points a modifier
     * @param _typeValue variable->RefluxValue
     * @param _normal la nouvelle normale (null si pas de modif a faire)
     * @return la commande ou null si pas de modif
     */
        public CtuluCommand setValuesForPoints(final int[] _idx, final Map _typeValue, final H2dRefluxValue _normal) {
            final TIntObjectHashMap sauv = setValuesForPointsIntern(_idx, _typeValue, _normal, true);
            if ((sauv == null) || (sauv.size() == 0)) {
                return null;
            }
            fireBcFrontierStructureChanged(this);
            return new CtuluCommand() {

                public void redo() {
                    setValuesForPointsIntern(_idx, _typeValue, _normal, false);
                }

                public void undo() {
                    restoreFromSave(sauv, false);
                }
            };
        }
    }

    private static CtuluPermanentTreeMap BORDLIST;

    /**
   * Valeur libre.
   */
    public static final H2dRefluxValue FREE_VALUE = new H2dRefluxValue(H2dBcType.LIBRE, 0, null);

    private static H2dRefluxBoundaryCondition createSolidCL(final int _i) {
        final H2dRefluxBoundaryCondition r = new H2dRefluxBoundaryCondition();
        r.setIndexPt(_i);
        r.setUTypePermanentAndNull();
        return r;
    }

    private static H2dRefluxBoundaryConditionMiddle createSolidMiddleCL(final int _i) {
        final H2dRefluxBoundaryConditionMiddle r = new H2dRefluxBoundaryConditionMiddle(H2dRefluxBoundaryType.SOLIDE);
        r.setIndexPt(_i);
        r.setUTypePermanentAndNull();
        return r;
    }

    private static boolean isOuvert(final H2dRefluxBoundaryCondition[] _cl) {
        return _cl[1].isOpenInUVH();
    }

    /**
   * @return la valeur par defaut pour de la friction (fixe a 0)
   */
    public static final H2dRefluxValue createDefaultFrictionParam() {
        return new H2dRefluxValue(H2dBcType.PERMANENT, 0, null);
    }

    /**
   * @return map avec H2dRefluxBoundaryType->BordComportment
   */
    public static final CtuluPermanentTreeMap getBoundaryTypeComportMap() {
        if (BORDLIST == null) {
            final Map m = new HashMap(7);
            m.put(H2dRefluxBoundaryType.LIQUIDE, createBoundaryBehavior());
            m.put(H2dRefluxBoundaryType.SOLIDE, createSolidBoundaryBehavior());
            m.put(H2dRefluxBoundaryType.LIQUIDE_DEBIT_IMPOSE, createDebitBoundaryBehavior());
            m.put(H2dRefluxBoundaryType.SOLIDE_FROTTEMENT, createFrottBoundaryBehavior());
            BORDLIST = new CtuluPermanentTreeMap(m);
        }
        return BORDLIST;
    }

    private static BoundaryBehavior createFrottBoundaryBehavior() {
        return new BoundaryBehavior() {

            public H2dRefluxValue getImposedComportement(final H2dVariableType _t) {
                if (_t == H2dVariableType.VITESSE_NORMALE) {
                    return new H2dRefluxValue(H2dBcType.PERMANENT, 0, null);
                } else if (_t == H2dVariableType.RUGOSITE) {
                    return null;
                } else {
                    return FREE_VALUE;
                }
            }

            public H2dVariableType[] getVariableAvailable() {
                return new H2dVariableType[] { H2dVariableType.RUGOSITE, H2dVariableType.VITESSE_TANGENTIELLE };
            }

            public H2dVariableType[] getVariableAvailableForSegment() {
                return new H2dVariableType[] { H2dVariableType.RUGOSITE };
            }

            public boolean isVariableAvailable(final H2dVariableType _t) {
                return (_t == H2dVariableType.RUGOSITE) || (_t == H2dVariableType.VITESSE_TANGENTIELLE);
            }

            public boolean isVariableAvailableForSegment(final H2dVariableType _t) {
                return _t == H2dVariableType.RUGOSITE;
            }

            public final boolean validateLess(final H2dRefluxBoundaryCondition _cl) {
                boolean r = true;
                if ((_cl.getUType() != H2dBcType.PERMANENT) || (_cl.getU() != 0)) {
                    r = false;
                    _cl.setUTypePermanentAndNull();
                }
                return r;
            }

            public boolean validateStrict(final H2dRefluxBoundaryCondition _bc) {
                boolean r = validateLess(_bc);
                if (_bc.getVType() == H2dBcType.PERMANENT) {
                    if (_bc.getV() != 0) {
                        r = false;
                        _bc.setVTypePermanentAndNull();
                    }
                } else if (_bc.getVType() != H2dBcType.LIBRE) {
                    r = false;
                    _bc.setVTypeFree();
                }
                if (_bc.getHType() != H2dBcType.LIBRE) {
                    r = false;
                    _bc.setHTypeFree();
                }
                if (_bc.getQType() != H2dBcType.LIBRE) {
                    r = false;
                    _bc.setQTypeFree();
                }
                if (!r) {
                    CtuluLibMessage.debug("bordcomport controle change");
                }
                return r;
            }
        };
    }

    private static BoundaryBehavior createDebitBoundaryBehavior() {
        return new BoundaryBehavior() {

            public H2dRefluxValue getImposedComportement(final H2dVariableType _t) {
                if (_t != H2dVariableType.DEBIT) {
                    return FREE_VALUE;
                }
                return null;
            }

            public H2dVariableType[] getVariableAvailable() {
                return new H2dVariableType[] { H2dVariableType.DEBIT, H2dVariableType.VITESSE_NORMALE, H2dVariableType.VITESSE_TANGENTIELLE };
            }

            public boolean isVariableAvailable(final H2dVariableType _t) {
                return _t == H2dVariableType.DEBIT || _t == H2dVariableType.VITESSE_TANGENTIELLE || _t == H2dVariableType.VITESSE_NORMALE;
            }

            public final boolean validateLess(final H2dRefluxBoundaryCondition _cl) {
                boolean r = true;
                if (_cl.getQType() == H2dBcType.LIBRE) {
                    r = false;
                    _cl.setQTypePermanentAndNull();
                }
                if (_cl.getHType() != H2dBcType.LIBRE) {
                    r = false;
                    _cl.setHTypeFree();
                }
                if ((_cl.isMiddleWithFriction()) && (_cl.getMiddleFriction().getFrictionType() != H2dBcType.LIBRE)) {
                    new Throwable("prob with a flowrate boundary").printStackTrace();
                    _cl.getMiddleFriction().setFrictionFree();
                    r = false;
                }
                return r;
            }

            public boolean validateStrict(final H2dRefluxBoundaryCondition _cl) {
                boolean r = validateLess(_cl);
                if (!r) {
                    CtuluLibMessage.debug("bordcomport controle change");
                }
                return r;
            }
        };
    }

    private static BoundaryBehavior createSolidBoundaryBehavior() {
        return new BoundaryBehavior() {

            public H2dRefluxValue getImposedComportement(final H2dVariableType _t) {
                if (_t == H2dVariableType.VITESSE_NORMALE) {
                    return new H2dRefluxValue(H2dBcType.PERMANENT, 0, null);
                } else if (_t != H2dVariableType.VITESSE_TANGENTIELLE) {
                    return FREE_VALUE;
                }
                return null;
            }

            public H2dVariableType[] getVariableAvailable() {
                return new H2dVariableType[] { H2dVariableType.VITESSE_TANGENTIELLE, H2dVariableType.VITESSE_NORMALE };
            }

            public H2dVariableType[] getVariableAvailableForSegment() {
                return new H2dVariableType[0];
            }

            public boolean isVariableAvailable(final H2dVariableType _t) {
                return _t == H2dVariableType.VITESSE_TANGENTIELLE;
            }

            public boolean isVariableAvailableForSegment(final H2dVariableType _t) {
                return false;
            }

            public final boolean validateLess(final H2dRefluxBoundaryCondition _cl) {
                boolean r = true;
                if ((_cl.getUType() != H2dBcType.LIBRE && _cl.getUType() != H2dBcType.PERMANENT) || (_cl.getUType() == H2dBcType.PERMANENT && _cl.getU() != 0)) {
                    _cl.setUTypePermanentAndNull();
                    r = false;
                }
                if (_cl.isMiddleWithFriction()) {
                    new Throwable("prob with a solid boundary").printStackTrace();
                    _cl.getMiddleFriction().setFrictionFree();
                    r = false;
                }
                return r;
            }

            public boolean validateStrict(final H2dRefluxBoundaryCondition _cl) {
                boolean r = validateLess(_cl);
                if ((_cl.getUType() != H2dBcType.PERMANENT) || (_cl.getUType() == H2dBcType.PERMANENT && _cl.getU() != 0)) {
                    _cl.setUTypePermanentAndNull();
                    r = false;
                }
                if (_cl.getVType() == H2dBcType.PERMANENT) {
                    if (_cl.getV() != 0) {
                        r = false;
                        _cl.setVTypePermanentAndNull();
                    }
                } else if (_cl.getVType() != H2dBcType.LIBRE) {
                    r = false;
                    _cl.setVTypeFree();
                }
                if (_cl.getHType() != H2dBcType.LIBRE) {
                    r = false;
                    _cl.setHTypeFree();
                }
                if (_cl.getQType() != H2dBcType.LIBRE) {
                    r = false;
                    _cl.setQTypeFree();
                }
                if (!r) {
                    CtuluLibMessage.debug("bordcomport controle change");
                }
                return r;
            }
        };
    }

    private static BoundaryBehavior createBoundaryBehavior() {
        return new BoundaryBehavior() {

            public H2dRefluxValue getImposedComportement(final H2dVariableType _t) {
                if ((_t == H2dVariableType.DEBIT) || (_t == H2dVariableType.RUGOSITE)) {
                    return FREE_VALUE;
                }
                return null;
            }

            public H2dVariableType[] getVariableAvailable() {
                return new H2dVariableType[] { H2dVariableType.COTE_EAU, H2dVariableType.VITESSE_NORMALE, H2dVariableType.VITESSE_TANGENTIELLE };
            }

            public boolean isVariableAvailable(final H2dVariableType _t) {
                return (_t != H2dVariableType.DEBIT) && (_t != H2dVariableType.RUGOSITE);
            }

            public final boolean validateLess(final H2dRefluxBoundaryCondition _cl) {
                if (_cl.isMiddleWithFriction()) {
                    new Throwable("prob with liquid boundary").printStackTrace();
                    return false;
                }
                if (_cl.getQType() != H2dBcType.LIBRE && _cl.getHType() != H2dBcType.LIBRE) {
                    _cl.setQTypeFree();
                    return false;
                }
                return true;
            }

            public final boolean validateStrict(final H2dRefluxBoundaryCondition _cl) {
                boolean r = validateLess(_cl);
                if (_cl.getQType() != H2dBcType.LIBRE) {
                    _cl.setQTypeFree();
                    r = false;
                }
                return r;
            }
        };
    }

    /**
   * @return tableau des variables autorisees pour les conditions limites et pour les L3
   */
    public static H2dVariableType[] getClBordVariables() {
        return new H2dVariableType[] { H2dVariableType.RUGOSITE, H2dVariableType.VITESSE_NORMALE, H2dVariableType.VITESSE_TANGENTIELLE, H2dVariableType.COTE_EAU, H2dVariableType.DEBIT };
    }

    /**
   * @return tableau des variables autorisees pour les conditions limites.
   */
    public static H2dVariableType[] getClNodaleVariables() {
        return new H2dVariableType[] { H2dVariableType.VITESSE_NORMALE, H2dVariableType.VITESSE_TANGENTIELLE, H2dVariableType.COTE_EAU, H2dVariableType.DEBIT };
    }

    /**
   * @param _boundaryType les types a tester
   * @return toutes les variables communes pouvant etre modifiees pour ces types de bords (intersection)
   */
    public static Set getCommonVariableAvailable(final H2dBoundaryType[] _boundaryType) {
        if (_boundaryType.length == 0) {
            return Collections.EMPTY_SET;
        }
        final Set r = new HashSet();
        r.addAll(Arrays.asList(getComportFor(_boundaryType[_boundaryType.length - 1]).getVariableAvailableForSegment()));
        final Set temp = new HashSet();
        for (int i = _boundaryType.length - 2; i >= 0; i--) {
            temp.clear();
            temp.addAll(Arrays.asList(getComportFor(_boundaryType[i]).getVariableAvailable()));
            r.retainAll(temp);
        }
        return r;
    }

    /**
   * @param _t le type demande
   * @return le comportement des variables
   */
    public static BoundaryBehavior getComportFor(final H2dBoundaryType _t) {
        if (BORDLIST == null) {
            getBoundaryTypeComportMap();
        }
        return (BoundaryBehavior) BORDLIST.get(_t);
    }

    /**
   * @param _m le maillage support
   * @return le gestionnaires des bords.
   */
    public static H2dRefluxBcManager init(final EfGridInterface _m) {
        final EfFrontierInterface frontieres = _m.getFrontiers();
        if (frontieres == null) {
            return null;
        }
        final int n = frontieres.getNbFrontier();
        final H2dRefluxBcManager.RefluxMiddleFrontier[] bordByFrontier = new RefluxMiddleFrontier[n];
        int ptIdxGlobal;
        final H2dRefluxBcManager r = new H2dRefluxBcManager(_m);
        for (int i = 0; i < n; i++) {
            final int nbPt = frontieres.getNbPt(i);
            final H2dRefluxBoundaryCondition[] cls = new H2dRefluxBoundaryCondition[nbPt];
            for (int j = 0; j < nbPt; j += 2) {
                ptIdxGlobal = frontieres.getIdxGlobal(i, j);
                cls[j] = createSolidCL(ptIdxGlobal);
            }
            for (int j = 1; j < nbPt; j += 2) {
                ptIdxGlobal = frontieres.getIdxGlobal(i, j);
                cls[j] = createSolidMiddleCL(ptIdxGlobal);
            }
            bordByFrontier[i] = r.createRefluxMiddleFrontier(i, cls);
            H2dRefluxBoundaryCondition.initializeNormales(_m, cls, i);
        }
        r.bcFrontier_ = bordByFrontier;
        return r;
    }

    /**
   * @param _s la source contenant la definition des conditions limites
   * @param _analyze receveur de message
   * @return le manager de bords correctement initialise.
   */
    public static H2dRefluxBcManager init(final H2dRefluxSourceInterface _s, final CtuluAnalyze _analyze) {
        final EfFrontierInterface frontieres = _s.getGrid().getFrontiers();
        int ptIdxGlobal;
        final H2dRefluxBcManager r = new H2dRefluxBcManager(_s.getGrid());
        final int n = frontieres.getNbFrontier();
        final RefluxMiddleFrontier[] bordByFrontier = new RefluxMiddleFrontier[n];
        final H2dRefluxBordIndexGeneral[] bordSpecifies = _s.getBords();
        final H2dRefluxBoundaryCondition[] cl3Temp = new H2dRefluxBoundaryCondition[3];
        for (int i = 0; i < n; i++) {
            final int nbPt = frontieres.getNbPt(i);
            final H2dRefluxBoundaryCondition[] cls = new H2dRefluxBoundaryCondition[nbPt];
            for (int j = 1; j < nbPt; j += 2) {
                ptIdxGlobal = frontieres.getIdxGlobal(i, j - 1);
                H2dRefluxBoundaryCondition cl = _s.getConditionLimite(ptIdxGlobal);
                if (cl == null) {
                    if (_analyze != null) {
                        _analyze.addError(H2dResource.getS("Pas de cl pour le point de bord {0}", CtuluLibString.getString(ptIdxGlobal)));
                    }
                    final H2dRefluxBoundaryCondition clN = new H2dRefluxBoundaryCondition();
                    clN.setIndexPt(ptIdxGlobal);
                    cls[j - 1] = clN;
                } else {
                    cl.initUsedEvol();
                    cls[j - 1] = cl;
                }
                cl3Temp[0] = cls[j - 1];
                if (j < (nbPt - 1)) {
                    ptIdxGlobal = frontieres.getIdxGlobal(i, j + 1);
                    cl = _s.getConditionLimite(ptIdxGlobal);
                    if (cl == null) {
                        if (_analyze != null) {
                            _analyze.addError(H2dResource.getS("Pas de cl pour le point de bord {0}", CtuluLibString.getString(ptIdxGlobal)));
                        }
                        final H2dRefluxBoundaryConditionMutable clN = new H2dRefluxBoundaryConditionMutable();
                        clN.setIndexPt(ptIdxGlobal);
                        cls[j + 1] = clN;
                    } else {
                        cl.initUsedEvol();
                        cls[j + 1] = cl;
                    }
                    cl3Temp[2] = cls[j + 1];
                }
                ptIdxGlobal = frontieres.getIdxGlobal(i, j);
                final H2dRefluxBordIndexGeneral bord = bordSpecifies == null ? null : H2dRefluxBordIndexGeneral.findBordWithIndex(ptIdxGlobal, bordSpecifies);
                H2dBoundaryType bordType = H2dRefluxBoundaryType.SOLIDE;
                cl = _s.getConditionLimite(ptIdxGlobal);
                if (cl == null) {
                    if (_analyze != null) {
                        _analyze.addFatalError(H2dResource.getS("Pas de cl pour le point de bord {0}", CtuluLibString.getString(ptIdxGlobal)));
                    }
                    return null;
                }
                if (bord == null) {
                    cl3Temp[1] = cl;
                    if (isOuvert(cl3Temp)) {
                        bordType = H2dRefluxBoundaryType.LIQUIDE;
                    }
                } else {
                    bordType = bord.getBordType();
                }
                if (bord != null && bordType == H2dRefluxBoundaryType.SOLIDE_FROTTEMENT) {
                    cl = new H2dRefluxBoundaryConditionMiddleFriction(cl);
                    cl.setValue(H2dVariableType.RUGOSITE, bord.getRugositeType(), bord.getRugosite(), bord.getRugositeTransitoireCourbe());
                } else {
                    cl = new H2dRefluxBoundaryConditionMiddle(cl, bordType);
                    if (bordType == H2dRefluxBoundaryType.LIQUIDE && cl.getHType() == H2dBcType.LIBRE && cl3Temp[0].getHType() != H2dBcType.LIBRE && cl3Temp[2].getHType() != H2dBcType.LIBRE) {
                        if (cl3Temp[0].getHType() == H2dBcType.PERMANENT && cl3Temp[2].getHType() == H2dBcType.PERMANENT) {
                            cl.setHTypePermanent((cl3Temp[0].getH() + cl3Temp[2].getH()) / 2);
                        } else {
                            if (cl3Temp[0].getHType() == H2dBcType.PERMANENT) {
                                cl.setH(cl3Temp[0].getH());
                            } else {
                                cl.setHTransitoire(cl3Temp[0].getHTransitoireCourbe());
                            }
                        }
                    }
                }
                cl.initUsedEvol();
                cl.setIndexPt(ptIdxGlobal);
                cls[j] = cl;
            }
            bordByFrontier[i] = r.createRefluxMiddleFrontier(i, cls);
        }
        r.bcFrontier_ = bordByFrontier;
        return r;
    }

    private H2dBcFrontierMiddleInterface[] bcFrontier_;

    /**
   *
   */
    protected H2dRefluxBcManager(final EfGridInterface _m) {
        super(_m);
    }

    private RefluxMiddleFrontier createRefluxMiddleFrontier(final int _maillageindex, final H2dRefluxBoundaryCondition[] _cls) {
        return new RefluxMiddleFrontier(_maillageindex, _cls);
    }

    protected void fireParametersForBoundaryPtsChange() {
        super.fireParametersForBoundaryPtsChange();
    }

    protected void firePtsNormaleChange() {
        for (final Iterator it = listeners_.iterator(); it.hasNext(); ) {
            ((H2dRefluxBcListener) it.next()).bcPointsNormalChanged();
        }
    }

    protected void replaceEvol(final Map _evolEquivEvol) {
        for (int i = bcFrontier_.length - 1; i >= 0; i--) {
            ((RefluxMiddleFrontier) bcFrontier_[i]).replaceEvol(_evolEquivEvol);
        }
    }

    /**
   * @param _l un nouveau listener a ajouter
   */
    public void addClListener(final H2dRefluxBcListener _l) {
        if (listeners_ == null) {
            listeners_ = new ArrayList();
        }
        if (_l == null) {
            new Throwable().printStackTrace();
        } else {
            listeners_.add(_l);
        }
    }

    /**
   * @return true si contient des donnees transitoires sur les cl
   */
    public boolean containsClTransient() {
        for (int i = bcFrontier_.length - 1; i >= 0; i--) {
            if (((RefluxMiddleFrontier) bcFrontier_[i]).containsClTransient()) {
                return true;
            }
        }
        return false;
    }

    /**
   * @return true si contient des donnees transitoires sur les prop nodales.
   */
    public boolean containsPnTransient() {
        for (int i = bcFrontier_.length - 1; i >= 0; i--) {
            if (((RefluxMiddleFrontier) bcFrontier_[i]).containsPnTransient()) {
                return true;
            }
        }
        return false;
    }

    /**
   * Parcourt toutes les frontieres pour remplir la table.
   * 
   * @param _m la table a remplir
   */
    public void fillWithEvolVar(final H2dEvolutionUseCounter _m) {
        for (int i = bcFrontier_.length - 1; i >= 0; i--) {
            ((RefluxMiddleFrontier) bcFrontier_[i]).fillWithEvolVar(_m);
        }
    }

    /**
   * Parcourt toutes les frontieres pour remplir la table.
   * 
   * @param _s la table a remplir
   */
    public void fillWithUsedEvolution(final Set _s) {
        for (int i = bcFrontier_.length - 1; i >= 0; i--) {
            ((RefluxMiddleFrontier) bcFrontier_[i]).fillWithUsedEvolution(_s);
        }
    }

    /**
   * @param _s la collection a remplir avec les variables pouvant avoir un comportement transitoire.
   */
    public void fillWithTransientVar(final Collection _s) {
        _s.add(H2dVariableType.VITESSE_NORMALE);
        _s.add(H2dVariableType.VITESSE_TANGENTIELLE);
        _s.add(H2dVariableType.COTE_EAU);
        _s.add(H2dVariableType.DEBIT);
        _s.add(H2dVariableType.RUGOSITE);
    }

    public void fireBcFrontierStructureChanged(final H2dBcFrontierInterface _frontier) {
        super.fireBcFrontierStructureChanged(_frontier);
    }

    public void fireBcParametersChanged(final H2dBoundary _b) {
        super.fireBcParametersChanged(_b);
    }

    public CtuluPermanentList getAllowedBordTypeList() {
        return new CtuluPermanentList(getBoundaryTypeComportMap().keySet());
    }

    /**
   * @param _idxFrontiere l'indice de la frontiere demandee
   * @return la frontiere (pas de controle sur l'indice)
   */
    public H2dBcFrontierInterface getBcFrontier(final int _idxFrontiere) {
        return bcFrontier_[_idxFrontiere];
    }

    /**
   * @param _idxFrontiere l'indice de la frontiere demandee
   * @return la frontiere (pas de controle sur l'indice)
   */
    public H2dBcFrontierMiddleInterface getMiddleFrontier(final int _idxFrontiere) {
        return bcFrontier_[_idxFrontiere];
    }

    public int getNbBoundaryType() {
        return getAllowedBordTypeList().size();
    }

    /**
   * @return les bords sous la forme fichier inp.
   */
    public H2dRefluxBordIndexGeneral[] getRefluxBord() {
        int n = bcFrontier_.length;
        final List r = new ArrayList();
        for (int i = 0; i < n; i++) {
            r.addAll(((RefluxMiddleFrontier) bcFrontier_[i]).getRefluxIndexGeneralBords());
        }
        n = r.size();
        if (n > 0) {
            final H2dRefluxBordIndexGeneral[] rfinal = new H2dRefluxBordIndexGeneral[n];
            r.toArray(rfinal);
            return rfinal;
        }
        return null;
    }

    /**
   * @return toutes les conditions limites de toutes les frontieres (dans l'ordre telemac)
   */
    public H2dRefluxBoundaryCondition[] getRefluxCl() {
        final H2dRefluxBoundaryCondition[] r = new H2dRefluxBoundaryCondition[maillage_.getFrontiers().getNbTotalPt()];
        final int n = bcFrontier_.length;
        int idx = 0;
        for (int i = 0; i < n; i++) {
            final RefluxMiddleFrontier fr = (RefluxMiddleFrontier) bcFrontier_[i];
            final int nbPoint = fr.getNbPt();
            boolean middle = false;
            for (int j = 0; j < nbPoint; j++) {
                r[idx] = new H2dRefluxBoundaryCondition(fr.bcArray_[j]);
                if (middle && (r[idx].getHType() != H2dBcType.LIBRE)) {
                    r[idx].copyCurve(null);
                    r[idx].setHTypeFree();
                }
                idx++;
                middle = !middle;
            }
        }
        if (idx != r.length) {
            new Throwable().printStackTrace();
        }
        return r;
    }

    /**
   * @param _idxFrontier l'indice de la frontiere demande
   * @return la frontiere demandee
   */
    public RefluxMiddleFrontier getRefluxMiddleFrontier(final int _idxFrontier) {
        return (RefluxMiddleFrontier) getBcFrontier(_idxFrontier);
    }

    public List getUsedBoundaryType() {
        final Set s = new HashSet(getAllowedBordTypeList().size());
        for (int i = bcFrontier_.length - 1; i >= 0; i--) {
            ((RefluxMiddleFrontier) bcFrontier_[i]).fillListWithUsedBoundary(s);
        }
        final List r = new ArrayList(s);
        Collections.sort(r);
        return r;
    }

    /**
   * @param _l le listener a enlever
   */
    public void removeClListener(final H2dRefluxBcListener _l) {
        if (listeners_ != null) {
            listeners_.remove(_l);
        }
    }
}
