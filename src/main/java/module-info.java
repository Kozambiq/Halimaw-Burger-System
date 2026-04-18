module com.myapp.halimawburgersystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.myapp.halimawburgersystem to javafx.fxml;
    exports com.myapp.halimawburgersystem;
}