package com.example.unofrontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

public class UnoApplication extends Application {
    private ApplicationContext context;

    @Override
    public void init() {
        // Initialize Spring context
        context = new AnnotationConfigApplicationContext(UnoSpringBootApplication.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(UnoApplication.class.getResource("/View/login.fxml"));
        // Set the controller factory to use Spring
        fxmlLoader.setControllerFactory(context::getBean);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Uno");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (context instanceof AutoCloseable) {
            try {
                ((AutoCloseable) context).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}