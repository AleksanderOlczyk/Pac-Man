import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SaveScore extends JFrame {

    private JFrame game;

    public SaveScore(JFrame game) {
        //window settings
        setTitle("Save Score");
        setSize(300, 100);
        setLocationRelativeTo(null);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                returnToMenu();
            }
        });

        //game frame
        this.game = game;

        //panel settings
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);

        //label settings
        JLabel label = new JLabel("Pick your nickname: ");
        label.setOpaque(false);
        label.setForeground(Color.WHITE);
        label.setBackground(Color.BLACK);
        label.setFont(Menu.REXLIA);

        //text field settings
        JTextField nickName = new JTextField(10);
        nickName.setText("");
        nickName.setForeground(Color.WHITE);
        nickName.setBackground(Color.BLACK);
        nickName.setOpaque(false);
        nickName.addActionListener(e -> {
            HighScores.saveHighScore(nickName.getText(), Game.getScore());
            returnToMenu();
        });

        //Save Score
        JButton saveScoreButton = new JButton("Save Score");
        saveScoreButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveScoreButton.setOpaque(false);
        saveScoreButton.setContentAreaFilled(false);
        saveScoreButton.setBorderPainted(false);
        saveScoreButton.setForeground(Color.WHITE);
        saveScoreButton.setBackground(Color.BLACK);
        saveScoreButton.setFont(Menu.REXLIA);
        saveScoreButton.addActionListener(e -> {
            HighScores.saveHighScore(nickName.getText(), Game.getScore());
            returnToMenu();
        });

        panel.add(label);
        panel.add(nickName);
        panel.add(saveScoreButton);
        add(panel);
        setVisible(true);
    }

    private void returnToMenu() {
        if (game != null)
            game.dispose();
        dispose();
        SwingUtilities.invokeLater(
            () -> {
                new Menu();
            }
        );
    }
}
