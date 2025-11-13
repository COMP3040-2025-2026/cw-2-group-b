package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Course;
import com.nottingham.mynottingham.backend.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    List<CourseSchedule> findByCourse(Course course);

    List<CourseSchedule> findByDayOfWeek(DayOfWeek dayOfWeek);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.course.teacher.id = :teacherId")
    List<CourseSchedule> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.course.teacher.id = :teacherId AND cs.dayOfWeek = :dayOfWeek")
    List<CourseSchedule> findByTeacherIdAndDayOfWeek(@Param("teacherId") Long teacherId, @Param("dayOfWeek") DayOfWeek dayOfWeek);
}
