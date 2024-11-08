package com.manydesigns.portofino.base;

import com.manydesigns.portofino.base.workflow.MDWfState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Paolo Predonzani - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo      - angelo.lupo@manydesigns.com
 */
public abstract class MDActor {

    public static final String copyright = "Copyright (c) 2005-2009, ManyDesigns srl";

    private final int id;

    private final MDClass owningClass;

    protected final List<MDActorOperation> operationPermissions;

    protected final List<MDActorAttribute> attributePermissions;

    protected final List<MDActorWfTransition> transitionPermissions;

    /**
     * Creates a new instance of MDActor
     */
    public MDActor(int id, MDClass owningClass) {
        this.id = id;
        this.owningClass = owningClass;
        this.operationPermissions = new ArrayList<MDActorOperation>();
        this.attributePermissions = new ArrayList<MDActorAttribute>();
        this.transitionPermissions = new ArrayList<MDActorWfTransition>();
    }

    public int getId() {
        return id;
    }

    public MDClass getOwningClass() {
        return owningClass;
    }

    public abstract boolean check(MDObject obj) throws Exception;

    public abstract void getUserMail(int oid, Collection userMail) throws Exception;

    public void registerAttributePermission(MDActorAttribute aa) {
        attributePermissions.add(aa);
    }

    public void registerOperationPermission(MDActorOperation ao) {
        operationPermissions.add(ao);
    }

    public void registerWfTransitionPermission(MDActorWfTransition awft) {
        transitionPermissions.add(awft);
    }

    public void fillOperations(Collection refSet, MDObject obj) throws Exception {
        if (!check(obj)) return;
        MDWfState state = obj.getWfState();
        for (MDActorOperation ao : operationPermissions) {
            ao.fillOperations(refSet, state, obj.getActualClass());
        }
    }

    public void fillAttributes(Set readSet, Set writeSet, MDObject obj) throws Exception {
        if (!check(obj)) return;
        MDWfState state = obj.getWfState();
        for (MDActorAttribute aa : attributePermissions) {
            aa.fillAttributes(readSet, writeSet, state);
        }
    }

    public void fillTransitions(Collection refSet, MDObject obj) throws Exception {
        if (!check(obj)) return;
        MDWfState state = obj.getWfState();
        for (MDActorWfTransition awft : transitionPermissions) {
            awft.fillTransitions(refSet, state);
        }
    }

    public boolean canReadSomething(MDWfState state, MDClass cls) {
        for (MDActorAttribute aa : attributePermissions) {
            MDAttribute attr = aa.getAttribute();
            if (aa.getState() == state && cls.isDescendantOf(attr.getOwnerClass())) return true;
        }
        return false;
    }

    public abstract void visit(MDConfigVisitor visitor);

    protected void visitActorPermissions(MDConfigVisitor visitor) {
        visitor.doPermissionListPre();
        for (MDActorOperation actorOperation : operationPermissions) {
            visitor.doActorOperation(actorOperation);
        }
        for (MDActorAttribute attributeOperation : attributePermissions) {
            visitor.doActorAttribute(attributeOperation);
        }
        for (MDActorWfTransition actorWft : transitionPermissions) {
            visitor.doActorWfTransition(actorWft);
        }
        visitor.doPermissionListPost();
    }
}
