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

create table vx_api_track(
 id bigint primary key auto_increment,
 appName varchar(60) not null comment '应用名称',
 apiName varchar(60) not null comment '接口名称',
 startTime bigint not null comment '业务开始时间',
 endTime bigint not null comment '业务结束时间',
 requestTime bigint comment '与后台服务交互开始时间',
 responseTime bigint comment '与后台服务交互结束时间',
 requestBufferLen int comment '用户请求的的主体buffer长度',
 responseBufferLen int comment 'responseBufferLen',
 successful bit comment '是否成功',
 errMsg varchar(100) comment '异常信息',
 errStackTrace text comment '一场堆栈消息',
 remoteIp varchar(16) comment '远程请求IP地址',
 backServiceUrl varchar(200) comment '后端服务地址'
) comment 'API服务请求追踪表';

create table vx_api_request_err(
 id bigint primary key auto_increment,
 appName varchar(60) not null comment '应用名称',
 apiName varchar(60) not null comment '接口名称',
 errMsg varchar(100) comment '异常信息',
 errStackTrace text comment '一场堆栈消息',
 createTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) comment '请求错误记录表';
