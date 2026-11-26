package com.iron.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

  private static final String RESOURCE_PATH = "/application.properties";
  private static final String TOKEN_KEY = "github.token";

  private AppConfig() {
  }

  public static String getGithubToken() throws IllegalStateException {
    InputStream in = AppConfig.class.getResourceAsStream(RESOURCE_PATH);
    if (in == null)
      throw new IllegalStateException("No se puede iniciar sin el application.properties");

    try (in) {
      Properties props = new Properties();
      props.load(in);
      String token = props.getProperty(TOKEN_KEY);

      if (token == null || token.isBlank())
        throw new IllegalStateException("No se obtuvo el token de github");

      return token.trim();
    } catch (IOException e) {
      throw new IllegalStateException("Error leyendo configuración", e);
    }
  }
}
