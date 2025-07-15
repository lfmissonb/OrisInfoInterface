package com.lfmissonb.oris;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class InterfaceManager {
    static OrisInfoInterface plugin;
    private static Logger logger;
    private final HttpServer httpServer;

    public InterfaceManager(OrisInfoInterface plugin) {
        logger = plugin.logger;
        InterfaceManager.plugin = plugin;

        String host = plugin.getConfig().getString("api.host");
        int port = plugin.getConfig().getInt("api.port");

        plugin.logDebug("Creating HttpServer...");
        try {
            assert host != null;
            this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        } catch (IOException e) {
            logger.severe("Cannot create HttpServer.");
            throw new RuntimeException(e);
        }
        plugin.logDebug("HttpServer created.");

        Gson gson = new Gson();

        JsonObject versionJson = new JsonObject();
        versionJson.addProperty("version", plugin.getPluginMeta().getVersion());
        String versionJsonString = gson.toJson(versionJson);

        httpServer.createContext("/", new BaseHandler() {
            @Override
            public void process(HttpExchange exchange) throws IOException {
                sendResponse(exchange, 404, "404 Not Found");
            }
        });

        httpServer.createContext("/version", new BaseHandler() {
            public void process(HttpExchange exchange) throws IOException {
                sendResult(exchange, versionJsonString);
            }
        });

        httpServer.createContext("/info", new BaseHandler() {
            public void process(HttpExchange exchange) throws IOException {
                JsonObject infoJson = new JsonObject();

                double tpsCurrent = -1;
                JsonObject tpsHistory = new JsonObject();
                if (plugin.getConfig().getBoolean("tps.enabled")) {
                    tpsCurrent = plugin.getTPS();
                    if (plugin.getConfig().getBoolean("tps.history.enabled")) {
                        tpsHistory = plugin.historyManager.getTPSRecords();
                    }
                }
                JsonObject tpsJson = new JsonObject();
                tpsJson.addProperty("current", tpsCurrent);
                tpsJson.add("history", tpsHistory);
                infoJson.add("tps", tpsJson);

                double msptCurrent = -1;
                JsonObject msptHistory = new JsonObject();
                if (plugin.getConfig().getBoolean("mspt.enabled")) {
                    msptCurrent = plugin.getMSPT();
                    if (plugin.getConfig().getBoolean("mspt.history.enabled")) {
                        msptHistory = plugin.historyManager.getMSPTRecords();
                    }
                }
                JsonObject msptJson = new JsonObject();
                msptJson.addProperty("current", msptCurrent);
                msptJson.add("history", msptHistory);
                infoJson.add("mspt", msptJson);

                double playerCurrent = -1;
                JsonObject playerHistory = new JsonObject();
                if (plugin.getConfig().getBoolean("player.enabled")) {
                    playerCurrent = plugin.getPlayerNum();
                    if (plugin.getConfig().getBoolean("player.history.enabled")) {
                        playerHistory = plugin.historyManager.getPlayerRecords();
                    }
                }
                JsonObject playerJson = new JsonObject();
                playerJson.addProperty("current", playerCurrent);
                playerJson.add("history", playerHistory);
                infoJson.add("player", playerJson);

                sendResult(exchange, gson.toJson(infoJson));
            }
        });

        this.httpServer.start();
        logger.info(String.format("HttpServer is listening on %s:%d", host, port));
    }

    public void stop() {
        this.httpServer.stop(0);
        logger.info("HttpServer stopped.");
    }

    private abstract static class BaseHandler implements HttpHandler {
        protected void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
            exchange.sendResponseHeaders(code, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
        }

        protected void sendResult(HttpExchange exchange, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        }


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
            String method = exchange.getRequestMethod();
            String uri = exchange.getRequestURI().toString();
            String protocol = exchange.getProtocol();
            String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            process(exchange);
            int statusCode = exchange.getResponseCode();
            if (plugin.getConfig().getBoolean("api.log-requests")) {
                logger.info(String.format("[%s] [%s %s %s %d] User-Agent: %s", ip, method, uri, protocol, statusCode, userAgent));
            }
            exchange.close();
        }

        public abstract void process(HttpExchange exchange) throws IOException;
    }
}

