module org.example.batallanaval_fpoe {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.example.batallanaval_fpoe to javafx.fxml;
    exports org.example.batallanaval_fpoe;
    exports org.example.batallanaval_fpoe.controller;
    opens org.example.batallanaval_fpoe.controller to javafx.fxml;
}