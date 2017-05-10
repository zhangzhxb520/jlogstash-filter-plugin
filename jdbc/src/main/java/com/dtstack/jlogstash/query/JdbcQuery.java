package com.dtstack.jlogstash.query;

import com.dtstack.jlogstash.config.JdbcConfig;
import com.dtstack.jlogstash.exception.InitializeException;
import com.dtstack.jlogstash.util.JdbcClientUtil;
import org.apache.commons.collections.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用JDBC从数据库加载字典
 *
 * @author zxb
 * @version 1.0.0
 *          2017年03月25日 23:36
 * @since Jdk1.6
 */
public class JdbcQuery {

    private JdbcConfig config;
    private Connection connection;
    public JdbcQuery(JdbcConfig config,Connection connection) {
        this.config = config;
        this.connection = connection;
    }
    public List<Map<Object, Object>> query(List<Object> paramValues) {
        PreparedStatement ppst = null;
        ResultSet resultSet = null;
        try {
            ppst = connection.prepareStatement(config.getStatement());
            //设置参数
            if (CollectionUtils.isNotEmpty(paramValues)) {
                for (int i = 1, len = paramValues.size(); i <= len; i++) {
                    Object value = paramValues.get(i - 1);
                    ppst.setObject(i, value);
                }
            }
            resultSet = ppst.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int cols = resultSetMetaData.getColumnCount();
            Map<Object, Object> resultMap;
            List<Map<Object, Object>> resultList = new ArrayList<Map<Object, Object>>();
            while (resultSet.next()) {
                resultMap = new HashMap<Object, Object>();
                for (int i = 1; i <= cols; i++) {
                    String columnName = resultSetMetaData.getColumnName(i);
                    Object value = resultSet.getObject(columnName);
                    resultMap.put(columnName, value);
                }
                resultList.add(resultMap);
            }
            return resultList;
        } catch (Exception e) {
            throw new InitializeException(e);
        } finally {
            JdbcClientUtil.releaseConnection(null, ppst, resultSet);
        }
    }
}
