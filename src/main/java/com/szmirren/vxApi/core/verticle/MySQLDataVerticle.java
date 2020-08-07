package com.szmirren.vxApi.core.verticle;

import com.szmirren.vxApi.core.common.StrUtil;
import com.szmirren.vxApi.core.common.VxApiDATASQLConstant;
import com.szmirren.vxApi.core.common.VxApiEventBusAddressConstant;
import com.szmirren.vxApi.core.entity.VxApiTrackInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * mysql 数据库定制实现
 *
 * @author endy0114
 */
public class MySQLDataVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(DATAVerticle.class);

    /**
     * 当前Vertx的唯一标识
     */
    private String thisVertxName;
    /**
     * 数据库操作句柄
     */
    private MySQLPool pool;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        LOG.info("start MYSQL DATA Verticle ...");
        thisVertxName = System.getProperty("thisVertxName", "VX-API");
        // 初始化mysql数据库连接池
        initMySQLPool();

        // application operation address
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.FIND_APP, this::findApplication);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.GET_APP, this::getApplication);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.ADD_APP, this::addApplication);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.DEL_APP, this::delApplication);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.UPDT_APP, this::updtApplication);
        // api operation address
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.FIND_API_ALL, this::findAPI);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.FIND_API_BY_PAGE, this::findAPIByPage);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.GET_API, this::getAPI);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.ADD_API, this::addAPI);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.DEL_API, this::delAPI);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.UPDT_API, this::updtAPI);
        // blacklist
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.FIND_BLACKLIST, this::findBlacklist);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.REPLACE_BLACKLIST, this::replaceBlacklist);
        // track info
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SAVE_REQUEST_ERR, this::saveRequestErr);
        vertx.eventBus().consumer(thisVertxName + VxApiEventBusAddressConstant.SAVE_TRACK_INFO, this::saveTrackInfo);
        LOG.info("start DATA Verticle successful");
        startPromise.complete();

    }

    @Override
    public void stop() throws Exception {
        // 释放数据库连接
        pool.close();
        super.stop();
    }

    private void initMySQLPool() {
        // 从配置里面获取参数
        JsonObject dbConfig = config();
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(dbConfig.getInteger("port", 3306))
                .setHost(dbConfig.getString("host", "localhost"))
                .setDatabase(dbConfig.getString("dataBase", "vx_gateway"))
                .setUser(dbConfig.getString("user", "admin"))
                .setPassword(dbConfig.getString("password", "admin"))
                .setCachePreparedStatements(dbConfig.getBoolean("cachePreparedStatements", true))
                .setIdleTimeout(dbConfig.getInteger("idleTimeout", 30000))
                .setConnectTimeout(dbConfig.getInteger("connectTimeout", 60000))
                .setCharacterEncoding(dbConfig.getString("characterEncoding", "utf-8"));
        // 设置线程池大小
        PoolOptions poolOptions = new PoolOptions().setMaxSize(dbConfig.getInteger("maxSize", 10));
        pool = MySQLPool.pool(vertx, connectOptions, poolOptions);
    }

    /**
     * 存储请求一场信息
     *
     * @param msg
     */
    private void saveRequestErr(Message<JsonObject> msg) {
        if (msg.body() == null) {
            LOG.warn("请求异常信息为空");
        } else {
            LOG.debug("异常信息数据：{}", msg.body());
            JsonObject body = msg.body();
            Tuple params = Tuple.of(body.getString("appName"), body.getString("apiName"),
                    body.getString("errMsg"), body.getString("errStackTrace"));
            pool.preparedQuery(VxApiDATASQLConstant.SAVE_REQUEST_ERR).execute(params, res -> {
                if (res.failed()) {
                    LOG.error("API请求异常信息存储失败！{}", res.cause().getMessage(), res.cause());
                }
            });
        }
    }

    /**
     * 存储API请求信息
     *
     * @param msg
     */
    private void saveTrackInfo(Message<JsonObject> msg) {
        if (msg.body() == null) {
            LOG.warn("请求异常信息为空");
        } else {
            LOG.debug("API请求信息数据：{}", msg.body());
            VxApiTrackInfo trackInfo = VxApiTrackInfo.fromJson(msg.body());
            Tuple params = Tuple.of(trackInfo.getAppName(), trackInfo.getApiName(),
                    trackInfo.getStartTime().toEpochMilli(),
                    trackInfo.getEndTime().toEpochMilli(),
                    trackInfo.getRequestTime().toEpochMilli(),
                    trackInfo.getResponseTime().toEpochMilli(),
                    trackInfo.getRequestBufferLen(), trackInfo.getResponseBufferLen(),
                    trackInfo.isSuccessful(),
                    trackInfo.getErrMsg(), trackInfo.getErrStackTrace(), trackInfo.getRemoteIp(), trackInfo.getBackServiceUrl());
            pool.preparedQuery(VxApiDATASQLConstant.SAVE_TRACK_INFO).execute(params, res -> {
                if (res.failed()) {
                    LOG.error("API请求信息存储失败！{}", res.cause().getMessage(), res.cause());
                }
            });
        }
    }

    /**
     * 更新和名单列表
     *
     * @param msg
     */
    private void replaceBlacklist(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(412, "the Blacklist IP is null");
        } else {
            JsonObject body = msg.body();
            Tuple params = Tuple.of(body.getJsonArray("blacklistBody").toString(), body.getString("blacklistName"));
            pool.preparedQuery(VxApiDATASQLConstant.UPDATE_BLACKLIST).execute(params, res -> {
                if (res.succeeded()) {
                    msg.reply(1);
                } else {
                    msg.fail(500, res.cause().toString());
                    LOG.error("执行添加应用程序-->失败:" + res.cause().toString());
                }
            });

        }
    }

    /**
     * 查询和名单列表
     *
     * @param msg
     */
    private void findBlacklist(Message<JsonObject> msg) {
        pool.query(VxApiDATASQLConstant.FIND_BLACKLIST).execute(res -> {
            if (res.succeeded()) {
                JsonObject result = new JsonObject();
                RowSet<Row> rows = res.result();
                if (rows != null && rows.size() > 0) {
                    Row row = rows.iterator().next();
                    result.put("content", new JsonArray(row.getString("content")));
                }
                msg.reply(result);
            } else {
                msg.fail(500, res.cause().toString());
                LOG.error("执行查询应用程序-->失败:" + res.cause().toString());
            }
        });
    }

    /**
     * 更新API接口信息
     *
     * @param msg
     */
    private void updtAPI(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the application name is null");
        } else {
            JsonObject body = msg.body();
            pool.preparedQuery(VxApiDATASQLConstant.UPDATE_API).execute(Tuple.of(body.getJsonObject("api"), body.getString("apiName")), res -> {
                if (res.succeeded()) {
                    msg.reply(1);
                } else {
                    msg.fail(500, res.cause().toString());
                    LOG.error("执行更新API-->失败:" + res.cause().toString());
                }
            });
        }
    }

    /**
     * 删除API
     *
     * @param msg
     */
    private void delAPI(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the msg body is null");
        } else {
            String appName = msg.body().getString("appName");
            String apiName = msg.body().getString("apiName");
            if (StrUtil.isNullOrEmpty(appName, apiName)) {
                msg.fail(411, "the appName or apiName is null");
            } else {
                pool.preparedQuery(VxApiDATASQLConstant.DEL_API_BY_NAME).execute(Tuple.of(appName, apiName), res -> {
                    if (res.succeeded()) {
                        msg.reply(1);
                    } else {
                        msg.fail(500, res.cause().toString());
                        LOG.error("执行删除API-->失败:" + res.cause().toString());
                    }
                });
            }
        }
    }

    /**
     * 添加API
     *
     * @param msg
     */
    private void addAPI(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the application options is null");
        } else {
            JsonObject body = msg.body();
            Tuple params = Tuple.of(body.getString("apiName"), body.getString("appName"), body.getJsonObject("api").toString());
            pool.preparedQuery(VxApiDATASQLConstant.ADD_API).execute(params, res -> {
                if (res.succeeded()) {
                    long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
                    msg.reply(lastInsertId);
                } else {
                    msg.fail(500, res.cause().toString());
                    LOG.error("执行添加API-->失败:" + res.cause().toString());
                }
            });
        }
    }

    /**
     * 根据名称查询API信息
     *
     * @param msg
     */
    private void getAPI(Message<String> msg) {
        String name = msg.body();
        if (name == null) {
            msg.fail(411, "the application name is null");
        } else {
            pool.preparedQuery(VxApiDATASQLConstant.FIND_API_BY_NAME).execute(Tuple.of(name), res -> {
                RowSet<Row> rows = res.result();
                JsonObject result = new JsonObject();
                if (rows != null && rows.size() > 0) {
                    Row row = rows.iterator().next();
                    result.put("name", row.getString("name"))
                            .put("appName", row.getString("appName"))
                            .put("content", row.getString("content"));
                }
                msg.reply(result);
            });
        }
    }

    /**
     * 分页查询API信息
     *
     * @param msg
     */
    private void findAPIByPage(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the application name is null");
        } else {
            JsonObject body = msg.body();
            pool.preparedQuery(VxApiDATASQLConstant.COUNT_API).execute(Tuple.of(body.getString("appName")), count -> {
                if (count.succeeded()) {
                    int dataCount = count.result().iterator().next().getInteger(0);
                    Tuple param = Tuple.of(body.getString("appName"), body.getInteger("offset"), body.getInteger("limit"));
                    pool.preparedQuery(VxApiDATASQLConstant.FINS_API_PAGE).execute(param, res -> {
                        if (res.succeeded()) {
                            RowSet<Row> rows = res.result();
                            JsonArray array = new JsonArray();
                            rows.forEach(row -> array.add(new JsonObject(row.getString("content"))));
                            msg.reply(new JsonObject().put("dataCount", dataCount).put("data", array));
                        } else {
                            msg.fail(500, res.cause().toString());
                            LOG.error("执行查看所有API-->失败:" + res.cause().toString());
                        }
                    });
                } else {
                    msg.fail(500, count.cause().toString());
                    LOG.error("执行查看所有API-->失败:" + count.cause().toString());
                }
            });
        }
    }

    /**
     * 查询应用下的API列表信息
     *
     * @param msg
     */
    private void findAPI(Message<JsonObject> msg) {
        if (msg.body() == null || msg.body().getString("appName") == null) {
            msg.fail(411, "the application name is null");
        } else {
            pool.preparedQuery(VxApiDATASQLConstant.FIND_API_LIST).execute(Tuple.of(msg.body().getString("appName")), res -> {
                if (res.succeeded()) {
                    RowSet<Row> rows = res.result();
                    JsonArray array = new JsonArray();
                    rows.forEach(row -> array.add(new JsonObject(row.getString("content"))));
                    msg.reply(array);
                } else {
                    msg.fail(500, res.cause().toString());
                    LOG.error("执行查看所有API-->失败:" + res.cause().toString());
                }
            });
        }
    }

    /**
     * 更新应用信息
     *
     * @param msg
     */
    private void updtApplication(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the application name is null");
        } else {
            JsonObject body = msg.body();
            pool.preparedQuery(VxApiDATASQLConstant.UPDATE_APPLICATION)
                    .execute(Tuple.of(body.getJsonObject("app"), body.getString("appName")), res -> {
                        if (res.succeeded()) {
                            msg.reply(1);
                        } else {
                            msg.fail(500, res.cause().toString());
                            LOG.error("执行更新应用程序-->失败:" + res.cause().toString());
                        }
                    });
        }
    }

    /**
     * 删除应用信息
     *
     * @param msg
     */
    private void delApplication(Message<String> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the application name is null");
        } else {
            pool.preparedQuery(VxApiDATASQLConstant.DEL_APPLICATION)
                    .execute(Tuple.of(msg.body()), res -> {
                        if (res.succeeded()) {
                            pool.preparedQuery(VxApiDATASQLConstant.DEL_API)
                                    .execute(Tuple.of(msg.body()), ires -> {
                                        if (ires.succeeded()) {
                                            msg.reply(ires.result().size());
                                        } else {
                                            msg.fail(500, res.cause().toString());
                                            LOG.error("执行删除应用程序-->失败:" + res.cause().toString());
                                        }
                                    });
                        } else {
                            msg.fail(500, res.cause().toString());
                            LOG.error("执行删除应用程序-->失败:" + res.cause().toString());
                        }
                    });
        }
    }

    /**
     * 添加应用信息
     *
     * @param msg
     */
    private void addApplication(Message<JsonObject> msg) {
        if (msg.body() == null) {
            msg.fail(411, "the application options is null");
        } else {
            JsonObject param = msg.body();
            pool.preparedQuery(VxApiDATASQLConstant.ADD_APPLICATION)
                    .execute(Tuple.of(param.getString("appName"), param.getJsonObject("app")), res -> {
                        if (res.succeeded()) {
                            long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
                            msg.reply(lastInsertId);
                        } else {
                            msg.fail(500, res.cause().toString());
                            LOG.error("执行添加应用程序-->失败:" + res.cause().toString());
                        }
                    });
        }
    }

    /**
     * 根据应用app的名字查询应用信息
     *
     * @param msg
     */
    private void getApplication(Message<String> msg) {
        String name = msg.body();
        if (name == null) {
            msg.fail(411, "the application name is null");
        } else {
            pool.preparedQuery(VxApiDATASQLConstant.FIND_APPLICATION_BY_NAME)
                    .mapping(row -> new JsonObject().put("name", row.getString("name"))
                            .put("content", row.getString("content")))
                    .execute(Tuple.of(name), res -> {
                        if (res.succeeded()) {
                            RowSet<JsonObject> rows = res.result();
                            if (rows != null && rows.size() > 0) {
                                msg.reply(rows.iterator().next());
                            }
                        } else {
                            msg.fail(500, res.cause().toString());
                            LOG.error("执行查询应用程序-->失败:" + res.cause().toString());
                        }
                    });
        }
    }

    /**
     * 查询应用信息
     *
     * @param msg
     */
    private void findApplication(Message<String> msg) {
        pool.query(VxApiDATASQLConstant.FIND_APPLICATION_LIST).execute(res -> {
            if (res.succeeded()) {
                RowSet<Row> rows = res.result();
                if (rows != null && rows.size() > 0) {
                    JsonArray array = new JsonArray();
                    rows.forEach(row -> array.add(new JsonObject(row.getString("content"))));
                    msg.reply(array);
                } else {
                    msg.reply(new JsonArray());
                }
            } else {
                msg.fail(500, res.cause().toString());
                LOG.error("执行查询所有应用程序-->失败:" + res.cause().toString());
            }
        });

    }
}
