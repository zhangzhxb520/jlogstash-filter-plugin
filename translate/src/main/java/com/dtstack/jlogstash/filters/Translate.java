package com.dtstack.jlogstash.filters;

import com.dtstack.jlogstash.annotation.Required;
import com.dtstack.jlogstash.config.JdbcConfig;
import com.dtstack.jlogstash.loader.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("ALL")
public class Translate extends BaseFilter {

    private static final Logger logger = LoggerFactory.getLogger(Translate.class);
    // 私有属性
    private static Map dictionary;
    private static boolean ignore;
    private static String target;
    // 配置
    @Required(required = true)
    private static String source;
    /**
     * 字典类型，可选值：jdbc,file,inline
     */
    private static String dicType;
    private static String dictionaryPath;
    // jdbc连接配置
    private static String jdbc_connection_string;
    private static String jdbc_driver_class;
    private static String jdbc_driver_library;
    private static String jdbc_user;
    private static String jdbc_password;
    private static String statement;
    private static DicLoader dicLoader;
    private int refreshInterval;
    private long nextLoadTime;

    public Translate(Map config) {
        super(config);
    }

    @Override
    public void prepare() {
        if (StringUtils.isEmpty(target)) {
            target = source; // 默认替换到来源字段
        }

        if (dictionary == null) {
            init();
            dictionary = dicLoader.load();

            if (dictionary == null) {
                ignore = true;
                logger.info("no dictionary found, ignore...");
            }
        }
    }

    @Override
    protected Map filter(final Map event) {
        // 字典值替换
        if (!ignore) {
            Object code = event.get(source);
            Object value = dictionary.get(code);
            if (value != null) {
                event.put(target, value);
            }
        }
        return event;
    }

    private void init() {
        DicLoaderType dicLoaderType = DicLoaderType.getByName(dicType);
        if (dicLoaderType == null) {
            dicLoaderType = DicLoaderType.FILE;
        }

        if (dicLoaderType == DicLoaderType.FILE) {
            dicLoader = new FileDicLoader(dictionaryPath);
        } else if (dicLoaderType == DicLoaderType.JDBC) {
            JdbcConfig config = new JdbcConfig();
            config.setJdbc_user(jdbc_user);
            config.setJdbc_password(jdbc_password);
            config.setJdbc_driver_library(jdbc_driver_library);
            config.setJdbc_driver_class(jdbc_driver_class);
            config.setJdbc_connection_string(jdbc_connection_string);
            config.setStatement(statement);

            dicLoader = new JdbcDicLoader(config);
        } else {
            dicLoader = new InLineDicLoader(dictionary);
        }
    }
}
