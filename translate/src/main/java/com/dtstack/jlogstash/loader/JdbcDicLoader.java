package com.dtstack.jlogstash.loader;

import com.dtstack.jlogstash.config.JdbcConfig;
import com.dtstack.jlogstash.exception.InitializeException;
import com.dtstack.jlogstash.util.JdbcClientUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 使用JDBC从数据库加载字典
 *
 * @author zxb
 * @version 1.0.0
 *          2017年03月25日 23:36
 * @since Jdk1.6
 */
public class JdbcDicLoader implements DicLoader {

    private JdbcConfig config;

    public JdbcDicLoader(JdbcConfig config) {
        this.config = config;
    }

    public Map load() {
        Connection connection = null;
        PreparedStatement ppst = null;
        ResultSet resultSet = null;
        try {
            connection = JdbcClientUtil.getConnection(config);
            ppst = connection.prepareStatement(config.getStatement());
            resultSet = ppst.executeQuery();

            Object key;
            Object value;
            Map<Object, Object> dicMap = new HashMap<Object, Object>();

            while (resultSet.next()) {
                key = resultSet.getObject(1);
                value = resultSet.getObject(2);
                dicMap.put(key, value);
            }
            return dicMap;
        } catch (Exception e) {
            throw new InitializeException(e);
        } finally {
            JdbcClientUtil.releaseConnection(connection, ppst, resultSet);
        }
    }
}
