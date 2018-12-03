## log-beam-flink
集成beam与flink

## kafka
# kafka安全认证
https://blog.csdn.net/yongyuandefengge/article/details/76684793/  


## flink
# 部署安装
flink下载：https://ci.apache.org/projects/flink/flink-docs-release-1.6/start/flink_on_windows.html  
安装参考：https://blog.csdn.net/sxf_123456/article/details/79448945  
1.下载，并解压  
2.运行bin/start-cluster.bat  
3.访问flink控制台：http://localhost:8081/  
flink安全认证配置参见:https://stackoverflow.com/questions/53137481/kafka-jaas-verify-failed-on-flink-cluster  

## beam
https://beam.apache.org/documentation/runners/flink/  

# 版本对应关系：
1.应用程序端：beam版本需与flink版本兼容：beam-2.8 ===> flink-1.5  
2.使用--flinkMaster提交job时：应用程序端flink版本需与flink服务端版本兼容：flink-1.5  ====> flink-1.5.5-bin-scala_2.11.tgz  
注：如不使用--flinkMaster方式提交，而直接使用flink控制台提交job时，无上述兼容问题  

# 运行beam-flink jar的方式：  
1.内置flink运行：  
java -jar target/log-beam-flink-fat.jar --filesToStage=target/log-beam-flink-fat.jar  

2.远程flink运行,此时可在flink控制台内查看job运行情况：  
java -jar target/log-beam-flink-fat.jar --flinkMaster=localhost:8081 --filesToStage=target/log-beam-flink-fat.jar  
注：  
（1）使用远程flink运行时，flink服务端版本需要与beam-flink-runner版本对应使用1.5，如服务端使用1.6将会出现job无法提交运行的情况 ,此时可以使用下面方式3提交job
（2）远程flink连接kafka时，如kafka-server端需安全认证时，需对flink配置jaas,
复制kafka_client_jaas.conf文件到flink/conf/目录下  
修改start-cluster.bat：  
为log_setting_jm与log_setting_tm变量加入如下java启动参数配置：  
-Djava.security.auth.login.config=file:"%FLINK_CONF_DIR%/kafka_client_jaas.conf"  

3.使用flink控制台提交job
登录flink服务端控制台http://localhost:8081/，使用“Submit new Job”，提交可执行jar（fat.jar）  

