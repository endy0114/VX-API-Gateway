package com.szmirren.vxApi.core.handler.route.impl;

import com.szmirren.vxApi.core.entity.VxApi;
import com.szmirren.vxApi.core.handler.route.VxApiRouteHandlerRedirectService;
import com.szmirren.vxApi.core.options.VxApiServerEntranceRedirectOptions;
import com.szmirren.vxApi.spi.handler.VxApiAfterHandler;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * VxApiRoute页面跳转处理器实现类
 *
 * @author <a href="http://szmirren.com">Mirren</a>
 */
public class VxApiRouteHandlerRedirectServiceImpl implements VxApiRouteHandlerRedirectService {

    private boolean isNext;
    private VxApiServerEntranceRedirectOptions redirectOptions;

    public VxApiRouteHandlerRedirectServiceImpl(boolean isNext, VxApi api) {
        super();
        this.isNext = isNext;
        JsonObject body = api.getServerEntrance().getBody();
        redirectOptions = VxApiServerEntranceRedirectOptions.fromJson(body);
        if (redirectOptions == null) {
            throw new NullPointerException("页面跳转服务类型的配置文件无法装换为服务类");
        }
    }

    @Override
    public void handle(RoutingContext rct) {
        rct.response().putHeader("Location", redirectOptions.getUrl()).setStatusCode(302);
        if (isNext) {
			// 告诉后置处理器当前操作成功执行
            rct.put(VxApiAfterHandler.PREV_IS_SUCCESS_KEY, Future.<Boolean>succeededFuture(true));
            rct.next();
        } else {
            if (!rct.response().ended()) {
                rct.response().end();
            }
        }

    }

}
