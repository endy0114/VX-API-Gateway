package com.szmirren.vxApi.core;

import com.szmirren.vxApi.cluster.VxApiClusterManagerFactory;
import com.szmirren.vxApi.core.common.PathUtil;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.UUID;

/**
 * VX-API的启动器
 *
 * @author <a href="http://szmirren.com">Mirren</a>
 */
public class VxApiLauncher extends Launcher {
    private final String CLUSTER_TYPE = "NONE";

    /**
     * 启动VX-API-Gateway
     */
    public static void start() {
        VxApiLauncher.main(new String[]{"run", "com.szmirren.vxApi.core.VxApiMain"});
    }

    /**
     * 停止VX-API-Gateway
     */
    public static void stop() {
        executeCommand("stop", System.getProperty("thisVertxName"));
    }

    public static void main(String[] args) {
        String thisVertxName = UUID.randomUUID().toString() + LocalTime.now().getNano();
        // 设置当前系统Vertx的唯一标识
        System.setProperty("thisVertxName", thisVertxName);
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        System.setProperty("vertx.disableDnsResolver", "true");
        new VxApiLauncher().dispatch(args);
    }

    public static void executeCommand(String cmd, String... args) {
        new VxApiLauncher().execute(cmd, args);
    }


}
