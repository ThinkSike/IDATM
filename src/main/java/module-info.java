module com.taskmanager {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.media;
    requires lombok;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.google.gson;
    
    opens com.taskmanager to javafx.fxml;
    exports com.taskmanager;
    exports com.taskmanager.view;
    exports com.taskmanager.model;
    exports com.taskmanager.service;
} 