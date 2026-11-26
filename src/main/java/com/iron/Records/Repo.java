package com.iron.Records;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Repo(
    long id,
    String name,
    @JsonProperty("full_name") String fullName,
    String description,
    @JsonProperty("private") boolean isPrivate,
    boolean archived,
    boolean fork,
    @JsonProperty("stargazers_count") int stargazersCount,
    @JsonProperty("forks_count") int forksCount,
    Instant created_at,
    Instant updated_at,
    Instant pushed_at,
    String language,
    String html_url,
    String clone_url,
    String ssh_url,
    String default_branch,
    Owner owner) {
}
