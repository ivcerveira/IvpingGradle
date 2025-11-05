package com.example.ivpinggradle;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class IvpingGradleController {
    private static final String SSH_BASE_URL = "https://s6006as3039.petrobras.biz/cgi-bin/ssh.sh?";

    private final ObservableList<HostData> dataList = FXCollections.observableArrayList();
    public MenuItem menuClose;
    public Button pingButton;

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

        // NOVA CHAMADA
        ExcelHostLoader excelLoader = new ExcelHostLoader();
        dataList.addAll(excelLoader.loadHosts());

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

        if (!dataList.isEmpty()) {tableView.getSelectionModel().selectFirst();}
        setupContextMenu();
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
            showWarning();
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

    private void showWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Conexão SSH não disponível");
        alert.setHeaderText(null);
        alert.setContentText("Apenas equipamentos de rede (SW/RT) suportam acesso SSH.");
        alert.initOwner(pingButton.getScene().getWindow());
        alert.showAndWait();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                getCopyHostnameItem(),
                getCopyIpItem(),
                getCopyLocationItem()
        );

        // Aplica o menu em cada linha da TableView
        tableView.setRowFactory(tv -> {
            TableRow<HostData> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getIndex()); // garante que a linha clicada fique selecionada
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private MenuItem getCopyIpItem() {
        MenuItem copyIpItem = new MenuItem("Copiar IP");
        copyIpItem.setOnAction(event -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.ip());
            }
        });
        return copyIpItem;
    }

    private MenuItem getCopyHostnameItem() {
        MenuItem copyHostItem = new MenuItem("Copiar Hostname");
        copyHostItem.setOnAction(event -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.host());
            }
        });
        return copyHostItem;
    }

    private MenuItem getCopyLocationItem() {
        MenuItem copyLocationItem = new MenuItem("Copiar Location");
        copyLocationItem.setOnAction(event -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.location());
            }
        });
        return copyLocationItem;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}
