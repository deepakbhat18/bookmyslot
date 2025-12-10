package com.college.bookmyslot.dto;

public class CalendarEventDto {

    private String title;
    private String start;  // ISO datetime string
    private String end;    // ISO datetime string
    private String teacherName;
    private String studentName;
    private String type;   // TEACHER_SLOT or STUDENT_BOOKING

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getStart() { return start; }

    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }

    public void setEnd(String end) { this.end = end; }

    public String getTeacherName() { return teacherName; }

    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getStudentName() { return studentName; }

    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }
}
