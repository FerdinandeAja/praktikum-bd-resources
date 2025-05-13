package com.example.bdsqltester.scenes.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import com.example.bdsqltester.datasources.MainDataSource;

public class AssignmentUserGradesController implements Initializable {

    @FXML
    private Label assignmentNameLabel;
    @FXML
    private TableView<UserGradeInfo> userGradesTableView;
    @FXML
    private TableColumn<UserGradeInfo, Number> userIdColumn;
    @FXML
    private TableColumn<UserGradeInfo, String> userNameColumn;
    @FXML
    private TableColumn<UserGradeInfo, Number> gradeColumn;

    private long assignmentId;
    private String assignmentName;
    private ObservableList<UserGradeInfo> userGrades = FXCollections.observableArrayList();

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
        loadUserGrades();
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
        assignmentNameLabel.setText("Nilai Pengguna untuk Tugas: " + this.assignmentName);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userIdColumn.setCellValueFactory(cellData -> cellData.getValue().userIdProperty());
        userNameColumn.setCellValueFactory(cellData -> cellData.getValue().userNameProperty());
        gradeColumn.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());
        userGradesTableView.setItems(userGrades);
    }

    private void loadUserGrades() {
        userGrades.clear();
        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT g.user_id, u.username, g.grade FROM grades g " +
                             "JOIN users u ON g.user_id = u.id " +
                             "WHERE g.assignment_id = ?")) {
            preparedStatement.setLong(1, assignmentId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int userId = resultSet.getInt("user_id");
                String username = resultSet.getString("username");
                int grade = resultSet.getInt("grade");
                userGrades.add(new UserGradeInfo(userId, username, grade));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle error loading grades (you might want to show an alert)
        }
    }

    public static class UserGradeInfo {
        private final SimpleIntegerProperty userId;
        private final SimpleStringProperty userName;
        private final SimpleIntegerProperty grade;

        public UserGradeInfo(int userId, String userName, int grade) {
            this.userId = new SimpleIntegerProperty(userId);
            this.userName = new SimpleStringProperty(userName);
            this.grade = new SimpleIntegerProperty(grade);
        }

        public int getUserId() {
            return userId.get();
        }

        public SimpleIntegerProperty userIdProperty() {
            return userId;
        }

        public String getUserName() {
            return userName.get();
        }

        public SimpleStringProperty userNameProperty() {
            return userName;
        }

        public int getGrade() {
            return grade.get();
        }

        public SimpleIntegerProperty gradeProperty() {
            return grade;
        }
    }
}