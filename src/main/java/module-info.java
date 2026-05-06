module com.myapp.halimawburgersystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires javafx.web;
    requires java.net.http;
    requires java.desktop;
    requires com.github.librepdf.openpdf;


    opens com.myapp.halimawburgersystem to javafx.fxml;
    exports com.myapp.halimawburgersystem;
    exports com.myapp.util;
    exports com.myapp.model;
    exports com.myapp.dao;
}