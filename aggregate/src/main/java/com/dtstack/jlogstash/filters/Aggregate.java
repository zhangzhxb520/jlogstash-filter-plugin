package com.dtstack.jlogstash.filters;

import com.dtstack.jlogstash.annotation.Required;
import com.dtstack.jlogstash.assembly.pthread.FilterThread;
import com.dtstack.jlogstash.callback.FilterThreadSetter;
import com.dtstack.jlogstash.compiler.CodeCompiler;
import com.dtstack.jlogstash.constans.CodeConstans;
import com.dtstack.jlogstash.constans.TimeOutConfigConstans;
import com.dtstack.jlogstash.enums.MapAction;
import com.dtstack.jlogstash.exception.InitializeException;
import com.dtstack.jlogstash.render.Formatter;
import com.dtstack.jlogstash.render.FreeMarkerRender;
import com.dtstack.jlogstash.util.TemplateUtil;
import com.dtstack.jlogstash.utils.EventDecorator;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聚合过滤插件
 * @author zxb
 * @version 1.0.0
 *          2017年03月28日 22:05
 * @since Jdk1.6
 */
public class Aggregate extends BaseFilter implements FilterThreadSetter {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregate.class);

    /**
     * 默认超时时间
     */
    private static final Integer DEFAULT_TIMEOUT = 1800;

    /**
     * 聚合Map，以TaskId为Key，AggregateMap为Value存储
     */
    private static Map<String, AggregateMap> aggregateMaps;
    /**
     * 用于定义task ID 的表达式。该值必须唯一的表示一个Task.
     * 例如：
     * <pre>
     *   filters:
     *      - Aggregate:
     *          task_id: "%{type}%{my_task_id}"
     * </pre>
     */
    @Required(required = true)
    private String task_id;
    /**
     * java代码，在该代码中可以使用event和map两个变量。
     * 例如：
     * <pre>
     *     filters:
     *         - Aggregate:
     *                code: '
     *                   Integer duration = event.get("duration");
     *                   if(duration != null){
     *                       Integer mapDuration = map.get("duration");
     *                       mapDuration += duration;
     *                       map.put("duration", mapDuration);
     *                   }
     *                '
     * </pre>
     */
    @Required(required = true)
    private String code;
    /**
     * 指定对当前aggregate map的操作。
     * <ul>
     * <li><span>create</span>: 创建map,并且只有当map先前没有创建过时才执行code</li>
     * <li><span>update</span>: 不创建map,并且只在map已经创建过时才执行code</li>
     * <li><span>create_or_update</span>: 如果map没有创建则创建，不管创没创建map都会执行code</li>
     * </ul>
     */
    private String map_action = "create_or_update";
    /**
     * 用于告诉当前过滤器，task已经结束。
     * 在code执行完成后将删除aggregate map
     */
    private boolean end_of_task = false;
    /**
     * 当JLogstash停止时，该路径用于指定aggregate_map存放的路径，当JLogstash启动时会从该路径重新加载aggregate_map。<br/>
     * 如果未定义，则JLogstash停止时，内存中的aggregate_map将会丢失。<p/>
     * 例如：
     * <pre>
     *     filters:
     *        - Aggregate:
     *            aggregate_maps_path: "/path/to/.aggregate_maps"
     * </pre>
     */
    private String aggregate_maps_path;
    /**
     * 超时时间，单位：秒。如果达到该时间，则认为task的"end event"可能丢失了。timeout后，该task的map将被移除。如果没有timeout指定，将使用默认值1800s.
     */
    private Integer timeout;
    /**
     * 指定当从aggregate map生成timeout事件后，将执行的code
     */
    private String timeout_code;
    /**
     * 当该值为true时，当timeout时，将把已经聚合的map作为一个新的event。
     */
    private boolean push_map_as_event_on_timeout;
    /**
     * 当该值为true时，一旦有新的task_id，则把上一task_id聚合的map作为event
     */
    private boolean push_previous_map_as_event;
    /**
     * 当timeout时，task_id是否写入timeout生成的事件的某一字段。通过该字段可以确定是哪一个task超时了。
     */
    private String timeout_task_id_field;
    /**
     * 指定timeout事件生成时需要添加的tags
     */
    private List<String> timeout_tags;

    /**
     * 驱逐间隔，单位：秒
     */
    private Integer evict_interval;

    private boolean aggregateMapsPathSet;

    /**
     * mapAction枚举表示形式
     */
    private MapAction mapAction;

    /**
     * 上一任务的taskId
     */
    private String lastTaskId;

    private long last_flush_timestamp;

    /**
     * 执行当前Aggregate Filter的FilterThread引用
     */
    private FilterThread filterThread;

    /**
     * 当前Filter之后的Filters
     */
    private List<BaseFilter> nextFilters;

    /**
     * 执行Code代码块的方法
     */
    private Method codeMethod;

    /**
     * 执行Timeout_code代码块的方法
     */
    private Method timeoutMethod;

    private EvictThread evictThread;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public Aggregate(Map config) {
        super(config);
    }

    @Override
    public void prepare() {
        if (aggregateMaps != null) {
            throw new InitializeException("you must set aggregate filter worker size to 1");
        } else {
            aggregateMaps = Maps.newConcurrentMap();
        }

        mapAction = MapAction.getByName(map_action);
        if (mapAction == null) {
            mapAction = MapAction.CREATE_OR_UPDATE;
        }

        codeMethod = initCodeMethod();

        if (StringUtils.isNotEmpty(timeout_code)) {
            timeoutMethod = initTimeoutCodeMethod();
        }

        if (evict_interval == null) {
            evictThread = new EvictThread();
        } else {
            evictThread = new EvictThread(evict_interval * 1000L);
        }
        executor.submit(evictThread);
    }

    /**
     * 每当一个event匹配该filter时将调用该方法
     *
     * @param event
     * @return
     */
    @Override
    protected Map filter(Map event) {
        Map eventToYield = null;

        String taskId = Formatter.format(event, task_id);

        // taskId表达式未翻译，直接忽略
        if (Formatter.isFormat(taskId)) {
            LOGGER.warn("unsupport task_id expression {}", taskId);
            return event;
        }

        // 如果聚合Map不存在，则创建聚合Map
        AggregateMap aggregateMap = aggregateMaps.get(taskId);
        if (aggregateMap == null && mapAction != MapAction.UPDATE) {
            // 如果push_previous_map_as_event为true，则从聚合map中创建新的event
            if (push_previous_map_as_event && lastTaskId != null && aggregateMaps.get(lastTaskId) != null) {
                eventToYield = extractPreviousMapAsEvent();
            }

            aggregateMap = new AggregateMap(taskId, System.currentTimeMillis());
            aggregateMaps.put(taskId, aggregateMap);
        }

        // 执行code
        Map map = aggregateMap.getMap();
        try {
            event = (Map) codeMethod.invoke(null, event, map);
            LOGGER.debug("Aggregate successful filter code execution, code：{}", code);
        } catch (Exception e) {
            LOGGER.error("Aggregate exception occurred", e);
            EventDecorator.addTag(event, "_aggregateexception"); // 执行失败
        }

        // 如果end_of_task为true，则删除聚合map
        if (end_of_task) {
            aggregateMaps.remove(taskId);
        }

        // 将聚合map添加为event
        if (eventToYield != null) {
            addEvent(eventToYield);
        }

        // 重置lastTaskId
        lastTaskId = taskId;
        return event;
    }

    /**
     * 当JLogstash停止时调用该方法。
     */
    @Override
    public void release() {
        evictThread.stop();
        executor.shutdown();
    }

    /**
     * @param filterThread
     */
    @Override
    public void setFilterThread(FilterThread filterThread) {
        this.filterThread = filterThread;

        nextFilters = new ArrayList<BaseFilter>(filterThread.getFilterProcessors());
        Iterator<BaseFilter> iterator = nextFilters.iterator();

        while (iterator.hasNext()) {
            BaseFilter itBaseFilter = iterator.next();
            if (itBaseFilter != this) {
                iterator.remove();
            } else {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * 初始化Code代码块对应的方法
     *
     * @return
     */
    private Method initCodeMethod() {
        try {
            String template = TemplateUtil.readTemplateFromPath(CodeConstans.CODE_TEMPLATE_NAME);
            FreeMarkerRender codeRender = new FreeMarkerRender(template, CodeConstans.CODE_TEMPLATE_NAME);

            Map<String, Object> event = new HashMap<String, Object>();
            event.put("code", code);
            String sourceCode = codeRender.render(template, event);

            CodeCompiler codeCompiler = new CodeCompiler();
            Class<?> codeClass = codeCompiler.compile(CodeConstans.CODE_CLASS_NAME, sourceCode);
            return codeClass.getMethod(CodeConstans.METHOD_NAME, Map.class, Map.class);
        } catch (Exception e) {
            throw new InitializeException(e);
        }
    }

    /**
     * 初始化timeout代码块对应的方法
     *
     * @return
     */
    private Method initTimeoutCodeMethod() {
        try {
            String template = TemplateUtil.readTemplateFromPath(TimeOutConfigConstans.TIMEOUT_CODE_TEMPLATE_NAME);
            FreeMarkerRender codeRender = new FreeMarkerRender(template, TimeOutConfigConstans.TIMEOUT_CODE_TEMPLATE_NAME);

            Map<String, Object> event = new HashMap<String, Object>();
            event.put("code", timeout_code);
            String sourceCode = codeRender.render(template, event);

            CodeCompiler codeCompiler = new CodeCompiler();
            Class<?> codeClass = codeCompiler.compile(TimeOutConfigConstans.CODE_CLASS_NAME, sourceCode);
            return codeClass.getMethod(TimeOutConfigConstans.METHOD_NAME, Map.class);
        } catch (Exception e) {
            throw new InitializeException(e);
        }
    }

    /**
     * 添加事件
     *
     * @param eventToYield
     */
    private void addEvent(Map eventToYield) {
        try {
            for (BaseFilter baseFilter : nextFilters) {
                if (eventToYield == null) break;
                baseFilter.process(eventToYield);
            }
        } catch (Exception e) {
            LOGGER.error("processEventAfterFilter failed, event:" + eventToYield, e);
        }
        if (eventToYield != null) FilterThread.put(eventToYield);
    }

    /**
     * 创建超时Event
     *
     * @param aggregateMap 聚合Map
     * @param task_id      任务ID
     * @return
     */
    private Map createTimeOutEvent(Map aggregateMap, String task_id) {
        // 添加taskId
        Map event_to_yield = new HashMap(aggregateMap);
        if (StringUtils.isNotEmpty(timeout_task_id_field)) {
            event_to_yield.put(timeout_task_id_field, task_id);
        }

        // 添加timeout_tags
        EventDecorator.addTags(event_to_yield, timeout_tags);

        // 执行timeout code
        try {
            if (timeoutMethod != null) {
                event_to_yield = (Map) timeoutMethod.invoke(null, event_to_yield);
            }
        } catch (Exception e) {
            LOGGER.error("Aggregate exception occurred", e);
            EventDecorator.addTag(event_to_yield, "_aggregateexception"); // 执行失败
        }

        return event_to_yield;
    }

    /**
     * 将上一聚合Map作为新的event返回
     *
     * @return
     */
    private Map extractPreviousMapAsEvent() {
        AggregateMap previousAggregateMap = aggregateMaps.remove(lastTaskId);
        if (previousAggregateMap == null) {
            return null;
        }
        String previousTaskId = previousAggregateMap.getTaskId();
        Map previousMap = previousAggregateMap.getMap();

        return createTimeOutEvent(previousMap, previousTaskId);
    }

    /**
     * JLogstash将每隔５秒调用一次该方法
     */
    private void flush() {
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }

        if (System.currentTimeMillis() > last_flush_timestamp + timeout / 2) {
            List<Map> eventToFlush = removeExpiredMaps();
            for (Map map : eventToFlush) {
                addEvent(map);
            }
            last_flush_timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 移除超时的聚合Map
     *
     * @return 超时时聚合map生成的event
     */
    private List<Map> removeExpiredMaps() {
        List<Map> eventToFlush = new ArrayList<Map>();

        long minTimeStamp = System.currentTimeMillis() - timeout * 1000;
        Collection<AggregateMap> aggregateMapColl = aggregateMaps.values();

        Iterator<AggregateMap> aggregateMapIterator = aggregateMapColl.iterator();
        for (; aggregateMapIterator.hasNext(); ) {
            AggregateMap aggregateMap = aggregateMapIterator.next();
            if (aggregateMap.getCreateTime() < minTimeStamp) {
                if (push_previous_map_as_event || push_map_as_event_on_timeout) {
                    Map eventMap = createTimeOutEvent(aggregateMap.getMap(), aggregateMap.getTaskId());
                    eventToFlush.add(eventMap);
                }
                aggregateMapIterator.remove();
            }
        }

        return eventToFlush;
    }

    /**
     * 驱逐AggregateMap的线程
     */
    class EvictThread implements Runnable {

        /**
         * 检查间隔 5秒
         */
        private Long interval = 5000L;

        private volatile boolean stop = false;

        public EvictThread() {
        }

        public EvictThread(Long interval) {
            this.interval = interval;
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    flush();
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    LOGGER.error("EvictThread interrupted...", e);
                    stop = true;
                }
            }
        }

        public void stop() {
            stop = true;
        }
    }
}
