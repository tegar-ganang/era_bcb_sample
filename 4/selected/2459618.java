package org.scribble.comparator;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.scribble.model.*;
import org.scribble.model.admin.ModelListener;
import org.scribble.extensions.RegistryInfo;

/**
 * This class provides the Interaction comparator rule.
 */
@RegistryInfo(extension = ComparatorRule.class)
public class InteractionComparatorRule implements ComparatorRule {

    /**
	 * This method determines whether the comparison rule is
	 * associated with the supplied type.
	 * 
	 * @param obj The object to check
	 * @return Whether the object is of a type supported by the
	 * 					comparison rule
	 */
    public boolean isTypeSupported(ModelObject obj) {
        return (obj instanceof Interaction);
    }

    /**
	 * This method determines whether the comparison rule is
	 * appropriate for the supplied model objects.
	 * 
	 * @param main The main model object to be compared
	 * @param ref The reference model object to be compared against
	 * @return Whether the rule is relevant for the
	 * 				model objects
	 */
    public boolean isComparisonSupported(ModelObject main, ModelObject ref) {
        return (main instanceof Interaction && ref instanceof Interaction);
    }

    /**
	 * This method compares a model object against a reference
	 * component to determine if they are equal.
	 * 
	 * @param context The context
	 * @param main The main model object
	 * @param reference The reference model object
	 * @param l The model listener
	 * @param deep Perform a deep compare
	 * @return Whether the model objects are comparable
	 */
    public boolean compare(ComparatorContext context, ModelObject main, ModelObject reference, ModelListener l, boolean deep) {
        boolean ret = false;
        Interaction maini = (Interaction) main;
        Interaction refi = (Interaction) reference;
        ret = context.compare(maini.getMessageSignature(), refi.getMessageSignature(), l, deep);
        if (ret) {
            Role fromRoleMain = findRole(maini, true);
            Role fromRoleRef = findRole(refi, true);
            Role toRoleMain = findRole(maini, false);
            Role toRoleRef = findRole(refi, false);
            if (ret && fromRoleMain != null && fromRoleRef != null) {
                ret = context.compare(fromRoleMain, fromRoleRef, l, deep);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Interaction " + this + ": compare 'from' roles: " + fromRoleMain + " against " + fromRoleRef + " = " + ret);
                }
            }
            if (ret && toRoleMain != null && toRoleRef != null) {
                ret = context.compare(toRoleMain, toRoleRef, l, deep);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Interaction " + this + ": compare 'to' roles: " + toRoleMain + " against " + toRoleRef + " = " + ret);
                }
            }
        }
        return (ret);
    }

    protected Role findRole(Interaction interaction, boolean from) {
        Role ret = null;
        if (from) {
            ret = interaction.getFromRole();
            if (ret == null && interaction.getChannel() != null) {
                ret = interaction.getChannel().getFromRole();
            }
            if (ret == null) {
                Role other = interaction.getToRole();
                if (other == null && interaction.getChannel() != null) {
                    other = interaction.getChannel().getToRole();
                }
                if (other != null) {
                    Definition defn = interaction.getEnclosingDefinition();
                    if (defn != null && defn.getLocatedName() != null) {
                        ret = defn.getLocatedName().getRole();
                        if (ret != null && ret.equals(other)) {
                            ret = null;
                        }
                    }
                }
            }
        } else {
            ret = interaction.getToRole();
            if (ret == null && interaction.getChannel() != null) {
                ret = interaction.getChannel().getToRole();
            }
            if (ret == null) {
                Role other = interaction.getFromRole();
                if (other == null && interaction.getChannel() != null) {
                    other = interaction.getChannel().getFromRole();
                }
                if (other != null) {
                    Definition defn = interaction.getEnclosingDefinition();
                    if (defn != null && defn.getLocatedName() != null) {
                        ret = defn.getLocatedName().getRole();
                        if (ret != null && ret.equals(other)) {
                            ret = null;
                        }
                    }
                }
            }
        }
        return (ret);
    }

    private static Logger logger = Logger.getLogger("org.scribble.comparator");
}
