import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class SecureFileTransfer extends Application {

    private boolean isSender = true;
    private File selectedFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ناقل الملفات الآمن (Direct Ethernet)");

        // عناصر الواجهة الأساسية
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-color: #f4f4f4;");

        // اختيار الوضع (إرسال أو استقبال)
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton rbSend = new RadioButton("إرسال ملف (الطرف الأول)");
        rbSend.setToggleGroup(modeGroup);
        rbSend.setSelected(true);
        RadioButton rbReceive = new RadioButton("استقبال ملف (الطرف الثاني)");
        rbReceive.setToggleGroup(modeGroup);
        HBox modeBox = new HBox(20, rbSend, rbReceive);
        modeBox.setAlignment(Pos.CENTER);

        // حقول الإدخال
        TextField ipField = new TextField("192.168.1.11");
        ipField.setPromptText("IP الجهاز الآخر (للمرسل فقط)");

        PasswordField passField = new PasswordField();
        passField.setPromptText("كلمة المرور المشتركة للتشفير");

        // اختيار الملف
        Button btnSelectFile = new Button("اختر الملف...");
        Label lblFileName = new Label("لم يتم اختيار ملف");
        btnSelectFile.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            if(isSender) {
                selectedFile = fileChooser.showOpenDialog(primaryStage);
            } else {
                selectedFile = fileChooser.showSaveDialog(primaryStage);
            }
            if (selectedFile != null) lblFileName.setText(selectedFile.getName());
        });

        // شريط التقدم والزر الرئيسي
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        Label lblStatus = new Label("جاهز...");
        Button btnStart = new Button("بدء النقل");
        btnStart.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-weight: bold;");
        btnStart.setMaxWidth(Double.MAX_VALUE);

        // تفاعل الواجهة
        rbReceive.setOnAction(e -> {
            isSender = false;
            ipField.setDisable(true);
            btnSelectFile.setText("اختر مكان حفظ الملف...");
            lblFileName.setText("لم يتم تحديد مسار الحفظ");
        });

        rbSend.setOnAction(e -> {
            isSender = true;
            ipField.setDisable(false);
            btnSelectFile.setText("اختر الملف...");
            lblFileName.setText("لم يتم اختيار ملف");
        });

        // زر البدء وتشغيل النقل في Background Thread (لتجنب تجميد الواجهة)
        btnStart.setOnAction(e -> {
            String ip = ipField.getText();
            String pass = passField.getText();
            if (pass.isEmpty() || selectedFile == null) {
                lblStatus.setText("الرجاء إدخال كلمة المرور واختيار الملف!");
                return;
            }

            btnStart.setDisable(true);
            Thread transferThread = new Thread(() -> {
                if (isSender) {
                    sendFile(ip, pass, selectedFile, progressBar, lblStatus);
                } else {
                    receiveFile(pass, selectedFile, progressBar, lblStatus);
                }
                Platform.runLater(() -> btnStart.setDisable(false));
            });
            transferThread.setDaemon(true);
            transferThread.start();
        });

        root.getChildren().addAll(modeBox, new Label("عنوان IP للطرف الآخر:"), ipField,
                new Label("كلمة المرور (يجب أن تتطابق):"), passField,
                btnSelectFile, lblFileName, btnStart, progressBar, lblStatus);

        Scene scene = new Scene(root, 450, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- خوارزمية إرسال الملف ---
    private void sendFile(String ip, String password, File file, ProgressBar bar, Label status) {
        try {
            updateStatus(status, "جاري الاتصال بـ " + ip + "...");
            Socket socket = new Socket(ip, 5000); // استخدام بورت 5000
            updateStatus(status, "تم الاتصال! جاري التشفير والإرسال...");

            // إعداد التشفير AES
            byte[] key = generateKey(password);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            OutputStream out = socket.getOutputStream();
            out.write(iv); // إرسال الـ IV أولاً لفك التشفير

            // إرسال حجم الملف الأصلي لحساب التقدم
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeLong(file.length());

            CipherOutputStream cos = new CipherOutputStream(out, cipher);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[8192 * 4]; // 32KB chunk للسرعة
            long totalBytes = file.length();
            long sentBytes = 0;
            int read;

            while ((read = bis.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
                sentBytes += read;
                updateProgress(bar, (double) sentBytes / totalBytes);
            }

            cos.flush(); cos.close(); bis.close(); socket.close();
            updateStatus(status, "تم الإرسال بنجاح!");

        } catch (Exception e) {
            updateStatus(status, "خطأ أثناء الإرسال: " + e.getMessage());
        }
    }

    // --- خوارزمية استقبال الملف ---
    private void receiveFile(String password, File file, ProgressBar bar, Label status) {
        try {
            updateStatus(status, "في انتظار اتصال الطرف الآخر على منفذ 5000...");
            ServerSocket serverSocket = new ServerSocket(5000);
            Socket socket = serverSocket.accept();
            updateStatus(status, "تم الاتصال! جاري الاستقبال وفك التشفير...");

            InputStream in = socket.getInputStream();

            // استقبال الـ IV ومفتاح التشفير
            byte[] iv = new byte[16];
            in.read(iv);
            byte[] key = generateKey(password);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            // استقبال حجم الملف
            DataInputStream dis = new DataInputStream(in);
            long totalBytes = dis.readLong();

            CipherInputStream cis = new CipherInputStream(in, cipher);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[8192 * 4];
            long receivedBytes = 0;
            int read;

            while ((read = cis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
                receivedBytes += read;
                updateProgress(bar, (double) receivedBytes / totalBytes);
            }

            bos.flush(); bos.close(); cis.close(); socket.close(); serverSocket.close();
            updateStatus(status, "تم الاستلام بنجاح!");

        } catch (Exception e) {
            updateStatus(status, "خطأ، قد تكون كلمة المرور خاطئة: " + e.getMessage());
        }
    }

    // توليد مفتاح 256-bit من كلمة المرور باستخدام SHA-256
    private byte[] generateKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes("UTF-8"));
    }

    private void updateStatus(Label label, String text) {
        Platform.runLater(() -> label.setText(text));
    }

    private void updateProgress(ProgressBar bar, double progress) {
        Platform.runLater(() -> bar.setProgress(progress));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
