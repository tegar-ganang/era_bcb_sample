package com.adobe.txi.todo.dto;

import java.io.Serializable;
import de.aixcept.flex2.annotations.ActionScript;
import de.aixcept.flex2.annotations.ActionScriptProperty;

@ActionScript(bindable = true, remoteObject = true)
public class TodoItemDto implements Serializable {

    private static final long serialVersionUID = 5750553524796306885L;

    private Integer id;

    private String title;

    @ActionScriptProperty(read = true, write = true, bindable = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ActionScriptProperty(read = true, write = true, bindable = true)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
