<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Add:](#add)
- [DateISO8601:](#dateiso8601)
- [Remove:](#remove)
- [Rename:](#rename)
- [IpIp:](#ipip)
- [UA:](#ua)
- [JGrok:](#jgrok)
- [Json:](#json)
- [Java:](#java)
- [Translate:](#translate)
	- [概要](#概要)
	- [详细参数说明](#详细参数说明)
- [Aggregate:](#aggregate)
	- [概要](#概要)
	- [详细参数说明](#详细参数说明)
	- [完整示例](#完整示例)

<!-- /TOC -->

# Add:

   fields: 必填 map结构({"host":"hostname","ip":"%{ip}%"})

   hostname: 本生是event里的属性，则就会取event.hostname里的值,没有就是原声的字符串hostname,

   %{ip}% 这样就表示调用内置函数获取本机ip

   现在的内置函数有hostname，timestamp，ip

# DateISO8601:

   match: 必填 map结构（{"timestamp":{"srcFormat":"dd/MMM/yyyy:HH:mm:ss Z","target":"timestamp","locale":"en"}}）

# Remove:

  fields:必填 list结构

  removeNULL:默认值false ，是否删除null或空字符串字段

# Rename:

  fields:必填 map结构{"oldname":"newname"}

# IpIp:

  source: 默认值 clientip 需要解析的ip

  target: 默认值 ipip

  size: 默认值 5000

# UA:

  source:必填 需要解析属性

# JGrok:

  srcs:必填 list 结构，需要grok解析的属性["e","b"]

  patterns:必填 map结构，需要的正则表达式，{"pattern":"[0-9A-B]"}

  如果:grok自带的已经有了,正则表达式不需要写，列如:{"%{COMBINEDAPACHELOG}":""}

# Json:

  fields: 必填 map 结构 example {"messgae":"messgae1"} 源属性是message  目标属性message1，没有目标属性可以为“”

# Java:
  code: 必填，String类型 。

  示例：
  ```
  filters:
     - Java:
         code: '
             Object name = event.get("NAME");
             event.put("XM", name);
         '
  ```

# Translate:
使用指定的字典，对event中的指定字段值进行翻译。目前定义字典支持配置文件中定义、本地文件定义、数据库表定义。

## 概要
| Setting|     Input Type|  Required| Default Value|
| :-------- | --------:| :------: | ----- |
| source| string  | Yes|  |
|target | string| No||
|dicType|String|Yes|可选值："jdbc","file","inline"|
|dictionaryPath |string|No||
|dictionary|Map|No||
|refreshInterval|int|No||
|nextLoadTime|long|No|||

## 详细参数说明

`source`:
* 类型：string
* 默认值：无
* 说明：指定作为字典Key的字段。

`target`:
* 类型：string
* 默认值：未指定时，使用`source`所指定的字段。
* 说明：用于指定将翻译后的值输出到哪个字段，未指定时则输出到`source`所指定的字段。

`dicType`:
* 类型：string
* 默认值："file"
* 可选值："file","inline","jdbc"
* 说明：指定字典的类型。"file"表示从文件（`yaml`格式）中加载字典，"inline"表示使用`dictionary`字段配置的键值对作为字典，"jdbc"表示从数据库表中加载字典。
* 配置示例：

file类型
```yaml
filters:
    - Translate:
        source: "code"
        dicType: "file"
        dictionaryPath: "/opt/test.yaml"
```

inline类型
```yaml
filters:
    - Translate:
        source: "code"
        dicType: "inline"
        dictionary: {"001": "医疗", "002": "交通"}
```

jdbc类型
```yaml
filters:
    - Translate:
        source: "code"
        dicType: "jdbc"
        jdbc_connection_string: "jdbc:dm://127.0.0.1:5236"
        jdbc_driver_class: "dm.jdbc.driver.DmDriver"
        jdbc_driver_library: "/opt/Dm7JdbcDriver16.jar"
        jdbc_user: "TEST"
        jdbc_password: "11223344"
        statement: "select code, value from dic"
```

`dictionary`:
* 类型：map
* 默认值：无
* 说明：当`dicType`配置为`inline`时，必要要配置`dictionary`字段。

`jdbc_connection_string`:
* 类型：string
* 默认值：无
* 说明：jdbc连接字符串

`jdbc_driver_class`:
* 类型：string
* 默认值：无
* 说明：jdbc驱动类名称

`jdbc_driver_library`:
* 类型：string
* 默认值：无
* 说明：jdbc驱动jar包的完全路径

`jdbc_user`:
* 类型：string
* 默认值：无
* 说明：jdbc用户名

`jdbc_password`:
* 类型：string
* 默认值：无
* 说明：jdbc用户密码

`statement`:
* 类型：string
* 默认值：无
* 说明：查询语句


# Aggregate:
Aggregate插件用于将输入的多个event聚合为一个event。

## 概要
| Setting|     Input Type|  Required| Default Value|
| :-------- | --------:| :------: | ----- |
| task_id| string  | Yes|  |
|code | string| Yes||
|map_action|String|No|默认值：create_or_update 可选值："create","update","create_or_update"|
|end_of_task |boolean|No|flase|
|timeout|Integer|No|1800|
|timeout_code|String|No||
|push_map_as_event_on_timeout|boolean|No|false|
|push_previous_map_as_event|boolean|No|false|
|timeout_task_id_field|String|No||
|timeout_tags|List|No||
|evict_interval|Integer|No|5|

## 详细参数说明

`task_id`:
* 类型：string
* 默认值：无
* 说明： 用于定义task ID 的表达式。该值必须唯一的表示一个Task。
* 示例：

```yaml
filters:
    - Aggregate:
        task_id: "%{type}%{my_task_id}"
```

`code`:
* 类型：string
* 默认值：无
* 说明： java代码，在该代码中可以使用event和map两个变量。
* 示例：

```yaml
filters:
   - Aggregate:
         code: '
            Integer duration = event.get("duration");
            if(duration != null){
            Integer mapDuration = map.get("duration");
            mapDuration += duration;
            map.put("duration", mapDuration);
        }
'
```

`map_action`:
* 类型：string
* 默认值："create_or_update"
* 说明： 指定对当前aggregate map的操作。
    * create: 创建map,并且只有当map先前没有创建过时才执行code
    * update: 不创建map,并且只在map已经创建过时才执行code
    * create_or_update: 如果map没有创建则创建，不管创没创建map都会执行code

`end_of_task`:
* 类型：boolean
* 默认值：false
* 说明： 用于告诉当前过滤器，task已经结束。在code执行完成后将删除aggregate map。

`timeout`:
* 类型：Integer
* 默认值：1800
* 说明: aggregate map的超时时间，默认为1800秒。超时后，该task的map将被移除。

`timeout_code`:
* 类型：String
* 默认值：
* 说明: 指定当从aggregate map生成timeout事件后，将执行的code

`push_map_as_event_on_timeout`:
* 类型：boolean
* 默认值： false
* 说明: 当该值为true时，当timeout时，将把已经聚合的map作为一个新的event

`push_previous_map_as_event`:
* 类型：boolean
* 默认值： false
* 说明: 当该值为true时，一旦有新的task_id，则把上一task_id聚合的map作为event

`timeout_task_id_field`:
* 类型：String
* 默认值：
* 说明: 当timeout时，task_id是否写入timeout生成的事件的某一字段。通过该字段可以确定是哪一个task超时了。

`timeout_tags`:
* 类型：List
* 默认值：
* 说明: 指定timeout事件生成时需要添加的tags。

`evict_interval`:
* 类型：Integer
* 默认值：5
* 说明：指定检查aggregate map是否超时的时间间隔，单位为秒。

## 完整示例
```yaml
inputs:
     - Jdbc:
         jdbc_connection_string: "jdbc:dm://127.0.0.1:5236"
         jdbc_driver_class: "dm.jdbc.driver.DmDriver"
         jdbc_driver_library: "/opt/Dm7JdbcDriver16-Dm7JdbcDriver16.jar"
         jdbc_fetch_size: 3000
         jdbc_user: "TEST"
         jdbc_password: "TEST"
         statement: "select ajbh,sarylb,xm,gmsfhm,dhhm from test order by ajbh"
filters:
     - Aggregate:
            task_id: "%{AJBH}"
            code: '
                map.put("AJBH", event.get("AJBH"));

                event.remove("AJBH");

                List<Map> sary = ((List<Map>)map.get("SARY"));
                if(sary == null){
                   sary = new ArrayList<Map>();
                }

                sary.add(event);
                map.put("SARY", sary);

                event = null;
            '
            push_previous_map_as_event: true
            timeout: 1
            evict_interval: 1

outputs:
    - Elasticsearch:
        index: test
        documentId: "%{AJBH}"
        documentType: "test"
        hosts: ["127.0.0.1"]
        cluster: "test"
```
