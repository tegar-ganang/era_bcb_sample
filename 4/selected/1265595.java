package mirrormonkey.state.member.outbound;

import mirrormonkey.framework.entity.StaticEntityData.MemberDataKey;
import mirrormonkey.framework.member.DynamicMemberData;
import mirrormonkey.framework.parameter.ParameterInterpreter;
import mirrormonkey.state.member.accessor.ValueReadAccessor;
import mirrormonkey.state.member.accessor.ValueWriteAccessor;
import mirrormonkey.state.module.StateUpdateModule;

public class TrackingOutboundStaticMemberStateData extends OutboundStaticMemberStateData {

    public TrackingOutboundStaticMemberStateData(int id, MemberDataKey memberKey, StateUpdateModule updateModule, ParameterInterpreter interpreter, float updateRate, ValueWriteAccessor writeAccessor, ValueReadAccessor readAccessor) {
        super(id, memberKey, updateModule, interpreter, updateRate, writeAccessor, readAccessor);
    }

    @Override
    public DynamicMemberData createDynamicData() {
        return new TrackingOutboundDynamicMemberStateData();
    }
}
