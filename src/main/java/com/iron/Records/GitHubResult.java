package com.iron.Records;

import java.time.Instant;

public sealed interface GitHubResult<T> {

  record Success<T>(T data) implements GitHubResult<T> {
  }

  record NotModified<T>() implements GitHubResult<T> {
  }

  record RateLimited<T>(Instant resetAt) implements GitHubResult<T> {
  }

  record Error<T>(int statusCode, String message) implements GitHubResult<T> {
  }
}
