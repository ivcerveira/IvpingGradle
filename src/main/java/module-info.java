module com.example.ivpinggradle {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.ooxml;
    requires java.desktop;


    opens com.example.ivpinggradle to javafx.fxml;
    exports com.example.ivpinggradle;
}
