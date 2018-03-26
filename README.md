# log-components
Components for write log , using aop to intercept beans, analysis log

# boot2docker安装配置问题
1.Virtualbox 启动 unable to load R3 module
http://blog.163.com/wilicedon_lee/blog/static/81588483201543010474331/
2.配置boot2docker共享目录
http://blog.csdn.net/jam_lee/article/details/40947429
3.修改镜像源
sudo su echo “EXTRA_ARGS=\”–registry-mirror=https://registry.docker-cn.com\”” >> /var/lib/boot2docker/profile
exit
重启boot2docker
https://www.csdn.net/article/2014-12-15/2823143-Docker-image
4.配置vm.max_map_count=262144
sudo vi /etc/sysctl.conf
添加vm.max_map_count=262144
使生效sudo sysctl -p /etc/sysctl.conf


# Docker cmd
docker pull elasticsearch
docker pull mobz/elasticsearch-head:5
docker pull kibana
docker pull wurstmeister/zookeeper 
docker pull wurstmeister/kafka
docker pull sheepkiller/kafka-manager
docker pull grafana/grafana

sudo sysctl -w vm.max_map_count=262144
docker run -d --name zipkin -p 9411:9411 openzipkin/zipkin
docker run --env STORAGE_TYPE=elasticsearch --env ES_HOSTS=192.168.59.103 -e JAVA_OPTS="-Xms512m -Xms512m" openzipkin/zipkin-dependencies
docker run -d --name es -p 9200:9200 -p 9300:9300 -v /c/Users/es/es.yml:/usr/share/elasticsearch/config/elasticsearch.yml elasticsearch
docker run -d --name es-head -p 9100:9100 mobz/elasticsearch-head:5
docker run -d --name kibana -p 5601:5601 --link es:elasticsearch -e ELASTICSEARCH_URL=http://192.168.59.103:9200 kibana 
docker run --name zk -d -p 2181:2181 -t wurstmeister/zookeeper 
docker run --name kafka -d -e KAFKA_ADVERTISED_HOST_NAME=192.168.59.103 -e KAFKA_ADVERTISED_PORT=9092 -e KAFKA_BROKER_ID=1 -e ZK=zk -e KAFKA_HEAP_OPTS="-Xms256m -Xms256m" -p 9092:9092 --link zk:zk -t wurstmeister/kafka
docker run -d --name kf-m -p 9000:9000 -e ZK_HOSTS="192.168.59.103:2181" sheepkiller/kafka-manager
docker run -d --name=grafana -p 3000:3000 -e "GF_INSTALL_PLUGINS=grafana-clock-panel,grafana-piechart-panel" grafana/grafana


# elasticsearch.yml如下：
#集群名称 所有节点要相同
cluster.name: "mangues_es"
#本节点名称
node.name: master
#作为master节点
node.master: true
#是否存储数据
node.data: true
# head插件设置
http.cors.enabled: true
http.cors.allow-origin: "*"
#设置可以访问的ip 这里全部设置通过
network.bind_host: 0.0.0.0
#设置节点 访问的地址 设置master所在机器的ip
network.publish_host: 192.168.59.103

