package mirrormonkey.state.member.inbound;

import mirrormonkey.framework.entity.StaticEntityData.MemberDataKey;
import mirrormonkey.framework.member.DynamicMemberData;
import mirrormonkey.framework.parameter.ParameterInterpreter;
import mirrormonkey.state.member.StaticMemberStateData;
import mirrormonkey.state.member.accessor.ValueReadAccessor;
import mirrormonkey.state.member.accessor.ValueWriteAccessor;
import mirrormonkey.state.module.StateUpdateModule;

public class InboundStaticMemberStateData extends StaticMemberStateData {

    public InboundStaticMemberStateData(int id, MemberDataKey memberKey, StateUpdateModule updateModule, ParameterInterpreter interpreter, ValueWriteAccessor writeAccessor, ValueReadAccessor readAccessor) {
        super(id, memberKey, updateModule, interpreter, writeAccessor, readAccessor);
    }

    @Override
    public DynamicMemberData createDynamicData() {
        return new InboundDynamicMemberStateData();
    }
}
