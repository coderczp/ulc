#!/bin/bash

cmd=$1
vm_arg=''
ip=127.0.0.1
java_path=/var/www/data/jdk1.8.0_60/bin/java

echo "import project.config"
source project.config
echo "env:"${env}


function checkParam(){
   if [ ! -f "${java_path}" ];then
       echo "default java not found in ${java_path},you can config in project.config"
       exit
   fi  
   if [[ "${ip}" == "" ]] ;then
      echo "ip not found ${ip}"  
      exit
   fi 
   if [[ "${port}" == "" ]];then
      echo "port not found ${port}"
      exit 
   fi
}

function getInnerIp(){
   ip=`/sbin/ifconfig -a|grep inet|grep -v 127.0.0.1|grep -v inet6|awk '{print $2}'|tr -d "addr:"|egrep "10\.|192|176"`
   url=http://$ip:$port/${shutdown_uri}
}

function doStop(){
   pid=`ps -ef | grep ${main_jar} |awk '{ if(index($0,"java")>0){print $2}}'`
   if [[ "${pid}" == "" ]]; then
      echo 'service is not running,will skip stop command'
   else
       curl $url/stop
       kill -15 ${pid}
       sleep 1
       echo 'kill '${pid}  
       ps -ef |grep ${ctx}
   fi
}

function doStart(){
   if [ ! -f "$myFile" ]; then
      touch lock
   else
      echo "find lock,maybe anthor user is starting"
      exit    
   fi   
   eval $nohup_start
   echo "service start listen at :$ip:$port"
   sleep 3
   tail -n100 start.log
   rm lock
}

function backUp(){
   ts=`date +%Y-%m-%d_%H:%M:%S`
   cmd="cp ${tar_file} ${tar_file}.bak.${ts}"
   eval ${cmd}
   echo $cmd
}

function untar(){
  echo "tar ${tar_file}"
  tar -xvf ${tar_file}
  echo "copy conf/${env}/* to res"
  cp -r conf/${env}/*  res/
}

function main(){
  start_cmd="$java_path ${vm_arg} -jar ${main_jar} --spring.config.location=res/application.properties --server.port=${port} --server.address=${ip}"
  nohup_start="nohup "$start_cmd" >start.log 2>&1 &"
  echo ${nohup_start}
  
  if [[ $cmd == "start" ]]; then
      doStart
  elif [[ $cmd == "stop" ]]; then
      doStop
  elif [[ $cmd == "restart" ]]; then
      doStop
      doStart
  elif [[ $cmd == "all" ]]; then
      backUp
      doStop
      untar 
      doStart  
  else
      echo "usage: stop|start|restart|all"
  fi
}

getInnerIp;
checkParam;
main;