package com.wwfish.cms.model.sysuser;

import com.wwfish.cms.model.BaseDto;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-18
 * Time: 10:43:20
 * To change this template use File | Settings | File Templates.
 */
public class SysRoleDto extends BaseDto {

    private String name;

    private String description;

    private String type;

    private List<AccessItemDto> menuAccesses;

    private List<AccessItemDto> channelAccesses;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AccessItemDto> getMenuAccesses() {
        return menuAccesses;
    }

    public void setMenuAccesses(List<AccessItemDto> menuAccesses) {
        this.menuAccesses = menuAccesses;
    }

    public List<AccessItemDto> getChannelAccesses() {
        return channelAccesses;
    }

    public void setChannelAccesses(List<AccessItemDto> channelAccesses) {
        this.channelAccesses = channelAccesses;
    }

    public void addMenuAccess(AccessItemDto dto) {
        if (menuAccesses == null) menuAccesses = new ArrayList();
        for (Iterator it = menuAccesses.iterator(); it.hasNext(); ) {
            AccessItemDto temp = (AccessItemDto) it.next();
            if (temp.getCode().equals(dto.getCode())) break;
        }
        menuAccesses.add(dto);
    }

    public void setChannelAccessListAndCheck(List<AccessItemDto> accesses) {
        channelAccesses = setAccessListAndCheck(channelAccesses, accesses);
    }

    public void setMenuAccessListAndCheck(List<AccessItemDto> accesses) {
        menuAccesses = setAccessListAndCheck(menuAccesses, accesses);
    }

    private List setAccessListAndCheck(List<AccessItemDto> oldAs, List<AccessItemDto> accesses) {
        List result = new ArrayList();
        if (accesses == null) return oldAs;
        if (oldAs == null) return accesses; else {
            for (Iterator it = accesses.iterator(); it.hasNext(); ) {
                AccessItemDto newDto = (AccessItemDto) it.next();
                for (Iterator iv = oldAs.iterator(); iv.hasNext(); ) {
                    AccessItemDto dto = (AccessItemDto) iv.next();
                    if (newDto.getCode().equals(dto.getCode())) {
                        result.add(dto);
                    }
                }
                result.add(newDto);
            }
            return result;
        }
    }
}
