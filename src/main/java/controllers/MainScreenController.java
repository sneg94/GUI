package controllers;

import general.Assumption;
import general.Configuration;
import general.Constants;
import general.Utilities;
import io.ConfigManager;
import io.ModelReader;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import network.AnalysisConnector;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: Make use of @NotNull and @Nullable annotations to avoid NullPointer-Exceptions.

public class MainScreenController {
    private static final String COMPONENT_REPOSITORY_FILENAME = "default.repository";
    private final String defaultSaveLocation;
    private AnalysisConnector analysisConnector;
    private HostServices hostServices;
    private File saveFile;
    private boolean isSaved;
    private String analysisPath;
    private String modelPath;

    private Map<String, ModelReader.ModelEntity> modelEntityMap;

    @FXML
    private Button performAnalysisButton;
    @FXML
    private ListView<Assumption> assumptions;
    @FXML
    private TextArea analysisOutputTextArea;
    @FXML
    private Label analysisPathLabel;
    @FXML
    private Label modelNameLabel;

    public MainScreenController() {
        this.defaultSaveLocation = Constants.USER_HOME_PATH + Constants.FILE_SYSTEM_SEPARATOR + "NewAssumptionSet.xml";
        this.isSaved = true;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void handleExitRequest(WindowEvent windowEvent) {
        if (!this.isSaved) {
            var confirmationResult = Utilities.showAlert(Alert.AlertType.CONFIRMATION,
                    "Unsaved Changes",
                    "There exist unsaved changed in the current configuration",
                    "Quit and discard the changes?");

            if (confirmationResult.isPresent() && confirmationResult.get() == ButtonType.CANCEL) {
                windowEvent.consume();
            }
        }
    }

    private boolean testAnalysisConnection(String uri) {
        this.analysisConnector = new AnalysisConnector(uri);

        // Test connection to analysis.
        var connectionSuccess = this.analysisConnector.testConnection().getKey() == 200;

        this.analysisPathLabel.setText(uri + (connectionSuccess ? " ✓" : " ❌"));
        return connectionSuccess;
    }

    private boolean isMissingAnalysisParameters() {
        return this.analysisPath == null || this.modelPath == null || this.assumptions.getItems().isEmpty();
    }

    @FXML
    private void handleNewAssumption(ActionEvent actionEvent) {
        if (this.analysisPath == null || this.analysisPath.isEmpty() ||
                this.modelEntityMap == null || this.modelEntityMap.isEmpty()) {
            Utilities.showAlert(Alert.AlertType.WARNING, "Warning", "Unable to create a new assumption!", "A path to a valid model and analysis first has to be set.");
            return;
        }

        // Create new modal window for entry of assumption parameters.
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("../UI/AssumptionSpecificationScreen.fxml"));
            AnchorPane root = loader.load();

            AssumptionSpecificationScreenController controller = loader.getController();
            Assumption newAssumption = new Assumption();
            controller.initAssumption(newAssumption);
            controller.initModelEntities(this.modelEntityMap);

            var stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Assumption Specification");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(((MenuItem) actionEvent.getSource()).getParentPopup().getOwnerWindow().getScene().getWindow());
            stage.showAndWait();

            // Only add assumption in case it was fully specified by the user.
            if (newAssumption.isFullySpecified()) {
                this.assumptions.getItems().add(newAssumption);
                this.performAnalysisButton.setDisable(this.isMissingAnalysisParameters());
                this.isSaved = false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @FXML
    private void newAnalysis() {
        // TODO Leverage isSavedField.
        System.out.println("Not implemented!");
    }

    @FXML
    private void openFromFile(ActionEvent actionEvent) {
        var stage = (Stage) ((MenuItem) actionEvent.getSource()).getParentPopup().getOwnerWindow();

        var fileChooser = new FileChooser();
        fileChooser.setTitle("Select an existing File");
        fileChooser.setInitialFileName("NewAssumptions.xml");
        fileChooser.setInitialDirectory(new File(Constants.USER_HOME_PATH));
        var selectedFile = fileChooser.showOpenDialog(stage);

        // File selection has been aborted by the user.
        if (selectedFile == null) {
            return;
        }

        if (!selectedFile.exists() || !selectedFile.isFile()) {
            var alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Loading Failed");
            alert.setContentText("The specified file could not be read!");

            alert.show();
        }

        try {
            Configuration configuration = ConfigManager.readConfig(selectedFile);

            // Init model entities from read model path.
            this.modelEntityMap = ModelReader.readFromRepositoryFile(new File(configuration.getModelPath()
                    + Constants.FILE_SYSTEM_SEPARATOR
                    + MainScreenController.COMPONENT_REPOSITORY_FILENAME));

            this.testAnalysisConnection(configuration.getAnalysisPath());
            this.analysisPath = configuration.getAnalysisPath();


            this.modelPath = configuration.getModelPath();
            var folders = this.modelPath.split(Constants.FILE_SYSTEM_SEPARATOR.equals("\\") ? "\\\\" : Constants.FILE_SYSTEM_SEPARATOR);
            this.modelNameLabel.setText(folders[folders.length - 1]);

            this.assumptions.getItems().clear();
            this.assumptions.getItems().addAll(configuration.getAssumptions());

            this.saveFile = selectedFile;
            this.performAnalysisButton.setDisable(this.isMissingAnalysisParameters());
            this.isSaved = true;
        } catch (XMLStreamException e) {
            Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Opening file failed", "The specified file is not a well-formed configuration.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Opening file failed", "The specified file could not be found.");
            e.printStackTrace();
        }
    }

    @FXML
    private void openRecent() {
        // TODO Maybe start with recent files in the current execution. Otherwise some form of persistent config file is required.
        System.out.println("Not implemented!");
    }

    @FXML
    private void saveToFile() {
        // No save file necessary if configuration is empty.
        if (this.assumptions.getItems().isEmpty() && this.analysisPath == null && this.modelPath == null) {
            return;
        }

        // Use default file if not otherwise set by the user.
        if (this.saveFile == null) {
            this.saveFile = new File(this.defaultSaveLocation);
        }

        // Avoid overwriting in case a file with the default name already exists.
        if (this.saveFile.exists() && this.saveFile.getAbsolutePath().equals(this.defaultSaveLocation)) {
            // Add number suffix until there is no conflict.
            int suffix = 1;
            do {
                this.saveFile = new File(this.defaultSaveLocation.substring(0, this.defaultSaveLocation.length() - 4) + suffix + ".xml");
                suffix++;
            } while (this.saveFile.exists());
        }

        // Write to save-file.
        Set<Assumption> assumptions = new HashSet<>(this.assumptions.getItems());
        try {
            this.saveFile.createNewFile();
            ConfigManager.writeConfig(this.saveFile, new Configuration(this.analysisPath, this.modelPath, assumptions));
            this.isSaved = true;
        } catch (FileNotFoundException e) {
            Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Saving failed", "The specified save location could not be found!");
            e.printStackTrace();
        } catch (IOException e) {
            Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Saving failed", "Could not write to file!");
            e.printStackTrace();
        } catch (XMLStreamException e) {
            Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Saving failed", "The current configuration is not well formed!");
            e.printStackTrace();
        }
    }

    @FXML
    private void saveAs(ActionEvent actionEvent) {
        var stage = (Stage) ((MenuItem) actionEvent.getSource()).getParentPopup().getOwnerWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Save Location");
        fileChooser.setInitialFileName("NewAssumptions.xml");
        fileChooser.setInitialDirectory(new File(Constants.USER_HOME_PATH));
        this.saveFile = fileChooser.showSaveDialog(stage);

        this.saveToFile();
    }

    @FXML
    private void handleQuit() {
        if (!this.isSaved) {
            var confirmationResult = Utilities.showAlert(Alert.AlertType.CONFIRMATION,
                    "Unsaved Changes",
                    "There exist unsaved changed in the current configuration",
                    "Quit and discard the changes?");

            if (confirmationResult.isPresent() && confirmationResult.get() == ButtonType.CANCEL) {
                return;
            }
        }

        Platform.exit();
    }

    @FXML
    private void openDocumentation() {
        this.hostServices.showDocument(Constants.DOCUMENTATION_URL);
    }

    @FXML
    private void handleAnalysisExecution() {
        if (this.modelPath != null && this.assumptions.getItems().size() > 0) {
            if (this.testAnalysisConnection(this.analysisPath)) {
                var analysisResponse = this.analysisConnector.performAnalysis(new AnalysisConnector.AnalysisParameter(this.modelPath, new HashSet<>(this.assumptions.getItems())));

                if (analysisResponse.getKey() != 0) {
                    this.analysisOutputTextArea.setText(analysisResponse.getValue());
                } else {
                    Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Communication with the analysis failed.", analysisResponse.getValue());
                }
            } else {
                Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Communication with the analysis failed.", "Connection to the analysis could not be established.");
            }
        }
    }

    @FXML
    private void handleAnalysisPathSelection() {
        var textInputDialog = new TextInputDialog(Constants.DEFAULT_ANALYSIS_PATH);
        textInputDialog.setTitle("Analysis URI");
        textInputDialog.setHeaderText("Please provide the web-service URI of the analysis.");
        textInputDialog.setContentText("URI:");

        var userInput = textInputDialog.showAndWait();

        userInput.ifPresent(input -> {
            this.analysisPath = input;
            this.isSaved = false;

            this.performAnalysisButton.setDisable(!this.testAnalysisConnection(input) && this.isMissingAnalysisParameters());
        });
    }

    @FXML
    private void handleModelNameSelection(MouseEvent mouseEvent) {
        var originatingLabel = (Label) mouseEvent.getSource();
        var stage = (Stage) originatingLabel.getScene().getWindow();

        var directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Model Folder");
        directoryChooser.setInitialDirectory(new File(Constants.USER_HOME_PATH));
        var selectedFolder = directoryChooser.showDialog(stage);

        // Check whether the user aborted the selection.
        if (selectedFolder != null) {
            var absolutePath = selectedFolder.getAbsolutePath();

            // Check whether the specified folder actually contains a repository file.
            File repositoryFile = new File(absolutePath + Constants.FILE_SYSTEM_SEPARATOR + MainScreenController.COMPONENT_REPOSITORY_FILENAME);
            if (repositoryFile.exists()) {
                // Load contents of repository file.
                try {
                    this.modelEntityMap = ModelReader.readFromRepositoryFile(repositoryFile);

                    // Accept valid selection.
                    this.modelPath = absolutePath;
                    // Only display last subfolder of the path for better readability.
                    var folders = this.modelPath.split(Constants.FILE_SYSTEM_SEPARATOR.equals("\\") ? "\\\\" : Constants.FILE_SYSTEM_SEPARATOR);
                    originatingLabel.setText(folders[folders.length - 1]);

                    this.performAnalysisButton.setDisable(this.isMissingAnalysisParameters());
                    this.isSaved = false;
                } catch (FileNotFoundException e) {
                    Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Loading model entities failed", "The repository file (default.repository) of the specified model could not be found.");
                    e.printStackTrace();
                } catch (XMLStreamException e) {
                    Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Loading model entities failed", "The repository file (default.repository) of the specified model was not well-formed.");
                    e.printStackTrace();
                }
            } else {
                // Invalid selection due to missing repository file.
                Utilities.showAlert(Alert.AlertType.ERROR, "Error", "Loading model entities failed", "The repository file (default.repository) of the specified model could not be found.");
            }
        }
    }
}
