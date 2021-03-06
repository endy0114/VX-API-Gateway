package com.szmirren.vxApi.core.handler.route;

import com.szmirren.vxApi.core.entity.VxApi;
import com.szmirren.vxApi.core.handler.route.impl.VxApiRouteHandlerRedirectServiceImpl;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * VxApiRoute页面跳转处理器
 *
 * @author <a href="http://szmirren.com">Mirren</a>
 */
public interface VxApiRouteHandlerRedirectService extends Handler<RoutingContext> {
    /**
     * 得到一个默认的页面跳转处理器实现
     *
	 * @param isNext
     * @param api
     * @return
     */
    static VxApiRouteHandlerRedirectService create(boolean isNext, VxApi api) {
        return new VxApiRouteHandlerRedirectServiceImpl(isNext, api);
    }

    ;
}
