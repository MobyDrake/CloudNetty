package ru.mobydrake.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class ClientMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("CloudBox");
        primaryStage.show();

        //ClientController controller = fxmlLoader.getController();
        //controller.setClientMain(this);
    }

    public ClientMain() {

    }

    public static void main(String[] args) {
        launch(args);
    }
}
