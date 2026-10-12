package io.github.htoor01.autota.classroom.google;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.drive.DriveScopes;

import io.github.htoor01.autota.classroom.ClassroomProperties;

/**
 * Signs in to Google using the installed-app OAuth flow: on first run a browser opens for consent,
 * after which tokens are cached in {@code tokensDir} and reused.
 *
 * <p>This flow suits running AutoTA locally. A deployed server will need a web-server OAuth flow instead.
 */
class GoogleCredentialFactory {

    /**
     * Permissions AutoTA requests. If you change these, delete the tokens directory so you are
     * asked for consent again.
     */
    static final List<String> SCOPES = List.of(
            ClassroomScopes.CLASSROOM_COURSES_READONLY,
            ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS,
            ClassroomScopes.CLASSROOM_ROSTERS_READONLY,
            DriveScopes.DRIVE_READONLY);

    private final ClassroomProperties properties;

    GoogleCredentialFactory(ClassroomProperties properties) {
        this.properties = properties;
    }

    Credential authorize(NetHttpTransport transport, JsonFactory jsonFactory) throws IOException {
        if (!Files.isRegularFile(properties.credentialsPath())) {
            throw new FileNotFoundException("Google OAuth client secrets not found at "
                    + properties.credentialsPath().toAbsolutePath()
                    + ". Download a Desktop app OAuth client from Google Cloud Console (see README),"
                    + " or run with the 'mock' profile.");
        }

        GoogleClientSecrets clientSecrets;
        try (Reader reader = Files.newBufferedReader(properties.credentialsPath())) {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, reader);
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(properties.tokensDir().toFile()))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(properties.oauthPort())
                .build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
