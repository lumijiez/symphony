package io.github.lumijiez;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        Logger logger = LogManager.getLogger(Main.class);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinGson());
            config.jetty.modifyWebSocketServletFactory(wsFactoryConfig -> {
                wsFactoryConfig.setIdleTimeout(Duration.ZERO);
            });
        }).start(8083);

        JavalinConfig.setup(app);

        logger.info("Discovery service up and running. - OK");
    }
}