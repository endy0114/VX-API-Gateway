package com.szmirren.vxApi.core.common;

/**
 * 存储VxApiGateway常用的属性
 *
 * @author <a href="http://szmirren.com">Mirren</a>
 */
public interface VxApiGatewayAttribute {
    /**
     * 网关的名字
     */
    String NAME = "VX-API";
    /**
     * 网关的全名
     */
    String FULL_NAME = "VX-API-Gateway";
    /**
     * VxApiGateway版本号
     */
    String VERSION = "1.0.3";
    /**
     * session默认的cookie名字
     */
    String SESSION_COOKIE_NAME = "VX-API.session";
    /**
     * 网关userAgent默认名字
     */
    String VX_API_USER_AGENT = "VX-API-Gateway/" + VERSION;

}
