package com.example.bdsqltester.scenes;

import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDoAssignmentController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label instructionsLabel;

    @FXML
    private TextArea answerTextArea;

    @FXML
    private Label userGradeLabel; // Sudah ditambahkan

    private Assignment currentAssignment;

    @FXML
    private Button backButton; // Sudah ada di FXML

    private String getLoggedInUsername() {
        return HelloApplication.getApplicationInstance().getLoggedInUsername();
    }

    public void setAssignment(Assignment assignment) {
        this.currentAssignment = assignment;
        titleLabel.setText(assignment.getName());
        instructionsLabel.setText(assignment.getInstructions());
        displayUserGradeForAssignment(getLoggedInUserId(), assignment.getId()); // Panggil untuk menampilkan nilai
    }

    @FXML
    void handleSubmitAssignment(ActionEvent event) throws IOException {
        if (currentAssignment != null) {
            String userAnswerQuery = answerTextArea.getText().trim();
            String correctAnswerQuery = currentAssignment.getAnswerKey(); // Ambil langsung

            Integer loggedInUserId = getLoggedInUserId();

            if (loggedInUserId == null) {
                showAlert("Error", "User tidak terotentikasi.");
                return;
            }

            // Tambahkan validasi: periksa apakah jawaban kosong
            if (userAnswerQuery.isEmpty()) {
                showAlert("Peringatan", "Anda belum memasukkan jawaban.");
                return; // Hentikan proses submit jika jawaban kosong
            }

            int grade = compareAndGrade(userAnswerQuery, correctAnswerQuery != null ? correctAnswerQuery.trim() : ""); // Trim hanya jika tidak null

            if (grade == 100) {
                Alert alertHasil = new Alert(Alert.AlertType.INFORMATION);
                alertHasil.setTitle("Hasil");
                alertHasil.setHeaderText("Penilaian Tugas");
                alertHasil.setContentText("Selamat! Jawaban Anda benar. Nilai Anda: " + grade);
                alertHasil.showAndWait();
                saveGrade(loggedInUserId, currentAssignment.getId(), grade);
                displayUserGradeForAssignment(loggedInUserId, currentAssignment.getId()); // Perbarui tampilan nilai setelah submit
                simpanSubmission(loggedInUserId, userAnswerQuery); // Simpan submission jika benar
                loadAssignmentListView();

            } else if (grade == 50) {
                Alert alertHasil = new Alert(Alert.AlertType.WARNING);
                alertHasil.setTitle("Hasil");
                alertHasil.setHeaderText("Penilaian Tugas");
                alertHasil.setContentText("Jawaban Anda sebagian benar. Nilai Anda: " + grade);
                alertHasil.showAndWait();
                saveGrade(loggedInUserId, currentAssignment.getId(), grade);
                displayUserGradeForAssignment(loggedInUserId, currentAssignment.getId()); // Perbarui tampilan nilai setelah submit
                simpanSubmission(loggedInUserId, userAnswerQuery); // Simpan submission jika sebagian benar
                loadAssignmentListView();
            } else if (grade == 0) {
                Alert alertSalah = new Alert(Alert.AlertType.ERROR);
                alertSalah.setTitle("Hasil");
                alertSalah.setHeaderText("Penilaian Tugas");
                alertSalah.setContentText("Maaf, jawaban Anda salah. Silakan coba lagi.");
                alertSalah.showAndWait();
                simpanSubmission(loggedInUserId, userAnswerQuery); // Tetap simpan submission yang salah
            } else {
                showAlert("Error", "Gagal membandingkan jawaban."); // Tangani nilai error dari compareAndGrade
            }

        } else {
            showAlert("Peringatan", "Tugas Tidak Ditemukan");
        }
    }

    @FXML
    void handleTestButtonClick(ActionEvent event) {
        String query = answerTextArea.getText();
        try (Connection connection = GradingDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            // Tampilkan hasil query
            StringBuilder results = new StringBuilder();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                if (columnName == null || columnName.isEmpty() || columnName.equals("?column?")) {
                    results.append("Kolom " + i).append("\t");
                } else {
                    results.append(columnName).append("\t");
                }
            }
            results.append("\n");
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    results.append(Objects.toString(resultSet.getObject(i))).append("\t");
                }
                results.append("\n");
            }

            showAlert("Hasil Tes", "Output Query Anda:", results.toString().isEmpty() ? "Tidak ada hasil." : results.toString());

        } catch (SQLException e) {
            showAlert("Error", "Gagal Menjalankan Query");
        }
    }

    @FXML
    void handleBackButton(ActionEvent event) throws IOException {
        loadAssignmentListView();
    }

    private Integer getLoggedInUserId() {
        String loggedInUsername = getLoggedInUsername();
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

    private void simpanSubmission(int userId, String submissionText) {
        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO user_submissions (assignment_id, user_id, submission_text, submission_date) VALUES (?, ?, ?, NOW())")) {
            preparedStatement.setLong(1, currentAssignment.getId());
            preparedStatement.setInt(2, userId);
            preparedStatement.setString(3, submissionText);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            showAlert("Error", "Gagal menyimpan jawaban");
            e.printStackTrace();
        }
    }

    private void loadAssignmentListView() throws IOException {
        HelloApplication app = HelloApplication.getApplicationInstance();
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/com/example/bdsqltester/user-view.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = (Stage) backButton.getScene().getWindow(); // Gunakan backButton untuk mendapatkan window
        stage.setTitle("Daftar Tugas");
        stage.setScene(scene);
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
        List<List<String>> results = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next()) {
            List<String> row = new ArrayList<>();
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

    private void saveGrade(int userId, long assignmentId, int grade) {
        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement checkStatement = connection.prepareStatement(
                     "SELECT grade FROM grades WHERE user_id = ? AND assignment_id = ?");
             PreparedStatement insertStatement = connection.prepareStatement(
                     "INSERT INTO grades (user_id, assignment_id, grade) VALUES (?, ?, ?)");
             PreparedStatement updateStatement = connection.prepareStatement(
                     "UPDATE grades SET grade = ? WHERE user_id = ? AND assignment_id = ? AND grade < ?")) {

            checkStatement.setInt(1, userId);
            checkStatement.setLong(2, assignmentId);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                int currentGrade = resultSet.getInt("grade");
                // Hanya update jika nilai baru lebih tinggi (opsional)
                if (grade > currentGrade) {
                    updateStatement.setInt(1, grade);
                    updateStatement.setInt(2, userId);
                    updateStatement.setLong(3, assignmentId);
                    updateStatement.setInt(4, grade);
                    updateStatement.executeUpdate();
                }
            } else {
                insertStatement.setInt(1, userId);
                insertStatement.setLong(2, assignmentId);
                insertStatement.setInt(3, grade);
                insertStatement.executeUpdate();
            }

        } catch (SQLException e) {
            showAlert("Error", "Gagal menyimpan nilai");
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void displayUserGradeForAssignment(int userId, long assignmentId) {
        try (Connection connection = MainDataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT grade FROM grades WHERE user_id = ? AND assignment_id = ?")) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setLong(2, assignmentId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int grade = resultSet.getInt("grade");
                userGradeLabel.setText(String.valueOf(grade));
            } else {
                userGradeLabel.setText("Belum Dinilai");
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal mengambil nilai pengguna untuk tugas ini.");
            e.printStackTrace();
        }
    }
}