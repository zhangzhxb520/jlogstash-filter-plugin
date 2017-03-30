package com.dtstack.jlogstash.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @author zxb
 * @version 1.0.0
 *          2017年03月29日 09:06
 * @since Jdk1.6
 */
public enum MapAction {

    CREATE("create"),
    UPDATE("update"),
    CREATE_OR_UPDATE("create_or_update");

    private String name;

    private MapAction(String name) {
        this.name = name;
    }

    public static MapAction getByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        for (MapAction action : MapAction.values()) {
            if (action.getName().equals(name)) {
                return action;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
