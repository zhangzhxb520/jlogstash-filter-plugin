package com.dtstack.jlogstash.filters;

import java.util.HashMap;
import java.util.Map;

/**
 * 聚合Map
 *
 * @author zxb
 * @version 1.0.0
 *          2017年03月29日 09:03
 * @since Jdk1.6
 */
public class AggregateMap {

    private Map map;

    private long createTime;

    private String taskId;

    public AggregateMap(String taskId, long createTime) {
        this.taskId = taskId;
        this.createTime = createTime;
        this.map = new HashMap();
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
