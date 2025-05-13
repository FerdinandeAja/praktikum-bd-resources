package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Assignment {
    private long id;
    private String name;
    private String instructions;
    private String answerKey;
    private int userGrade;

    // Konstruktor dengan argumen (yang sudah ada)
    public Assignment(long id, String name, String instructions, String answerKey) {
        this.id = id;
        this.name = name;
        this.instructions = instructions;
        this.answerKey = answerKey;
        this.userGrade = 0; // Atau nilai default lain
    }

    public Assignment(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.name = rs.getString("name");
        this.instructions = rs.getString("instructions");
        this.answerKey = rs.getString("answer_key");
        this.userGrade = 0; // Atau nilai default lain
    }

    // Konstruktor tanpa argumen (tambahkan ini)
    public Assignment() {
        // Inisialisasi nilai default jika perlu
        this.userGrade = 0;
    }

    // Getter dan setter untuk semua properti
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getAnswerKey() {
        return answerKey;
    }

    public void setAnswerKey(String answerKey) {
        this.answerKey = answerKey;
    }

    public int getUserGrade() {
        return userGrade;
    }

    public void setUserGrade(int userGrade) {
        this.userGrade = userGrade;
    }

    @Override
    public String toString() {
        return name; // Mengembalikan nama tugas sebagai representasi string
    }
}