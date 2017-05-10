package com.dtstack.jlogstash.filters;

import com.dtstack.jlogstash.annotation.Required;
import com.dtstack.jlogstash.config.JdbcConfig;
import com.dtstack.jlogstash.exception.InitializeException;
import com.dtstack.jlogstash.query.JdbcQuery;
import com.dtstack.jlogstash.util.JdbcClientUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wdd
 * @version 1.0.0
 *          2017年05月10日 10.39
 * @since Jdk1.6
 */
public class Jdbc extends BaseFilter {
    private static final Logger logger = LoggerFactory.getLogger(Jdbc.class);
    @Required(required = true)
    private static List<String> params;
    @Required(required = true)
    private static String jdbc_connection_string;
    @Required(required = true)
    private static String jdbc_driver_class;
    @Required(required = true)
    private static String jdbc_driver_library;
    @Required(required = true)
    private static String jdbc_user;
    @Required(required = true)
    private static String jdbc_password;
    @Required(required = true)
    private static String statement;
    @Required(required = true)
    private static String target;

    private static JdbcConfig config = new JdbcConfig();
    private static Connection connection;

    public Jdbc(Map config) {
        super(config);
    }

    public void prepare() {
        init();
    }

    protected Map filter(Map event) {
        List<Object> paramValues = new ArrayList<Object>();
        if (CollectionUtils.isNotEmpty(params)) {
            for (String paramKey : params) {
                Object value = event.get(paramKey);
                if (value == null) {
                    throw new InitializeException("参数不匹配");
                }
                paramValues.add(value);
            }
        }
        JdbcQuery jdbcQuery = new JdbcQuery(config , connection);
        List<Map<Object, Object>> resultList = jdbcQuery.query(paramValues);
        event.put(target, resultList);
        return event;
    }

    @Override
    public void release() {
        if(connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("close connection is error");
            }
        }

    }

    private void init() {
        config.setJdbc_user(jdbc_user);
        config.setJdbc_password(jdbc_password);
        config.setJdbc_driver_library(jdbc_driver_library);
        config.setJdbc_driver_class(jdbc_driver_class);
        config.setJdbc_connection_string(jdbc_connection_string);
        config.setStatement(statement);
        try {
            if(connection == null){
                connection = JdbcClientUtil.getConnection(config);
            }
        } catch (MalformedURLException e) {
            logger.error("init jdbc connection is faild" , e);
        } catch (ClassNotFoundException e) {
            logger.error("init jdbc connection is faild" , e);
        } catch (SQLException e) {
            logger.error("init jdbc connection is faild" , e);
        } catch (IllegalAccessException e) {
            logger.error("init jdbc connection is faild" , e);
        } catch (InstantiationException e) {
            logger.error("init jdbc connection is faild" , e);
        }

        if(connection == null){
            throw new InitializeException("初始化JDBC链接失败");
        }
    }
}
