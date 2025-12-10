package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.TeacherSlot;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TeacherSlotRepository extends JpaRepository<TeacherSlot, Long> {

    List<TeacherSlot> findByTeacher(User teacher);

    List<TeacherSlot> findByTeacherAndDate(User teacher, LocalDate date);

    List<TeacherSlot> findByDateAndStatus(LocalDate date, TeacherSlot.Status status);

    long countByStatus(TeacherSlot.Status status);
}
