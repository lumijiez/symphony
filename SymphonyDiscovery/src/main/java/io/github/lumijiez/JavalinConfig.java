package io.github.lumijiez;

import com.google.gson.Gson;
import io.javalin.Javalin;

public class JavalinConfig {
    public static Gson gson = new Gson();

    public static void setup(Javalin app) {
        app.get("/check", ctx -> ctx.result("OK"));
    }
}
