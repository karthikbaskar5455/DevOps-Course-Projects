package com.college;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StudentTest {

    @Test
    void shouldAcceptValidStudentName() {
        Student student = new Student("Akash", 21);

        assertTrue(student.hasValidName());
    }

    @Test
    void shouldRejectBlankStudentName() {
        Student student = new Student("   ", 21);

        assertFalse(student.hasValidName());
    }
}