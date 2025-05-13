module com.example.bdsqltester {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.postgresql.jdbc;
    requires com.zaxxer.hikari;
    requires java.sql;

    opens com.example.bdsqltester to javafx.fxml;
    exports com.example.bdsqltester;
    opens com.example.bdsqltester.scenes to javafx.fxml;
    exports com.example.bdsqltester.scenes;
    opens com.example.bdsqltester.dtos to javafx.base;
    exports com.example.bdsqltester.dtos;
    opens com.example.bdsqltester.scenes.admin to javafx.fxml;
}