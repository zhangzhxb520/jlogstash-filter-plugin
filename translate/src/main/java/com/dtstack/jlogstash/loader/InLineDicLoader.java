package com.dtstack.jlogstash.loader;

import java.util.Map;

/**
 * 配置文件中的行内字典
 * @author zxb
 * @version 1.0.0
 *          2017年03月25日 23:37
 * @since Jdk1.6
 */
public class InLineDicLoader implements DicLoader {

    private Map dic;

    public InLineDicLoader(Map dic){
        this.dic = dic;
    }

    public Map load() {
        return dic;
    }
}
