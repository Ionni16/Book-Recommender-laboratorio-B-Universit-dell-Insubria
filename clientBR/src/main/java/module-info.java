module bookrecommender.clientBR {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires bookrecommender.common;

    opens bookrecommender to javafx.fxml, javafx.graphics;
    opens bookrecommender.ui to javafx.fxml, javafx.graphics;
    opens bookrecommender.repo to javafx.fxml, javafx.graphics;
    opens bookrecommender.service to javafx.fxml, javafx.graphics;

    exports bookrecommender;
    exports bookrecommender.ui;
    exports bookrecommender.repo;
    exports bookrecommender.service;
}