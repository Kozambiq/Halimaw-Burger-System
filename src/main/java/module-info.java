module com.myapp.halimawburgersystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;

    opens com.myapp.halimawburgersystem to javafx.fxml;
    exports com.myapp.halimawburgersystem;
    exports com.myapp.util;
    exports com.myapp.model;
    exports com.myapp.dao;
}