package com.iron;

import com.iron.Services.GitHubService;
import com.iron.Records.GitHubResult;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Hello world!
 *
 */
public class App extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(
        App.class.getResource("views/Prueba.fxml"));

    Scene scene = new Scene(loader.load());

    stage.setScene(scene);
    stage.setTitle("Github Manager");
    stage.show();
  }

  public static void main(String[] args) {
    GitHubService service = new GitHubService();
    launch(args);
  }
}
