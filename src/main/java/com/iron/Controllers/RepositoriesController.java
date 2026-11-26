package com.iron.Controllers;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.iron.Records.GitHubResult;
import com.iron.Records.Repo;
import com.iron.Services.GitHubService;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class RepositoriesController {

  @FXML
  private Label reposSubtitle;
  @FXML
  private Label repoPublic;
  @FXML
  private Label repoPrivate;
  @FXML
  private Label repoStars;
  @FXML
  private Label repoForks;
  @FXML
  private TextField searchField;
  @FXML
  private HBox filterChipsBox;
  @FXML
  private Label repoCount;
  @FXML
  private VBox repoList;
  @FXML
  private Label languageCount;
  @FXML
  private VBox languagesBox;
  @FXML
  private VBox pinnedBox;

  private final GitHubService service = new GitHubService();
  private List<Repo> allRepos = List.of();
  private String activeFilter = "All";

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
      .ofPattern("MMM dd, yyyy")
      .withZone(ZoneId.systemDefault());

  @FXML
  public void initialize() {
    searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    reposSubtitle.setText("Syncing with GitHub…");

    Thread.ofVirtual().start(() -> {
      var result = service.fetchAllRepos();
      Platform.runLater(() -> {
        switch (result) {
          case GitHubResult.Success(var repos) -> {
            allRepos = repos;
            reposSubtitle.setText("Showing " + repos.size() + " repositories");
            updateStats(repos);
            renderLanguages(repos);
            applyFilters();
          }
          case GitHubResult.NotModified() ->
            reposSubtitle.setText("Up to date (" + allRepos.size() + " repositories)");
          case GitHubResult.RateLimited(var resetAt) ->
            reposSubtitle.setText("Rate limit exceeded. Resets at " + DATE_FMT.format(resetAt));
          case GitHubResult.Error(var code, var msg) ->
            reposSubtitle.setText("Error (" + code + "): " + msg);
        }

      });
    });
  }

  @FXML
  private void handleFilter(ActionEvent event) {
    if (event.getSource() instanceof Button clicked) {
      activeFilter = clicked.getText();
      filterChipsBox.getChildren().forEach(node -> {
        if (node instanceof Button btn) {
          btn.getStyleClass().remove("filter-chip-active");
          if (btn == clicked) {
            btn.getStyleClass().add("filter-chip-active");
          }
        }
      });
      applyFilters();
    }
  } //

  private void applyFilters() {
    String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

    List<Repo> filtered = allRepos.stream()
        .filter(repo -> switch (activeFilter) {
          case "Public" -> !repo.isPrivate();
          case "Private" -> repo.isPrivate();
          case "Archived" -> repo.archived();
          case "Mirrors" -> repo.fork();
          default -> true;
        })
        .filter(repo -> query.isEmpty()
            || repo.name().toLowerCase().contains(query)
            || (repo.description() != null && repo.description().toLowerCase().contains(query)))
        .toList();

    repoCount.setText(filtered.size() + " repositories");
    renderRepoList(filtered);
  }

  private void updateStats(List<Repo> repos) {
    long publicCount = repos.stream().filter(r -> !r.isPrivate()).count();
    long privateCount = repos.size() - publicCount;
    int stars = repos.stream().mapToInt(Repo::stargazersCount).sum();
    int forks = repos.stream().mapToInt(Repo::forksCount).sum();

    repoPublic.setText(String.valueOf(publicCount));
    repoPrivate.setText(String.valueOf(privateCount));
    repoStars.setText(String.valueOf(stars));
    repoForks.setText(String.valueOf(forks));
  }

  private void renderRepoList(List<Repo> repos) {
    repoList.getChildren().clear();

    for (Repo repo : repos) {
      VBox card = new VBox(8.0);
      card.getStyleClass().add("repo-card");

      HBox header = new HBox(8.0);
      header.setAlignment(Pos.CENTER_LEFT);

      Label nameLabel = new Label(repo.name());
      nameLabel.getStyleClass().add("repo-name");

      Label visibilityBadge = new Label(repo.isPrivate() ? "Private" : "Public");
      visibilityBadge.getStyleClass().add(repo.isPrivate() ? "badge-warning" : "badge-neutral");

      header.getChildren().addAll(nameLabel, visibilityBadge);

      if (repo.archived()) {
        Label archivedBadge = new Label("Archived");
        archivedBadge.getStyleClass().add("badge-neutral");
        header.getChildren().add(archivedBadge);
      }

      card.getChildren().add(header);

      if (repo.description() != null && !repo.description().isBlank()) {
        Label descLabel = new Label(repo.description());
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("repo-desc");
        card.getChildren().add(descLabel);
      }

      HBox meta = new HBox(14.0);
      meta.setAlignment(Pos.CENTER_LEFT);
      meta.getStyleClass().add("repo-meta");

      if (repo.language() != null && !repo.language().isBlank()) {
        HBox langBox = new HBox(5.0);
        langBox.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4.5);
        dot.getStyleClass().add("lang-dot");
        Label langLabel = new Label(repo.language());
        langBox.getChildren().addAll(dot, langLabel);
        meta.getChildren().add(langBox);
      }

      Label starsLabel = new Label("★ " + repo.stargazersCount());
      Label forksLabel = new Label("⑂ " + repo.forksCount());
      meta.getChildren().addAll(starsLabel, forksLabel);

      if (repo.pushed_at() != null) {
        Label updatedLabel = new Label("Updated " + DATE_FMT.format(repo.pushed_at()));
        meta.getChildren().add(updatedLabel);
      }

      card.getChildren().add(meta);
      repoList.getChildren().add(card);
    }
  }

  private void renderLanguages(List<Repo> repos) {
    languagesBox.getChildren().clear();

    Map<String, Long> counts = repos.stream()
        .filter(r -> r.language() != null && !r.language().isBlank())
        .collect(Collectors.groupingBy(Repo::language, Collectors.counting()));

    languageCount.setText(String.valueOf(counts.size()));
    long totalWithLang = counts.values().stream().mapToLong(Long::longValue).sum();
    if (totalWithLang == 0)
      return;

    counts.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
        .limit(6)
        .forEach(entry -> {
          VBox row = new VBox(4.0);
          HBox top = new HBox();
          top.setAlignment(Pos.CENTER_LEFT);

          Label langName = new Label(entry.getKey());
          langName.getStyleClass().add("lang-name");

          Region spacer = new Region();
          HBox.setHgrow(spacer, Priority.ALWAYS);

          double pct = (entry.getValue() * 100.0) / totalWithLang;
          Label pctLabel = new Label(String.format("%.1f%%", pct));
          pctLabel.getStyleClass().add("lang-pct");

          top.getChildren().addAll(langName, spacer, pctLabel);

          ProgressBar bar = new ProgressBar(pct / 100.0);
          bar.setMaxWidth(Double.MAX_VALUE);
          bar.getStyleClass().add("lang-bar");

          row.getChildren().addAll(top, bar);
          languagesBox.getChildren().add(row);
        });
  }
}
