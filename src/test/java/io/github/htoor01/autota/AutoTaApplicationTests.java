package io.github.htoor01.autota;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.htoor01.autota.classroom.ClassroomClient;

/** Starts the full application with the default (mock) profile, so no Google credentials are needed. */
@SpringBootTest
class AutoTaApplicationTests {

    @Autowired
    private ClassroomClient classroomClient;

    @Test
    void startsWithMockClassroomByDefault() {
        assertThat(classroomClient.getClass().getSimpleName()).isEqualTo("MockClassroomClient");
        assertThat(classroomClient.listCourses()).isNotEmpty();
    }
}
