package mirrormonkey.state.member;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mirrormonkey.framework.SyncAppState;
import mirrormonkey.framework.annotations.IdentityAware;
import mirrormonkey.framework.entity.StaticEntityDataIR;
import mirrormonkey.framework.member.MemberDataIR;
import mirrormonkey.framework.member.StaticMemberData;
import mirrormonkey.state.annotations.ReceiveStateFrom;
import mirrormonkey.state.annotations.TrackValue;
import mirrormonkey.state.annotations.UpdateState;
import mirrormonkey.state.member.StaticMemberStateData.StateMemberKey;
import mirrormonkey.state.member.accessor.FieldReadAccessor;
import mirrormonkey.state.member.accessor.FieldWriteAccessor;
import mirrormonkey.state.member.accessor.GetterReadAccessor;
import mirrormonkey.state.member.accessor.SetterWriteAccessor;
import mirrormonkey.state.member.accessor.ValueReadAccessor;
import mirrormonkey.state.member.accessor.ValueWriteAccessor;
import mirrormonkey.state.member.inbound.InboundStaticMemberStateData;
import mirrormonkey.state.member.outbound.OutboundStaticMemberStateData;
import mirrormonkey.state.member.outbound.TrackingOutboundStaticMemberStateData;
import mirrormonkey.state.module.StateUpdateModule;
import mirrormonkey.util.annotations.hfilter.ClassFilter;
import mirrormonkey.util.annotations.parsing.AnnotationParser;
import mirrormonkey.util.annotations.parsing.BeanFieldIR;

public class StaticMemberStateDataIR extends BeanFieldIR implements MemberDataIR {

    public final StateMemberKey memberKey;

    public StaticMemberStateDataIR(AnnotationParser parser, Field element) {
        super(parser, element);
        memberKey = new StateMemberKey(element.getName(), element.getType());
        addCollectType(UpdateState.class);
        addCollectType(IdentityAware.class);
        addCollectType(ReceiveStateFrom.class);
        addCollectType(TrackValue.class);
    }

    public StaticMemberStateDataIR(StaticMemberStateDataIR previous, Field element) {
        super(previous, element);
        memberKey = previous.memberKey;
    }

    public StaticMemberStateDataIR(AnnotationParser parser, Method element) {
        super(parser, element);
        String name = element.getName().substring(3, 4).toLowerCase();
        name = name + element.getName().substring(4);
        Class<?> type;
        if (element.getName().startsWith("get")) {
            type = element.getReturnType();
        } else {
            type = element.getParameterTypes()[0];
        }
        memberKey = new StateMemberKey(name, type);
        addCollectType(UpdateState.class);
        addCollectType(IdentityAware.class);
        addCollectType(ReceiveStateFrom.class);
        addCollectType(TrackValue.class);
    }

    public StaticMemberStateDataIR(StaticMemberStateDataIR previous, Method element) {
        super(previous, element);
        memberKey = previous.memberKey;
    }

    @Override
    public int compareTo(MemberDataIR o) {
        if (getSortingOrder() != o.getSortingOrder()) {
            return Integer.valueOf(getSortingOrder()).compareTo(Integer.valueOf(o.getSortingOrder()));
        }
        StaticMemberStateDataIR other = (StaticMemberStateDataIR) o;
        int comp = memberKey.name.compareTo(other.memberKey.name);
        if (comp != 0) {
            return comp;
        }
        return memberKey.getClass().getName().compareTo(other.memberKey.getClass().getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!StaticMemberStateDataIR.class.isInstance(o)) {
            return false;
        }
        return memberKey.equals(((StaticMemberStateDataIR) o).memberKey);
    }

    @Override
    public StaticMemberData extractData(int id, StaticEntityDataIR staticEntityDataIR, SyncAppState<?> appState, MemberDataIR connectedMemberIR) {
        ValueReadAccessor readAccessor = getter.getCollectedMethod() == null ? new FieldReadAccessor(field.getCollectedField()) : new GetterReadAccessor(getter.getCollectedMethod());
        ValueWriteAccessor writeAccessor = setter.getCollectedMethod() == null ? new FieldWriteAccessor(field.getCollectedField()) : new SetterWriteAccessor(setter.getCollectedMethod());
        UpdateState annot = getCollectedAnnotation(UpdateState.class);
        TrackValue track = getCollectedAnnotation(TrackValue.class);
        ReceiveStateFrom receiveFrom = getCollectedAnnotation(ReceiveStateFrom.class);
        boolean allowInbound = ClassFilter.Eval.contains(receiveFrom.value(), staticEntityDataIR.connectedClass);
        boolean allowOutbound = ClassFilter.Eval.contains(((StaticMemberStateDataIR) connectedMemberIR).getCollectedAnnotation(ReceiveStateFrom.class).value(), staticEntityDataIR.localClass);
        if (allowInbound && allowOutbound) {
            throw new IllegalStateException("Can not send and receive state at the same time:\nlocal class: " + staticEntityDataIR.localClass + "\nconnected class: " + staticEntityDataIR.connectedClass);
        } else if (!allowInbound && !allowOutbound) {
            return null;
        }
        if (allowInbound) {
            return new InboundStaticMemberStateData(id, memberKey, appState.getModule(StateUpdateModule.class), appState.entityProvider.getInterpreter(getCollectedAnnotation(IdentityAware.class)), writeAccessor, readAccessor);
        } else if (track.value() == true) {
            return new TrackingOutboundStaticMemberStateData(id, memberKey, appState.getModule(StateUpdateModule.class), appState.entityProvider.getInterpreter(getCollectedAnnotation(IdentityAware.class)), annot.freq(), writeAccessor, readAccessor);
        } else {
            return new OutboundStaticMemberStateData(id, memberKey, appState.getModule(StateUpdateModule.class), appState.entityProvider.getInterpreter(getCollectedAnnotation(IdentityAware.class)), annot.freq(), writeAccessor, readAccessor);
        }
    }

    @Override
    public int getSortingOrder() {
        return 200;
    }
}
