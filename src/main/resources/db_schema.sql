create database vx_gateway;
use vx_gateway;

create table vx_api_apis(
 id int primary key auto_increment,
 name varchar(60) unique comment 'API名称',
 app_name varchar(60) comment 'APP名称',
 content text comment 'API定义，json格式字符串'
);

create table vx_api_application(
 id int primary key auto_increment,
 name varchar(60) unique comment '应用名称',
 content text comment '应用定义，应为一组API的集合，所有API的公共定义'
);

create table vx_api_blackList(
 id int primary key auto_increment,
 name varchar(60) unique comment '黑名单名称',
 content text comment '黑名单IP地址，json格式数据'
);
