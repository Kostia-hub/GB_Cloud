import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

public class Controller implements Initializable {

    public ListView<String> listView;
    public TextField text;
    public Button send;
    private Socket socket;
    private static DataInputStream is;
    private static DataOutputStream os;
    private String clientPath = "client/ClientStorage";

    public static void stop() {
        try {
            os.writeUTF("quit");
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(ActionEvent actionEvent) {
        String message = text.getText();
        if (message.equals("quit")) {
            try {
                os.writeUTF("quit");
                os.flush();
                text.setText(is.readUTF());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // ./upload fileName -> ./upload 1.txt
        // ./download fileName
        String[] tokens = message.split(" ");
        String command = tokens[0];
        String fileName = tokens[1];
        if (command.equals("./upload")) {
            File file = new File("client/ClientStorage/" + fileName);
            try (FileInputStream fis = new FileInputStream(file)) {
                os.writeUTF(command);
                os.writeUTF(fileName);
                os.writeLong(file.length());
                byte[] buffer = new byte[256];
                int read = 0;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                String response = is.readUTF();
                if (response.equals("OK")) {                  // Если от сервака пришло "OK"
                    //createNewWindow("File uploaded!");  // всплывает сообщение "File uploaded!"
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "File uploaded!", ButtonType.OK); //Всплывающее окно Alert
                    alert.showAndWait();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {  // Иначе берем файл с сервака
            try {
                os.writeUTF(command);
                os.writeUTF(fileName);
                String response = is.readUTF();
                if (response.equals("OK")) {
                    long fileLength = is.readLong();
                    File file = new File("client/ClientStorage/" + fileName); //Создается файл на клиенте
                    file.createNewFile();
                    try (FileOutputStream fos = new FileOutputStream(file)) {           //Открываем файл на запись; try с ресурсами автоклозебл
                        byte[] buffer = new byte[256];
                        if (fileLength < 256) {
                            fileLength += 256;   //      Чтобы была хоть 1 итерация
                        }                                                //   |
                        int read = 0;                                    //  \/
                        for (int i = 0; i < fileLength / 256; i++) { //    Вот тут (если fileLength будет < 256, то будет 0 итераций)
                            read = is.read(buffer);
                            fos.write(buffer, 0, read);
                        }
                        if ((fileLength-256) == file.length()) {
                            createNewWindow("File downloaded!");
                        }
                    }
                } else {
                    createNewWindow("file not found!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        text.clear();
    }

    public void createNewWindow(String mess){    //метод вызывает всплывающее окно
        Label secondLabel = new Label(mess);
        StackPane secondaryLayout = new StackPane();
        secondaryLayout.getChildren().add(secondLabel);
        // New window (Stage)
        Stage newWindow = new Stage(); //Новое окно
        newWindow.setScene(new Scene(secondaryLayout, 230, 100));
        newWindow.show(); //show - показ окна
    }

    public void initialize(URL location, ResourceBundle resources) {
        text.setOnAction(this::sendMessage);
        File dir = new File(clientPath);
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            listView.getItems().add(file.getName() + "(" + file.length() + " bytes)");
        }
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}