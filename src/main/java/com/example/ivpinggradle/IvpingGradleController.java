package com.example.ivpinggradle;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class IvpingGradleController {
    private static final String DATA_FOLDER = "Ivping_data";
    private static final String EXCEL_FILE_NAME = "data_hosts.xlsx";
    private static final String SSH_BASE_URL = "https://s6006as3039.petrobras.biz/cgi-bin/ssh.sh?";

    private final ObservableList<HostData> dataList = FXCollections.observableArrayList();
    public MenuItem menuClose;
    public Button btnPing;

    @FXML
    private TableView<HostData> tableView;
    @FXML
    private TableColumn<HostData, String> colHost;
    @FXML
    private TableColumn<HostData, String> colIp;
    @FXML
    private TableColumn<HostData, String> colLocation;
    @FXML
    public CheckBox chkPingContinuous;
    @FXML
    public Button btnSsh;
    @FXML
    private TextField txtSearch;

    @FXML
    private void initialize() {
        setupTableColumns();
        loadExcelData();
        FilteredList<HostData> filteredData = new FilteredList<>(dataList, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filteredData.setPredicate(hostData -> {
            if (newValue == null || newValue.isBlank()) {
                return true;
            }
            String lowerCaseFilter = newValue.toLowerCase();
            return hostData.host().toLowerCase().contains(lowerCaseFilter) ||
                    hostData.ip().toLowerCase().contains(lowerCaseFilter) ||
                    hostData.location().toLowerCase().contains(lowerCaseFilter);
        }));

        SortedList<HostData> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        if (!dataList.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onClearClicked() {
        txtSearch.clear();
        txtSearch.requestFocus();
    }

    @FXML
    private void onPingClicked() {
        HostData selectedHost = tableView.getSelectionModel().getSelectedItem();

        if (selectedHost == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aviso");
            alert.setHeaderText(null);
            alert.setContentText("Por favor, selecione um Host na tabela antes de executar o Ping.");
            alert.showAndWait();
            return;
        }

        boolean continuous = chkPingContinuous.isSelected();
        PingUtils.runPing(selectedHost, continuous);
    }

    private void setupTableColumns() {
        colHost.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().host()));
        colIp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ip()));
        colLocation.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().location()));
        colHost.setPrefWidth(170);
        colIp.setPrefWidth(150);

        tableView.widthProperty().addListener((observable, oldValue, newWidth) -> {
            double tableWidth = newWidth.doubleValue();
            double remainingWidth = tableWidth - (colHost.getWidth() + colIp.getWidth());
            if (remainingWidth > 0) {
                colLocation.setPrefWidth(remainingWidth);
            }
        });
    }

    private void loadExcelData() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String filePath;

        if (localAppData != null && !localAppData.isBlank()) {
            filePath = localAppData + File.separator + DATA_FOLDER + File.separator + EXCEL_FILE_NAME;
        } else {
            String userHome = System.getProperty("user.home");
            filePath = userHome + File.separator + "AppData" + File.separator + "Local" +
                    File.separator + DATA_FOLDER + File.separator + "data2_hosts.xlsx";// comparar com original no ChatGPT
        }

        File excelFile = new File(filePath);
        if (!excelFile.exists()) {
            System.err.println("Arquivo nao encontrado: " + filePath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row != null) {
                    String hostName = getCellValue(row.getCell(0));
                    String ipAddress = getCellValue(row.getCell(1));
                    String deviceLocation = getCellValue(row.getCell(2));
                    dataList.add(new HostData(hostName, ipAddress, deviceLocation));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    @FXML
    private void handleCloseApp() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar saída");
        alert.setHeaderText(null);
        alert.setContentText("Deseja realmente fechar o aplicativo?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                javafx.application.Platform.exit();
            }
        });
    }

    @FXML
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sobre");
        alert.setHeaderText("Ivping");
        alert.setContentText("Aplicativo para executar Testes de Ping em Hosts");
        alert.showAndWait();
    }

    @FXML
    private void onSshClicked() {
        HostData selectedHost = tableView.getSelectionModel().getSelectedItem();

        if (selectedHost == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aviso");
            alert.setHeaderText(null);
            alert.setContentText("Por favor, selecione um Host na tabela antes de abrir a conexão SSH.");
            alert.showAndWait();
            return;
        }

        String hostName = selectedHost.host();

        if (!hostName.matches("^(SW|RT).*")) {
            System.out.println("Apenas Switch e Roteador");
            return;
        }

        String url = SSH_BASE_URL + hostName;

        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText(null);
            alert.setContentText("Não foi possível abrir o navegador.\n" + e.getMessage());
            alert.showAndWait();
        }
    }
}
