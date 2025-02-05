package es.crttn.dad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    RootController rc;

    @Override
    public void start(Stage primaryStage) throws Exception {

        rc = new RootController();

        primaryStage.setTitle("Mail Server");
        primaryStage.setScene(new Scene(rc.getRoot(), 800, 600));
        primaryStage.show();
    }
}
