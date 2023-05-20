import javax.swing.*;
import java.awt.*;

public class GameSettings extends JFrame {

    private static final int MIN_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private JFrame mainMenu, errorFrame;

    public GameSettings(JFrame mainMenu) {
        //window settings
        setTitle("Game Settings");
        setSize(300, 100);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        //main menu frame
        this.mainMenu = mainMenu;

        //panel settings
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);

        //label settings
        JLabel label = new JLabel("Choose board size: ");
        label.setOpaque(false);
        label.setForeground(Color.WHITE);
        label.setBackground(Color.BLACK);
        label.setFont(Menu.REXLIA);

        //text field settings
        JTextField height = new JTextField(3);
        height.setText("10");
        height.setForeground(Color.WHITE);
        height.setBackground(Color.BLACK);
        height.setOpaque(false);


        //separator
        JLabel separator = new JLabel("x");
        separator.setOpaque(false);
        separator.setForeground(Color.WHITE);
        separator.setBackground(Color.BLACK);
        separator.setFont(Menu.REXLIA);

        //text field settings
        JTextField width = new JTextField(3);
        width.setText("10");
        width.setForeground(Color.WHITE);
        width.setBackground(Color.BLACK);
        width.setOpaque(false);

        height.addActionListener(e -> {
            int heightSize, widthSize;
            if (isValueInt(height) && isValueInt(width)) {
                heightSize = Integer.parseInt(height.getText());
                widthSize = Integer.parseInt(width.getText());
                if (widthSize < MIN_SIZE || widthSize > MAX_SIZE
                        || heightSize < MIN_SIZE || heightSize > MAX_SIZE) {
                    showError("Board size must be between 10 and 100");
                } else {
                    startGame(heightSize, widthSize);
                }
            } else
                showError("Please provide only integers");
        });

        width.addActionListener(e -> {
            int heightSize, widthSize;
            if (isValueInt(height) && isValueInt(width)) {
                heightSize = Integer.parseInt(height.getText());
                widthSize = Integer.parseInt(width.getText());
                if (widthSize < MIN_SIZE || widthSize > MAX_SIZE
                        || heightSize < MIN_SIZE || heightSize > MAX_SIZE) {
                    showError("Board size must be between 10 and 100");
                } else {
                    startGame(heightSize, widthSize);
                }
            } else
                showError("Please provide only integers");
        });


        //start game button
        JButton startGameButton = new JButton("Start Game");
        startGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startGameButton.setOpaque(false);
        startGameButton.setContentAreaFilled(false);
        startGameButton.setBorderPainted(false);
        startGameButton.setForeground(Color.WHITE);
        startGameButton.setBackground(Color.BLACK);
        startGameButton.setFont(Menu.REXLIA);
        startGameButton.addActionListener(e -> {
            int heightSize, widthSize;
            if (isValueInt(height) && isValueInt(width)) {
                heightSize = Integer.parseInt(height.getText());
                widthSize = Integer.parseInt(width.getText());
                if (widthSize < MIN_SIZE || widthSize > MAX_SIZE
                        || heightSize < MIN_SIZE || heightSize > MAX_SIZE) {
                    showError("Board size must be between 10 and 100");
                } else {
                    startGame(heightSize, widthSize);
                }
            } else
                showError("Please provide only integers");
        });

        panel.add(label);
        panel.add(height);
        panel.add(separator);
        panel.add(width);
        panel.add(startGameButton);
        add(panel);
        setVisible(true);
    }

    private void startGame(int height, int width) {
        //clossing windows
        if (errorFrame != null)
            errorFrame.dispose();
        mainMenu.dispose();
        dispose();

        SwingUtilities.invokeLater(
                () -> {
                    new Game(height, width);
                }
        );
    }

    private void showError(String mess) {
        if (errorFrame == null) {
            SwingUtilities.invokeLater(
                    () -> {
                        errorFrame = new Error(mess);
                    }
            );
        } else {
            errorFrame.dispose();
            SwingUtilities.invokeLater(
                    () -> {
                        errorFrame = new Error(mess);
                    }
            );
        }
    }

    public boolean isValueInt(JTextField textField) {
        try {
            int value = Integer.parseInt(textField.getText());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
