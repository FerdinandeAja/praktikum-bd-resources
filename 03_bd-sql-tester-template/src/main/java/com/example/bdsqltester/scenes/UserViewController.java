package com.example.bdsqltester.scenes;

import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class UserViewController implements Initializable {

    @FXML
    private TableView<Assignment> assignmentTableView;

    @FXML
    private TableColumn<Assignment, Long> idColumn;

    @FXML
    private TableColumn<Assignment, String> nameColumn;

    @FXML
    private TableColumn<Assignment, Integer> nilaiSayaColumn;

    @FXML
    private TableColumn<Assignment, Void> actionColumn; // Kolom untuk tombol "Kerjakan"

    private ObservableList<Assignment> assignmentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Integer loggedInUserId = getLoggedInUserIdFromSession();

        if (loggedInUserId != null) {
            setupTableView();
            loadAssignments(loggedInUserId);
        } else {
            showAlert("Error", "Gagal mendapatkan ID user.");
        }
    }

    private Integer getLoggedInUserIdFromSession() {
        String loggedInUsername = HelloApplication.getApplicationInstance().getLoggedInUsername();
        if (loggedInUsername == null || loggedInUsername.isEmpty()) {
            showAlert("Error", "Username pengguna tidak ditemukan.");
            return null;
        }

        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            preparedStatement.setString(1, loggedInUsername);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            } else {
                showAlert("Error", "Pengguna dengan username " + loggedInUsername + " tidak ditemukan.");
                return null;
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal mengambil ID user.");
            e.printStackTrace();
            return null;
        }
    }

    private void setupTableView() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nilaiSayaColumn.setCellValueFactory(new PropertyValueFactory<>("userGrade"));

        // Tambahkan tombol "Kerjakan" untuk setiap baris
        actionColumn.setCellFactory(param -> new TableCell<Assignment, Void>() {
            private final Button viewButton = new Button("Kerjakan");

            {
                viewButton.setOnAction(event -> {
                    Assignment assignment = getTableView().getItems().get(getIndex());
                    loadDoAssignmentView(assignment);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewButton);
                }
            }
        });

        assignmentTableView.setItems(assignmentList);
    }

    private void loadAssignments(int userId) {
        assignmentList.clear();
        Map<Long, Integer> userGrades = getUserGrades(userId);

        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT id, name, instructions, answer_key FROM assignments")) { // Perbaikan: ambil 'answer_key'
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Assignment assignment = new Assignment();
                assignment.setId(resultSet.getLong("id"));
                assignment.setName(resultSet.getString("name"));
                assignment.setInstructions(resultSet.getString("instructions")); // Perbaikan: set 'instructions'
                assignment.setAnswerKey(resultSet.getString("answer_key")); // Perbaikan: set 'answer_key'
                assignment.setUserGrade(userGrades.getOrDefault(assignment.getId(), 0));
                assignmentList.add(assignment);
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal memuat daftar tugas.");
            e.printStackTrace();
        }
    }

    private Map<Long, Integer> getUserGrades(int userId) {
        Map<Long, Integer> userGrades = new HashMap<>();
        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT assignment_id, grade FROM grades WHERE user_id = ?")) {
            preparedStatement.setInt(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                userGrades.put(resultSet.getLong("assignment_id"), resultSet.getInt("grade"));
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal mengambil nilai pengguna.");
            e.printStackTrace();
        }
        return userGrades;
    }

    private void loadDoAssignmentView(Assignment assignment) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/com/example/bdsqltester/user-do-assignment.fxml"));
            Scene scene = new Scene(loader.load());
            UserDoAssignmentController controller = loader.getController();
            controller.setAssignment(assignment);
            Stage stage = (Stage) assignmentTableView.getScene().getWindow();
            stage.setTitle("Kerjakan Tugas");
            stage.setScene(scene);
        } catch (IOException e) {
            showAlert("Error", "Gagal memuat tampilan tugas.");
            e.printStackTrace();
        }
    }

    @FXML
    void handleSignOut(ActionEvent event) throws IOException {
        // Clear the loggedInUsername
        HelloApplication.getApplicationInstance().setLoggedInUsername(null);

        // Kembali ke tampilan login
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) assignmentTableView.getScene().getWindow();
        stage.setTitle("Login");
        stage.setScene(scene);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}