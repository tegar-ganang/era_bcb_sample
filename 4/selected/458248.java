package edu.url.lasalle.campus.scorm2004rte.server.ActivityTree.Elements;

import java.io.Serializable;

/** <!-- Javadoc -->
 * $Id: MapInfo.java,v 1.6 2008/04/22 18:18:07 ecespedes Exp $ 
 * <b>T�tol:</b> MapInfo <br /><br />
 * <b>Descripci�:</b> �s el contenidor de la descripci� mapejada de<br>
 * l'objectiu. Aix� defineix el mapeix de la informaci� de l'objectiu<br>
 * local d'una activitat de i per a un objectiu global compartit.<br>
 * <br>
 * Per llegir Objectives Maps (read{SatisfiedStatus|NormalizedMeasure}):<br>
 * 	Si existeixen m�ltiples elements <mapInfo> per a un objectiu,<br>
 * aleshores nom�s un <mapInfo> pot tindre el readSatisfiedStatus a true.<br>
 * El mateix succeeix per a readNormalizedMeasure.<br>
 * <br>
 * Per escriure Objectives Maps (write{SatisfiedStatus|NormalizedMeasure}):<br>
 * Per una activitat, si hi ha m�ltiples objectius que tenen l'element<br> 
 * <mapInfo> compartint el mateix targetObjectiveID, aleshores nom�s un dels<br>
 * objectius pot tindre el writeNormalizedMeasure a true. El mateix succeeix<br>
 * amb l'atribut writeSatisfiedStatus. 
 * <br><br>
 *
 * @author Eduard C�spedes i Borr�s /Enginyeria La Salle/ ecespedes@salleurl.edu
 * @version Versi� $Revision: 1.6 $ $Date: 2008/04/22 18:18:07 $
 * $Log: MapInfo.java,v $
 * Revision 1.6  2008/04/22 18:18:07  ecespedes
 * Arreglat bugs en el seq�enciament i els objectius secundaris mapejats.
 *
 * Revision 1.5  2008/01/18 18:07:24  ecespedes
 * Serialitzades TOTES les classes per tal de que els altres puguin fer proves
 * en paral�lel amb el proc�s de desenvolupament del gestor de BD.
 *
 * Revision 1.4  2007/12/17 15:27:48  ecespedes
 * Fent MapInfo. Bug en els Leaf_Items
 *
 * Revision 1.3  2007/12/05 09:34:19  xgumara
 * Implementat m�tode equals i hashCode.
 *
 * Revision 1.2  2007/11/11 22:22:06  ecespedes
 * Creat l'estructura TreeAnnotations, que �s per marcar si un item
 * s'ha de tornar a comprovar el seu seq�enciament abans d'enviar
 *  a l'usuari.
 *
 *
 */
public class MapInfo implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -117257960857968122L;

    /**
	 * targetObjectiveID: L'identificador de l'objecte global compartit.
	 * 
	 * String Type: required.
	 */
    public String targetObjectiveID = null;

    /**
	 * readSatisfiedStatus: Es llegir� la informaci� de l'objecte global 
	 * compartit i se li assignar� a l'objecte local. En aquest cas es 
	 * llegir� l'estat de progr�s.
	 *  
	 * boolean Type: Default true.
	 */
    public boolean readSatisfiedStatus = true;

    /**
	 * writeSatisfiedStatus: Es transferir� l'estat de progr�s de l'objecte
	 * local a l'objecte global compartit.
	 *  
	 * boolean Type: Default false.
	 */
    public boolean writeSatisfiedStatus = false;

    /**
	 * readNormalizedMeasure: Es llegir� la informaci� de l'objecte global 
	 * compartit i se li assignar� a l'objecte local.
	 *  
	 * boolean Type: Default true.
	 */
    public boolean readNormalizedMeasure = true;

    /**
	 * writeNormalizedMeasure: Igual per� amb el valor de la mesura 
	 * normalitzada.
	 *  
	 * boolean Type: Default false.
	 */
    public boolean writeNormalizedMeasure = false;

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        MapInfo mapInfo = (MapInfo) obj;
        return (readNormalizedMeasure == mapInfo.readNormalizedMeasure && readSatisfiedStatus == mapInfo.readSatisfiedStatus && targetObjectiveID.equals(mapInfo.targetObjectiveID) && writeNormalizedMeasure == mapInfo.writeNormalizedMeasure && writeSatisfiedStatus == mapInfo.writeSatisfiedStatus);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
