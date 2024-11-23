package io.github.lumijiez;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(8083);
        JavalinConfig.setup(app);


        System.out.print("Discovery service up and running");
    }
}