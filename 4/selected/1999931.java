package net.sf.woko.facets.edit;

import net.sf.woko.facets.FacetConstants;
import net.sf.woko.facets.view.ViewObjectProperties;
import net.sf.woko.persistence.PersistenceUtil;
import net.sourceforge.jfacets.annotations.FacetKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Facet used to render the properties of the target object in edit-mode.
 * <br/><br/>
 * <b>Assignation details :</b>
 * <ul>
 * <li><b>name</b> : editProperties</li>
 * <li><b>profileId</b> : ROLE_ALL</li>
 * <li><b>targetObjectType</b> : Object</li>
 * </ul>
 */
@FacetKey(name = FacetConstants.editProperties, profileId = "ROLE_ALL")
public class EditObjectProperties extends ViewObjectProperties {

    /** the id of the object */
    private String id;

    /** the className of the object */
    private String className;

    /** a list of booleans indicating which props are read-only */
    private List<Boolean> writeAllowed;

    /**
	 * set id and className from target object, and return the 
	 * path to the fragment to be used for rendering the 
	 * properties :
	 * <code>/WEB-INF/woko/fragments/edit/properties.jsp</code>
	 */
    @Override
    public String getFragmentPath() {
        super.getFragmentPath();
        Object targetObject = getContext().getTargetObject();
        id = getPersistenceUtil().getId(targetObject);
        className = PersistenceUtil.deproxifyCglibClass(targetObject.getClass()).getName();
        getRequest().setAttribute("targetObject", targetObject);
        return "/WEB-INF/woko/fragments/edit/properties.jsp";
    }

    /**
	 * return a list of Boolean that has the same size 
	 * than the one returned by getPropertyNames with 
	 * the edit mode (writable or not).
	 * Uses getReadOnlyPropertyNames() to set the values.
	 */
    public final List<Boolean> getWriteAllowed() {
        if (writeAllowed == null) {
            writeAllowed = new ArrayList<Boolean>();
            List<String> readOnlyPropNames = getReadOnlyPropertyNames();
            if (readOnlyPropNames == null) readOnlyPropNames = new ArrayList<String>();
            for (String pName : getPropertyNames()) {
                if (readOnlyPropNames.contains(pName)) writeAllowed.add(Boolean.FALSE); else writeAllowed.add(Boolean.TRUE);
            }
        }
        return writeAllowed;
    }

    /**
	 * Return a list of read-only property names.
	 * In this implementation, returns null : all props are writable. 
	 * You may override this method in your facets in order to disable 
	 * the editing of some props for some objects and some profiles.
	 */
    public List<String> getReadOnlyPropertyNames() {
        return null;
    }

    public String getClassName() {
        return className;
    }

    public String getId() {
        return id;
    }
}
