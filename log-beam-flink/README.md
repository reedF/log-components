## log-beam-flink
集成beam与flink

## kafka
# kafka安全认证
https://blog.csdn.net/yongyuandefengge/article/details/76684793/  


## flink
# 部署安装
https://ci.apache.org/projects/flink/flink-docs-release-1.6/start/flink_on_windows.html  
1.下载，并解压  
2.运行bin/start-cluster.bat  
3.访问：http://localhost:8081/  


## beam
https://beam.apache.org/documentation/runners/flink/  

1.版本对应关系：beam版本需与flink版本兼容：beam-2.8 ===> flink-1.5  
2.运行beam-flink jar的方式：  
内置flink运行：  
java -jar target/log-beam-flink-fat.jar --filesToStage=target/log-beam-flink-fat.jar  
远程flink运行：  
java -jar target/log-beam-flink-fat.jar --flinkMaster=localhost:8081 --filesToStage=target/log-beam-flink-fat.jar  
