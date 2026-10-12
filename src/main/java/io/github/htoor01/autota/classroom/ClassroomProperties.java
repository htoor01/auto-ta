package io.github.htoor01.autota.classroom;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the Google Classroom integration, bound from {@code autota.classroom.*}.
 *
 * @param applicationName name sent to Google with every API request
 * @param credentialsPath OAuth client secrets downloaded from Google Cloud Console (kept out of git)
 * @param tokensDir       where OAuth access/refresh tokens are cached after the first login
 * @param oauthPort       local port the OAuth consent redirect returns to
 */
@ConfigurationProperties("autota.classroom")
public record ClassroomProperties(
        String applicationName,
        Path credentialsPath,
        Path tokensDir,
        int oauthPort) {
}
