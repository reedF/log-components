package com.reed.log.model;

import lombok.Data;

/**
 * msg in kafka
 * @author reed
 *{
"databaseName":"xxxx", // 数据库名称，
"tableName":"xxx",
"actionType":"INSERT | UPDATE | DELETE",
"primaryKeyName":"xxx1,xxx2", //
"fields":{
"k1":v1, // k1 指表中某一个字段的名称
"k2":v2
...
},
"updatedFields":"k1,k3,k5..." // 发生了更新的字段名称，当actionType 为 INSERT 或 DELETE 时，为null
"updatedOriginalFields":{ // 发生更新字段，更新前的值，当 actionType 为INSERT 或 DELETE 时，该数据域不存在！
"k1":v1,
"k3":v3,
......
},
"ts":"2018-05-18 16:10:33,2018-05-18 16:11:00" //记录执行时间与消息接收时间
}
 */
@Data
public class KafkaMsg extends BaseObj {

	private String databaseName;

	private String tableName;

	private String actionType;

	private String primaryKeyName;

	private String fields;

	private String updatedFields;

	private String updatedOriginalFields;

	private String ts;

}
