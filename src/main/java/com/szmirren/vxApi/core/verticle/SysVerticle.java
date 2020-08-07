package com.szmirren.vxApi.core.verticle;

import com.szmirren.vxApi.core.common.StrUtil;
import com.szmirren.vxApi.core.common.VxApiDATAStoreConstant;
import com.szmirren.vxApi.core.common.VxApiEventBusAddressConstant;
import com.szmirren.vxApi.core.entity.VxApiTrackInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 一些系统参数
 *
 * @author <a href="http://szmirren.com">Mirren</a>
 */
public class SysVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(SysVerticle.class);

    /**
     * VX-API的启动时间
     */
    private LocalDateTime startVxApiTime = LocalDateTime.now();
    /**
     * 线程数量
     */
    private int availableProcessors = 0;
    /**
     * JVM总的内存量
     */
    private long totalMemory = 0;
    /**
     * JVM的空闲内存量
     */
    private long freeMemory = 0;
    /**
     * JVM最大内存量
     */
    private long maxMemory = 0;
    /**
     * 异常次数
     */
    private int errorCount = 0;
    /**
     * 请求到达VX的次数
     */
    private long requestVxApiCount = 0;
    /**
     * 请求到达核心处理器(HTTP/HTTPS)的次数
     */
    private long requestHttpApiCount = 0;
    /**
     * 核心处理器(HTTP/HTTPS)当前正在处理API的数量
     */
    private long currentHttpApiProcessingCount = 0;

    /**
     * 存储API的监控记录信息
     */
    private Map<String, Deque<JsonObject>> trackSucceededMap = new HashMap<>();
    /**
     * 存储API请求失败的数数
     */
    private Map<String, Long> requestFailedCount = new HashMap<>();
    /**
     * 存储API请求的数量
     */
    private Map<String, Long> requestCount = new HashMap<>();
    /**
     * 当前Vertx的唯一标识
     */
    private String thisVertxName;

    @Override
    public void start(Promise<Void> startFuture) throws Exception {
        LOG.info("start System Verticle ... ");
        thisVertxName = System.getProperty("thisVertxName", "VX-API");
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_GET_INFO, this::getSysInfo);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_PLUS_ERROR, this::plusError);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_PLUS_TRACK_INFO, this::plusTrackInfo);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_GET_TRACK_INFO, this::getTrackInfo);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_BLACK_IP_FIND, this::findIpList);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_BLACK_IP_REPLACE, this::replaceIpList);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_PLUS_VX_REQUEST, msg -> requestVxApiCount++);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_PLUS_HTTP_API_REQUEST, msg -> {
            requestHttpApiCount++;
            currentHttpApiProcessingCount++;
        });
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SYSTEM_MINUS_CURRENT_PROCESSING, msg -> {
            if (currentHttpApiProcessingCount > 0) {
                currentHttpApiProcessingCount--;
            }
        });

        LOG.info("start System Verticle successful");
        super.start(startFuture);
    }

    /**
     * 查看系统基本信息
     *
     * @param msg
     */
    public void getSysInfo(Message<JsonObject> msg) {

        Promise<JsonObject> countResult = Promise.promise();
        // 获取在线网关应用与API的数量
        vertx.eventBus().<JsonObject>request(thisVertxName + VxApiEventBusAddressConstant.DEPLOY_APP_COUNT, null, res -> {
            if (res.succeeded()) {
                JsonObject result = res.result().body();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("执行获取在线应用的数量-->结果:" + result);
                }
                countResult.complete(result);
            } else {
                countResult.complete(new JsonObject());
                LOG.error("执行获取在线应用的数量-->失败:", res.cause());
            }
        });

        // 获取黑名单
        countResult.future().compose(res -> {
            Promise<JsonObject> blackResult = Promise.promise();
            vertx.eventBus().<JsonObject>request(thisVertxName + VxApiEventBusAddressConstant.FIND_BLACKLIST, null, blackList -> {
                if (blackList.succeeded()) {
                    JsonObject body = blackList.result().body();
                    if (body.getValue(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME) instanceof JsonArray) {
                        res.put("content", body.getJsonArray(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME));
                    } else if (body.getValue(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME) instanceof String) {
                        res.put("content", new JsonArray(body.getString(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME)));
                    } else {
                        res.put("content", new JsonArray());
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("执行查询运行状态-->结果:" + res);
                    }
                    blackResult.complete(res);
                } else {
                    LOG.error("执行查询运行状态-->结果:" + blackList.cause());
                    blackResult.fail(blackList.cause());
                }

            });
            return blackResult.future();
        }).onSuccess(res -> {
            availableProcessors = Runtime.getRuntime().availableProcessors();
            totalMemory = Runtime.getRuntime().totalMemory();
            freeMemory = Runtime.getRuntime().freeMemory();
            maxMemory = Runtime.getRuntime().maxMemory();
            JsonObject result = new JsonObject();
            result.put("availableProcessors", availableProcessors);
            result.put("totalMemory", totalMemory / (1024 * 1024));
            result.put("freeMemory", freeMemory / (1024 * 1024));
            result.put("maxMemory", maxMemory / (1024 * 1024));
            Duration duration = Duration.between(startVxApiTime, LocalDateTime.now());
            result.put("vxApiRunTime", StrUtil.millisToDateTime(duration.toMillis(), "$dD $hH $mMin $sS"));
            result.put("appCount", res.getInteger("app", 0));
            result.put("apiCount", res.getInteger("api", 0));
            result.put("errorCount", errorCount);
            result.put("requestVxApiCount", requestVxApiCount);
            result.put("requestHttpApiCount", requestHttpApiCount);
            result.put("currentHttpApiProcessingCount", currentHttpApiProcessingCount);
            //黑名单信息
            result.put("content", res.getJsonArray("content"));
            // 返回消息
            msg.reply(result);
        }).onFailure(err -> {
            msg.fail(500, err.getMessage());
        });


    }

    /**
     * 添加异常的数量
     *
     * @param msg
     */
    public void plusError(Message<JsonObject> msg) {
        errorCount += 1;
        if (msg.body() != null) {
            VxApiTrackInfo info = VxApiTrackInfo.fromJson(msg.body());
            LOG.error("应用:{} , API:{} ,在执行的过程中发生了异常:{} ,堆栈信息{}", info.getAppName(), info.getApiName(),
                    info.getErrMsg(), info.getErrStackTrace());
            // 信息入库
            vertx.eventBus().send(thisVertxName + VxApiEventBusAddressConstant.SAVE_REQUEST_ERR, msg.body());
        }
    }

    /**
     * 添加API追踪信息
     *
     * @param msg
     */
    public void plusTrackInfo(Message<JsonObject> msg) {
        if (msg.body() != null) {
            VxApiTrackInfo info = VxApiTrackInfo.fromJson(msg.body());
            LOG.debug("应用:{} , API:{} ,执行结果{}", info.getAppName(), info.getApiName(), info);
            // map的key
            String key = info.getAppName() + "-" + info.getApiName();
            // 记录API相关信息
            if (!info.isSuccessful()) {
                // 记录异常
                errorCount += 1;
                requestFailedCount.putIfAbsent(key, 0L);
                requestFailedCount.put(key, requestFailedCount.get(key) + 1);
                LOG.error("应用:{} , API:{} ,在执行的过程中发生了异常:{} ,堆栈信息{}", info.getAppName(), info.getApiName(), info.getErrMsg(), info.getErrStackTrace());
            } else {
                JsonObject json = new JsonObject();
                Duration proc = Duration.between(info.getStartTime(), info.getEndTime());
                json.put("time", info.getStartTime());
                json.put("overallTime", proc.toMillis());
                Duration reqs = Duration.between(info.getRequestTime(), info.getResponseTime());
                json.put("requestTime", reqs.toMillis());
                json.put("requestBodyLen", info.getRequestBufferLen());
                json.put("responseBodyLen", info.getResponseBufferLen());
                if (trackSucceededMap.get(key) == null) {
                    trackSucceededMap.put(key, new LinkedList<>());
                } else {
                    if (trackSucceededMap.get(key).size() > 100) {
                        trackSucceededMap.get(key).pollFirst();
                    }
                }
                trackSucceededMap.get(key).add(json);
            }
            // 添加请求数量统计
            requestCount.putIfAbsent(key, 0L);
            requestCount.put(key, requestCount.get(key) + 1);
            // 信息入库
            vertx.eventBus().send(thisVertxName + VxApiEventBusAddressConstant.SAVE_TRACK_INFO, msg.body());
        }
    }

    /**
     * 查看API运行信息
     *
     * @param msg
     */
    public void getTrackInfo(Message<JsonObject> msg) {
        String appName = msg.body().getString("appName");
        String apiName = msg.body().getString("apiName");
        String key = appName + "-" + apiName;
        JsonObject result = new JsonObject();
        result.put("rc", requestCount.get(key) == null ? 0L : requestCount.get(key));
        result.put("ec", requestFailedCount.get(key) == null ? 0L : requestFailedCount.get(key));
        result.put("track", trackSucceededMap.get(key) == null ? new JsonObject() : trackSucceededMap.get(key));
        msg.reply(result);
    }

    /**
     * 查看黑名单ip地址
     *
     * @param msg
     */
    public void findIpList(Message<JsonObject> msg) {
        vertx.eventBus().<JsonObject>request(thisVertxName + VxApiEventBusAddressConstant.FIND_BLACKLIST, null, res -> {
            if (res.succeeded()) {
                JsonObject body = res.result().body();
                if (body.getValue(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME) instanceof JsonArray) {
                    msg.reply(body.getJsonArray(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME));
                } else if (body.getValue(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME) instanceof String) {
                    msg.reply(new JsonArray(body.getString(VxApiDATAStoreConstant.BLACKLIST_CONTENT_NAME)));
                } else {
                    msg.reply(new JsonArray());
                }
            } else {
                msg.fail(500, res.cause().getMessage());
            }
        });
    }

    /**
     * 更新黑名单ip地址
     *
     * @param msg
     */
    public void replaceIpList(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(1400, "参数为空或者缺少参数");
        } else {
            if (msg.body().getValue("ipList") instanceof JsonArray) {
                JsonArray array = msg.body().getJsonArray("ipList");
                JsonObject body = new JsonObject().put("blacklistName", "blacklist").put("blacklistBody", array);
                vertx.eventBus().<Integer>request(thisVertxName + VxApiEventBusAddressConstant.REPLACE_BLACKLIST, body, res -> {
                    if (res.succeeded()) {
                        msg.reply(res.result().body());
                        // 广播更新自己ip地址
                        vertx.eventBus().publish(VxApiEventBusAddressConstant.SYSTEM_PUBLISH_BLACK_IP_LIST, array);
                    } else {
                        msg.fail(500, res.cause().getMessage());
                    }
                });
            } else {
                msg.fail(1405, "参数为空或者缺少参数");
            }
        }
    }

}
