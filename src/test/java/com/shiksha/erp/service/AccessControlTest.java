package com.shiksha.erp.service;

import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.Teacher;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.repository.StudentRepository;
import com.shiksha.erp.repository.TeacherBatchRepository;
import com.shiksha.erp.repository.TeacherRepository;
import com.shiksha.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherBatchRepository teacherBatchRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private TeacherAccessHelper teacherAccessHelper;

    @InjectMocks
    private ParentStudentHelper parentStudentHelper;

    @Test
    @DisplayName("TeacherAccessHelper: should return true only for batches owned by the teacher")
    void testTeacherBatchOwnership() {
        Teacher teacher1 = Teacher.builder().id(1L).build();

        when(teacherBatchRepository.existsByTeacherIdAndClassBatchId(1L, 100L)).thenReturn(true);
        when(teacherBatchRepository.existsByTeacherIdAndClassBatchId(1L, 200L)).thenReturn(false);

        assertTrue(teacherAccessHelper.isBatchOwnedByTeacher(100L, teacher1));
        assertFalse(teacherAccessHelper.isBatchOwnedByTeacher(200L, teacher1));
    }

    @Test
    @DisplayName("ParentStudentHelper: validateParentAccess should throw when parent accesses another student's ID")
    void testParentDataIsolation_UnauthorizedAccessThrows() {
        User parentUser = User.builder().id(5L).username("parent1").build();
        Student linkedStudent = Student.builder().id(10L).name("Aarav").build();

        when(userRepository.findByUsername("parent1")).thenReturn(Optional.of(parentUser));
        when(studentRepository.findFirstByParentUserId(5L)).thenReturn(Optional.of(linkedStudent));

        // Authorized: accessing own student (ID 10)
        assertDoesNotThrow(() -> parentStudentHelper.validateParentAccess("parent1", 10L));

        // Unauthorized: accessing another student (ID 99)
        assertThrows(RuntimeException.class, () -> parentStudentHelper.validateParentAccess("parent1", 99L));
    }
}
