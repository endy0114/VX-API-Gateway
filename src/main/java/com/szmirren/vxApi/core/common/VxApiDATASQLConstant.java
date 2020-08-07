package com.szmirren.vxApi.core.common;

/**
 * 数据库SQL常量
 */
public interface VxApiDATASQLConstant {

    /**
     * 查询应用列表
     */
    String FIND_APPLICATION_LIST = "select name,content from vx_api_application";
    /**
     * 通过名称查找应用信息
     */
    String FIND_APPLICATION_BY_NAME = "select name,content from vx_api_application where name = ?";
    /**
     * 添加应用信息
     */
    String ADD_APPLICATION = "insert into vx_api_application (name,content) values(?,?)";
    /**
     * 根据应用名称删除应用
     */
    String DEL_APPLICATION = "delete from vx_api_application where name = ?";
    /**
     * 更新应用信息
     */
    String UPDATE_APPLICATION = "update vx_api_application set content= ? where name = ?";

    /**
     * 根据名称删除API
     */
    String DEL_API = "delete from vx_api_apis where name = ?";
    /**
     * 查找应用下面所哟的API信息
     */
    String FIND_API_LIST = "select name,app_name as appName,content from vx_api_apis where app_name = ?";
    /**
     * 统计应用下面的API个数
     */
    String COUNT_API = "select count(name) from vx_api_apis where app_name = ?";
    /**
     * 分页查询应用下面的API个数
     */
    String FINS_API_PAGE = "select name,app_name as appName,content from vx_api_apis where app_name = ? limit ?, ?";
    /**
     * 通过API的名称查找API
     */
    String FIND_API_BY_NAME = "select name,app_name as appName,content from vx_api_apis where name = ?";
    /**
     * 添加API接口信息
     */
    String ADD_API = "insert into vx_api_apis (name,app_name,content) values(?,?,?)";
    /**
     * 通过应用名称和API名称删除API信息
     */
    String DEL_API_BY_NAME = "delete from vx_api_apis where app_name = ? and name = ? ";
    /**
     * 更新API信息
     */
    String UPDATE_API = "update vx_api_apis set content= ? where name = ?";
    /**
     * 查询黑名单列表
     */
    String FIND_BLACKLIST = "select name as blacklist ,content from vx_api_blacklist";
    /**
     * 更新和名单
     */
    String UPDATE_BLACKLIST = "REPLACE INTO vx_api_blacklist (content,name) values (?,?)";

}
