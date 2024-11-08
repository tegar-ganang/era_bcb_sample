package mirrormonkey.state.member;

import mirrormonkey.framework.entity.StaticEntityData;
import mirrormonkey.framework.entity.StaticEntityData.MemberDataKey;
import mirrormonkey.framework.member.StaticMemberData;
import mirrormonkey.framework.parameter.ParameterInterpreter;
import mirrormonkey.state.member.accessor.ValueReadAccessor;
import mirrormonkey.state.member.accessor.ValueWriteAccessor;
import mirrormonkey.state.module.StateUpdateModule;

public abstract class StaticMemberStateData implements StaticMemberData {

    public final int id;

    public final MemberDataKey memberKey;

    public final StateUpdateModule updateModule;

    public StaticEntityData staticData;

    public final ParameterInterpreter interpreter;

    public final ValueWriteAccessor writeAccessor;

    public final ValueReadAccessor readAccessor;

    public StaticMemberStateData(int id, MemberDataKey memberKey, StateUpdateModule updateModule, ParameterInterpreter interpreter, ValueWriteAccessor writeAccessor, ValueReadAccessor readAccessor) {
        this.id = id;
        this.memberKey = memberKey;
        this.updateModule = updateModule;
        this.interpreter = interpreter;
        this.writeAccessor = writeAccessor;
        this.readAccessor = readAccessor;
    }

    @Override
    public void setStaticEntityData(StaticEntityData staticData) {
        this.staticData = staticData;
    }

    @Override
    public MemberDataKey getMemberKey() {
        return memberKey;
    }

    public static final class StateMemberKey implements MemberDataKey {

        public final String name;

        public final Class<?> type;

        public StateMemberKey(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return 13 * name.hashCode() + type.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!o.getClass().equals(StateMemberKey.class)) {
                return false;
            }
            StateMemberKey other = (StateMemberKey) o;
            return name.equals(other.name) && type.equals(other.type);
        }

        @Override
        public String toString() {
            return getClass().getName() + ": " + name;
        }
    }
}
