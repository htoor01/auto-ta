package io.github.htoor01.autota.classroom.google;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.drive.Drive;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.htoor01.autota.classroom.ClassroomProperties;

/** Builds the authenticated Google API clients. Only active with the {@code google} profile. */
@Configuration
@Profile("google")
class GoogleConfig {

    @Bean
    NetHttpTransport googleHttpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    JsonFactory googleJsonFactory() {
        return GsonFactory.getDefaultInstance();
    }

    @Bean
    Credential googleCredential(ClassroomProperties properties, NetHttpTransport transport,
                                JsonFactory jsonFactory) throws IOException {
        return new GoogleCredentialFactory(properties).authorize(transport, jsonFactory);
    }

    @Bean
    Classroom classroom(ClassroomProperties properties, NetHttpTransport transport,
                        JsonFactory jsonFactory, Credential credential) {
        return new Classroom.Builder(transport, jsonFactory, credential)
                .setApplicationName(properties.applicationName())
                .build();
    }

    @Bean
    Drive drive(ClassroomProperties properties, NetHttpTransport transport,
                JsonFactory jsonFactory, Credential credential) {
        return new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName(properties.applicationName())
                .build();
    }
}
