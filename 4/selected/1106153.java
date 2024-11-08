package db;

import java.util.HashSet;
import java.util.Set;

public class Channel {

    private Long id;

    private String channelName;

    private String password;

    private String displayName;

    private Set<Account> members = new HashSet<Account>();

    protected Channel() {
    }

    public Set<Account> getMembers() {
        return members;
    }

    protected void setMembers(Set<Account> value) {
        members = value;
    }

    public void addMember(Account member) {
        members.add(member);
        member.getChannels().add(this);
    }

    public void removeMember(Account member) {
        members.remove(member);
        member.getChannels().remove(this);
    }

    public Channel(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String toString() {
        return String.format("[%d]%s (%s)", getId(), getChannelName(), getDisplayName());
    }

    public void __printMembers() {
        for (Account a : members) {
            System.out.println(a);
        }
    }
}
