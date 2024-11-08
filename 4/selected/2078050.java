package mirrormonkey.state.member.outbound;

import mirrormonkey.framework.Updatable;
import mirrormonkey.framework.entity.StaticEntityData.MemberDataKey;
import mirrormonkey.framework.member.DynamicMemberData;
import mirrormonkey.framework.parameter.ParameterInterpreter;
import mirrormonkey.state.member.StaticMemberStateData;
import mirrormonkey.state.member.accessor.ValueReadAccessor;
import mirrormonkey.state.member.accessor.ValueWriteAccessor;
import mirrormonkey.state.module.StateUpdateModule;

public class OutboundStaticMemberStateData extends StaticMemberStateData implements Updatable {

    public final float updateRate;

    public float sinceLastUpdate;

    public OutboundStaticMemberStateData(int id, MemberDataKey memberKey, StateUpdateModule updateModule, ParameterInterpreter interpreter, float updateRate, ValueWriteAccessor writeAccessor, ValueReadAccessor readAccessor) {
        super(id, memberKey, updateModule, interpreter, writeAccessor, readAccessor);
        this.updateRate = updateRate;
        sinceLastUpdate = 0f;
    }

    @Override
    public DynamicMemberData createDynamicData() {
        return new OutboundDynamicMemberStateData();
    }

    @Override
    public void update(float tpf) {
        if ((sinceLastUpdate += tpf) >= updateRate) {
            sinceLastUpdate = 0;
            updateModule.fieldUpdatedThisFrame(this);
        }
    }
}
