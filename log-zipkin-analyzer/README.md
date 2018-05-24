# 功能
使用kafka-stream,提供对zipkin span trace 日志的统计分析
# 集成
1.使用spring-data-elasticsearch-3.1.0.M1集成spring-boot与es，可直接使用ElasticsearchRepository
兼容性可参考：http://www.cnblogs.com/guozp/p/8686904.html
# 报警
提供了动态配置报警模板，可根据需要定义条件，对满足条件的报警项触发邮件或短信报警
1.添加或编辑报警项:localhost:8080/alarm/get 
json格式参数：
根据错误报警：
{
  "title": "test-error",
  "description": "rr",
  "enable": true,
  "dataSourceType": "ES",
  "dataSource": "",
  "frequency": 120,
  "throttle": 300,
  "threshold": 3,
  "condition": "{\"term\":{\"type.keyword\":\"ERROR\"}}",
  "actions": [
    {
      "name": "邮件",
      "type": "Email",
      "from": null,
      "to": "XXX@XXXX.com",
      "subject": "test alarm",
      "msg": "test中文"
    }
  ]
}
根据耗时报警:
{
  "title": "test",
  "description": "rr",
  "enable": true,
  "dataSourceType": "ES",
  "dataSource": "",
  "frequency": 60,
  "throttle": 300,
  "threshold": 3,
  "condition": "{\"term\":{\"type.keyword\":\"COST\"}},{ \"range\" : { \"children.duration\" : {\"from\": 3000000}}}",
  "actions": [
    {
      "name": "短信",
      "type": "SMS",
      "from": null,
      "to": "test",
      "subject": "ALarm======",
      "msg": "testet中文"
    },
    {
      "name": "邮件",
      "type": "Email",
      "from": null,
      "to": "XXX@XXXX.com",
      "subject": "ALarm======",
      "msg": "testetestset中文"
    }
  ]
}