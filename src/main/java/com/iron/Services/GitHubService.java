package com.iron.Services;

import java.net.http.*;
import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.iron.Config.AppConfig;
import com.iron.Records.GitHubResult;
import com.iron.Records.Repo;

public class GitHubService {

  private static final String BASE_REPOS_URL = "https://api.github.com/user/repos?per_page=100&sort=pushed&affiliation=owner,collaborator,organization_member";
  private static final String API_VERSION = "2022-11-28";

  private final String token = AppConfig.getGithubToken();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NORMAL).build();
  private final ObjectMapper mapper;
  private final Semaphore semaphore;

  private volatile String lastReposEtag;

  public GitHubService() {
    this.semaphore = new Semaphore(5);
    this.mapper = new ObjectMapper().findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private String extractNextUrl(HttpHeaders headers) {
    Optional<String> value = headers.firstValue("Link");
    if (!value.isPresent())
      return null;

    String data = value.get();
    String[] parts = data.split(",");

    for (String part : parts) {
      if (part.contains("rel=\"next\"")) {
        int start = part.indexOf('<');
        int end = part.indexOf('>');

        if (start >= 0 && end > start)
          return part.substring(start + 1, end);
      }
    }

    return null;
  }

  public GitHubResult<List<Repo>> fetchAllRepos() {
    try {
      List<Repo> allRepos = new ArrayList<>();
      String nextUrl = BASE_REPOS_URL;
      boolean isFirstPage = true;
      String firstPageEtag = null;

      while (nextUrl != null) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(nextUrl))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", API_VERSION).GET();

        if (isFirstPage && lastReposEtag != null)
          builder.header("If-None-Match", lastReposEtag);
        HttpRequest req = builder.build();
        semaphore.acquire();
        HttpResponse<String> response;

        try {
          response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } finally {
          semaphore.release();
        }

        int status = response.statusCode();
        if (status == 304)
          return new GitHubResult.NotModified<>();

        if (status == 403 || status == 429) {
          Instant out = response.headers().firstValue("X-RateLimit-Reset")
              .map(val -> Instant.ofEpochSecond(Long.parseLong(val))).orElseGet(() -> Instant.now().plusSeconds(60));
          return new GitHubResult.RateLimited<>(out);
        }

        if (status >= 400)
          return new GitHubResult.Error<>(status, response.body());

        if (isFirstPage)
          firstPageEtag = response.headers().firstValue("ETag").orElse(null);

        List<Repo> pageRepos = mapper.readValue(response.body(), new TypeReference<List<Repo>>() {
        });
        allRepos.addAll(pageRepos);
        nextUrl = extractNextUrl(response.headers());
        isFirstPage = false;
      }

      this.lastReposEtag = firstPageEtag;
      return new GitHubResult.Success<>(List.copyOf(allRepos));

    } catch (IOException e) {
      return new GitHubResult.Error<>(-1, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new GitHubResult.Error<>(-1, "Operacion interrumpida");
    }
  }
}
