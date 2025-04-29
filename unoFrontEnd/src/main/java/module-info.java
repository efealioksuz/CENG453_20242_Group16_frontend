module com.example.unofrontend {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens com.example.unofrontend.controllers to javafx.fxml;
    opens com.example.unofrontend.models to com.fasterxml.jackson.databind;

    exports com.example.unofrontend;
    exports com.example.unofrontend.controllers;
    exports com.example.unofrontend.models;
    exports com.example.unofrontend.services;
}