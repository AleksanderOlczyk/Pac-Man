import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        System.setOut(
                new PrintStream(
                        new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8
                )
        );

        SwingUtilities.invokeLater(
            () -> {
                new Menu();
            }
        );
    }
}
