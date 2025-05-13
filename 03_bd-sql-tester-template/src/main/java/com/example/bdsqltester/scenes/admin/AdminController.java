package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.GradingDataSource;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminController implements Initializable {

    @FXML
    private ListView<Assignment> assignmentList;
    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea instructionsField;
    @FXML
    private TextArea answerKeyField;

    private ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private Assignment selectedAssignment;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        assignmentList.setItems(assignments);
        loadAssignments();

        assignmentList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedAssignment = newValue;
            if (selectedAssignment != null) {
                populateFields(selectedAssignment);
            } else {
                clearFields();
            }
        });
    }

    private void loadAssignments() {
        assignments.clear();
        try (Connection connection = MainDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id, name, instructions, answer_key FROM assignments")) {
            while (resultSet.next()) {
                Assignment assignment = new Assignment();
                assignment.setId(resultSet.getLong("id"));
                assignment.setName(resultSet.getString("name"));
                assignment.setInstructions(resultSet.getString("instructions"));
                assignment.setAnswerKey(resultSet.getString("answer_key"));
                assignments.add(assignment);
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal memuat daftar tugas.");
            e.printStackTrace();
        }
    }

    private void populateFields(Assignment assignment) {
        idField.setText(String.valueOf(assignment.getId()));
        nameField.setText(assignment.getName());
        instructionsField.setText(assignment.getInstructions());
        answerKeyField.setText(assignment.getAnswerKey());
    }

    private void clearFields() {
        idField.clear();
        nameField.clear();
        instructionsField.clear();
        answerKeyField.clear();
        selectedAssignment = null;
    }

    @FXML
    void onNewAssignmentClick(ActionEvent event) {
        clearFields();
    }

    @FXML
    void onSaveClick(ActionEvent event) {
        String name = nameField.getText();
        String instructions = instructionsField.getText();
        String answerKey = answerKeyField.getText();

        if (name.isEmpty() || instructions.isEmpty() || answerKey.isEmpty()) {
            showAlert("Peringatan", "Harap isi semua kolom.");
            return;
        }

        try (Connection connection = MainDataSource.getConnection()) {
            String sql;
            PreparedStatement preparedStatement;

            if (name.isEmpty() || instructions.isEmpty() || answerKey.isEmpty()) {
                sql = "UPDATE assignments SET name = ?, instructions = ?, answer_key = ? WHERE id = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, instructions);
                preparedStatement.setString(3, answerKey);
                preparedStatement.setLong(4, selectedAssignment.getId());
            } else {
                sql = "INSERT INTO assignments (name, instructions, answer_key) VALUES (?, ?, ?)";
                preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, instructions);
                preparedStatement.setString(3, answerKey);
            }

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Sukses", "Tugas berhasil disimpan.");
                loadAssignments(); // Refresh list
            } else {
                showAlert("Error", "Gagal menyimpan tugas.");
            }

        } catch (SQLException e) {
            showAlert("Error Database", "Gagal menyimpan tugas ke database.");
            e.printStackTrace();
        }
    }

    @FXML
    void onDeleteClick(ActionEvent event) {
        if (selectedAssignment == null) {
            showAlert("Peringatan", "Pilih tugas yang ingin dihapus.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Tugas");
        alert.setContentText("Apakah Anda yakin ingin menghapus tugas dengan ID " + selectedAssignment.getId() + "?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection connection = MainDataSource.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM assignments WHERE id = ?")) {
                    preparedStatement.setLong(1, selectedAssignment.getId());
                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        showAlert("Sukses", "Tugas berhasil dihapus.");
                        loadAssignments(); // Refresh list
                        clearFields();
                    } else {
                        showAlert("Error", "Gagal menghapus tugas.");
                    }
                } catch (SQLException e) {
                    showAlert("Error Database", "Gagal menghapus tugas dari database.");
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    void onTestButtonClick(ActionEvent event) {
        if (selectedAssignment == null) {
            showAlert("Peringatan", "Pilih tugas untuk diuji.");
            return;
        }
        testAnswerKey(selectedAssignment);
    }

    private void testAnswerKey(Assignment assignment) {
        TextInputDialog dialog = new TextInputDialog(assignment.getAnswerKey());
        dialog.setTitle("Uji Kunci Jawaban");
        dialog.setHeaderText("Masukkan query untuk diuji dengan kunci jawaban:");
        dialog.setContentText("Query:");

        dialog.showAndWait().ifPresent(userQuery -> {
            int result = compareAndGrade(userQuery, assignment.getAnswerKey());
            showAlert("Hasil Tes", "Hasil Perbandingan:", getComparisonResultText(result));
        });
    }

    private String getComparisonResultText(int result) {
        return switch (result) {
            case 100 -> "Jawaban benar (identik).";
            case 50 -> "Jawaban sebagian benar (isi sama, urutan beda).";
            case 0 -> "Jawaban salah.";
            default -> "Gagal membandingkan.";
        };
    }

    private int compareAndGrade(String userAnswerQuery, String correctAnswerQuery) {
        try (Connection gradingConnection = GradingDataSource.getConnection();
             Statement userStatement = gradingConnection.createStatement();
             ResultSet userResultSet = userStatement.executeQuery(userAnswerQuery);
             Statement correctStatement = gradingConnection.createStatement();
             ResultSet correctResultSet = correctStatement.executeQuery(correctAnswerQuery)) {

            List<List<String>> userResults = getResultSetAsList(userResultSet);
            List<List<String>> correctResults = getResultSetAsList(correctResultSet);

            if (userResults.equals(correctResults)) {
                return 100;
            } else if (areContentsEqualIgnoringOrder(userResults, correctResults)) {
                return 50;
            } else {
                return 0;
            }

        } catch (SQLException e) {
            showAlert("Error", "Gagal Membandingkan Jawaban");
            return -1; // Return nilai error jika gagal membandingkan
        }
    }

    private List<List<String>> getResultSetAsList(ResultSet resultSet) throws SQLException {
        List<List<String>> results = new ArrayList<>(); // Inisialisasi ArrayList
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            List<String> row = new ArrayList<>(); // Inisialisasi ArrayList
            for (int i = 1; i <= columnCount; i++) {
                row.add(Objects.toString(resultSet.getObject(i)));
            }
            results.add(row);
        }
        return results;
    }

    private boolean areContentsEqualIgnoringOrder(List<List<String>> list1, List<List<String>> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        List<List<String>> sortedList1 = list1.stream().map(row -> row.stream().sorted().collect(Collectors.toList())).sorted(this::compareRows).collect(Collectors.toList());
        List<List<String>> sortedList2 = list2.stream().map(row -> row.stream().sorted().collect(Collectors.toList())).sorted(this::compareRows).collect(Collectors.toList());
        return sortedList1.equals(sortedList2);
    }

    private int compareRows(List<String> row1, List<String> row2) {
        for (int i = 0; i < Math.min(row1.size(), row2.size()); i++) {
            int comparisonResult = row1.get(i).compareTo(row2.get(i));
            if (comparisonResult != 0) {
                return comparisonResult;
            }
        }
        return Integer.compare(row1.size(), row2.size());
    }

    @FXML
    void onShowGradesClick(ActionEvent event) {
        if (selectedAssignment == null) {
            showAlert("Peringatan", "Pilih tugas untuk melihat nilai pengguna.");
            return;
        }

        try {
            long assignmentId = selectedAssignment.getId();
            String assignmentName = selectedAssignment.getName();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/bdsqltester/assignment-user-grades-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Nilai Pengguna untuk Tugas: " + assignmentName);
            stage.setScene(new Scene(loader.load()));

            AssignmentUserGradesController controller = loader.getController();
            controller.setAssignmentId(assignmentId);
            controller.setAssignmentName(assignmentName);

            stage.show();

        } catch (IOException e) {
            showAlert("Error", "Gagal memuat tampilan nilai pengguna.");
            e.printStackTrace();
        }
    }

    @FXML
    void onLogoutClick(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/bdsqltester/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setScene(scene);
            stage.show();

            // Tutup tampilan admin saat ini
            ((Stage) (((Button) event.getSource()).getScene().getWindow())).close();

        } catch (IOException e) {
            showAlert("Error", "Gagal memuat tampilan login.");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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