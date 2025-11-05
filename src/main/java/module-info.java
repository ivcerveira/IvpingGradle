module com.example.ivpinggradle {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.ooxml;
    //noinspection Java9RedundantRequiresStatement
    requires java.desktop;
    //noinspection Java9RedundantRequiresStatement
    requires java.logging;


    opens com.example.ivpinggradle to javafx.fxml;
    exports com.example.ivpinggradle;
}
