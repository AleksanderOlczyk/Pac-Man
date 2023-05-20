import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class Menu extends JFrame {

    public static final Font REXLIA; //main manu and high score text font
    public static final Font NAMCO; //high score label font

    static {
        try {
            NAMCO = Font.createFont(Font.TRUETYPE_FONT, new File("assets\\fonts\\namco.ttf")).deriveFont(12f);
            REXLIA = Font.createFont(Font.TRUETYPE_FONT, new File("assets\\fonts\\rexlia.otf")).deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(NAMCO);
            ge.registerFont(REXLIA);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JFrame highScore, newGameSettings;

    public Menu() {
        //window settings
        setTitle("Pac-Man");
        setSize(405, 405);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        //panel
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        add(menuPanel);

        //buttons
        JButton newGameButton = new JButton("New Game");
        newGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGameButton.setOpaque(false);
        newGameButton.setContentAreaFilled(false);
        newGameButton.setBorderPainted(false);
        newGameButton.setForeground(Color.BLACK);
        newGameButton.setFont(Menu.REXLIA);
        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startNewGame();
            }
        });
        menuPanel.add(Box.createVerticalStrut(167));
        menuPanel.add(newGameButton);

        JButton highScoresButton = new JButton("High Scores");
        highScoresButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        highScoresButton.setOpaque(false);
        highScoresButton.setContentAreaFilled(false);
        highScoresButton.setBorderPainted(false);
        highScoresButton.setForeground(Color.BLACK);
        highScoresButton.setFont(Menu.REXLIA);
        highScoresButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHighScores();
            }
        });
        menuPanel.add(Box.createVerticalStrut(7));
        menuPanel.add(highScoresButton);

        JButton exitButton = new JButton("Exit");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setOpaque(false);
        exitButton.setContentAreaFilled(false);
        exitButton.setBorderPainted(false);
        exitButton.setForeground(Color.BLACK);
        exitButton.setFont(Menu.REXLIA);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitGame();
            }
        });
        menuPanel.add(Box.createVerticalStrut(8));
        menuPanel.add(exitButton);

        //background
        AnimatedBackgroundPanel backgroundPanel = new AnimatedBackgroundPanel();
        backgroundPanel.setLayout(new BoxLayout(backgroundPanel, BoxLayout.Y_AXIS));
        setContentPane(backgroundPanel);
        backgroundPanel.add(Box.createVerticalGlue());
        backgroundPanel.add(menuPanel);
        backgroundPanel.add(Box.createVerticalGlue());
        backgroundPanel.setBorder(BorderFactory.createLineBorder(new Color(138, 119, 66)));


        //test
//        for (int i = 0; i < 10; i++) {
//            HighScores.saveHighScore("Test" + i, (int)(Math.random() * 10 + 100));
//        }

        setVisible(true);
    }

    private void startNewGame() {
        if (highScore != null)
            highScore.dispose();
        if (newGameSettings == null) {
            SwingUtilities.invokeLater(
                    () -> {
                        newGameSettings = new GameSettings(this);
                    }
            );
        } else  {
            newGameSettings.dispose();
            SwingUtilities.invokeLater(
                    () -> {
                        newGameSettings = new GameSettings(this);
                    }
            );
        }
    }

    private void showHighScores() {
        if (newGameSettings != null)
            newGameSettings.dispose();
        if (highScore == null) {
            SwingUtilities.invokeLater(
                    () -> {
                        highScore = new HighScores();
                    }
            );
        } else  {
            highScore.dispose();
            SwingUtilities.invokeLater(
                    () -> {
                        highScore = new HighScores();
                    }
            );
        }
    }

    private void exitGame() {
        System.exit(0);
    }
}
