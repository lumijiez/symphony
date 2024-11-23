package io.github.lumijiez;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinGson());
        }).start(8083);

        JavalinConfig.setup(app);

        System.out.print("Discovery service up and running");
    }
}