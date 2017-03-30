package com.dtstack.jlogstash.loader;

import org.apache.commons.lang3.StringUtils;

/**
 * 字典加载器类型
 *
 * @author zxb
 * @version 1.0.0
 *          2017年03月25日 23:39
 * @since Jdk1.6
 */
public enum DicLoaderType {

    FILE("file"),

    INLINE("inline"),

    JDBC("jdbc");

    private String name;

    private DicLoaderType(String name) {
        this.name = name;
    }

    public static DicLoaderType getByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        for (DicLoaderType type : DicLoaderType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
