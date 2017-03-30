package com.dtstack.jlogstash.constans;

/**
 * 配置常量
 *
 * @author zxb
 * @version 1.0.0
 *          2017年03月29日 14:50
 * @since Jdk1.6
 */
public class TimeOutConfigConstans {


    /**
     * timeout_code模板名称
     */
    public static final String TIMEOUT_CODE_TEMPLATE_NAME = "timeout_code.ftl";


    /**
     * Timeout Code所对应的工具类的className
     */
    public static final String TIMEOUT_CODE_CLASS_NAME = "com.dtstack.jlogstash.filters.AggregateTimeoutCodeUtil";

    /**
     * AggregateTimeoutCodeUtil类全名称
     */
    public static final String CODE_CLASS_NAME = "com.dtstack.jlogstash.filters.AggregateTimeoutCodeUtil";

    /**
     * 处理方法名称
     */
    public static final String METHOD_NAME = "handle";
}
