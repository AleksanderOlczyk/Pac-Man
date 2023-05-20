import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HighScores extends JFrame {

    protected JList<String> playersJList;
    protected JList<Integer> scoresJList;
    protected JPanel panel;

    public HighScores() {
        //window settings
        setTitle("High Scores");
        setSize(300, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setVisible(true);

        //panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(0, 0, 0, 0));
        setContentPane(mainPanel);

        //loading high scores
        loadHighScores();

        //create header panel with "Name" and "Score" labels
        JPanel headerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JLabel nameLabel = new JLabel("name", SwingConstants.CENTER);
        JLabel scoreLabel = new JLabel("score", SwingConstants.CENTER);

        //fonts
        nameLabel.setFont(Menu.NAMCO);
        scoreLabel.setFont(Menu.NAMCO);

        //colors
        nameLabel.setForeground(Color.WHITE);
        scoreLabel.setForeground(Color.WHITE);

        //background
        headerPanel.setBackground(Color.BLACK);
        headerPanel.add(nameLabel);
        headerPanel.add(scoreLabel);

        //empty space on top
        Border emptyBorder = BorderFactory.createEmptyBorder(10, 0, 0, 0);
        headerPanel.setBorder(emptyBorder);

        //create a JScrollPane for the panel
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        //return button
        JButton returnButton = new JButton("Return");
        returnButton.setForeground(Color.BLACK);
        returnButton.setBackground(new Color(0, 116, 255));
        returnButton.setBorderPainted(false);
        returnButton.setFocusPainted(false);
        returnButton.setFont(Menu.REXLIA);
        returnButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        mainPanel.add(returnButton, BorderLayout.SOUTH);
    }

    public static List<PlayerResult> getScoresBP() {
        List<PlayerResult> highScores = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("assets\\scores\\scores.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(" ");
                if (tokens.length == 2) {
                    String playerName = tokens[0];
                    int score = Integer.parseInt(tokens[1]);
                    PlayerResult playerResult = new PlayerResult(playerName, score);
                    highScores.add(playerResult);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading high scores: " + e.getMessage());
        }

        return highScores;

        //getting serializable data
        /*List<PlayerResult> highScores = new ArrayList<>();

        if (highScores.isEmpty()) {
            return highScores;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("assets\\scores\\scores.dat"))) {
            highScores = (List<PlayerResult>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error getting  high scores: " + e.getMessage());
        }

        return highScores;*/
    }

    public static void saveHighScoreBP(String playerName, int score) {
        List<PlayerResult> highScores = getScores();
        highScores.add(new PlayerResult(playerName, score));

        //sort high scores in descending order
        highScores.sort(Comparator.comparing(PlayerResult::getScore).reversed());

        // Write updated high scores to file
        StringBuilder sb = new StringBuilder();
        for (PlayerResult result : highScores) {
            sb.append(result.getName())
                    .append(" ")
                    .append(result.getScore())
                    .append("\n");
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("assets\\scores\\scores.txt"))) {
            bw.write(sb.toString());
        } catch (IOException e) {
            System.out.println("Error saving high scores: " + e.getMessage());
        }

        //saving serializable data
        /*List<PlayerResult> highScores = getScores();
        highScores.add(new PlayerResult(playerName, score));

        //sort high scores in descending order
        highScores.sort(Comparator.comparing(PlayerResult::getScore).reversed());

        // Write updated high scores to file
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("assets\\scores\\scores.dat"))) {
            oos.writeObject(highScores);
        } catch (IOException e) {
            System.out.println("Error saving high scores: " + e.getMessage());
        }*/
    }

    public static void saveHighScore(String playerName, int score) {
        List<PlayerResult> highScores = getScores();
        highScores.add(new PlayerResult(playerName, score));

        //sort high scores in descending order
        highScores.sort(Comparator.comparing(PlayerResult::getScore).reversed());

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("assets\\scores\\scores.dat"))) {
            oos.writeObject(highScores);
        } catch (IOException e) {
            System.out.println("Error saving high scores: " + e.getMessage());
        }
    }
    public static List<PlayerResult> getScores() {
        List<PlayerResult> highScores = new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("assets\\scores\\scores.dat"))) {
            highScores = (List<PlayerResult>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading high scores: " + e.getMessage());
        }

        return highScores;
    }

    public static void deleteHighScore(String playerName) {
        List<PlayerResult> highScores = getScores();
        highScores.removeIf(playerResult -> playerResult.getName().equals(playerName));
        saveScores(highScores);
    }

    public void loadHighScores() {
        List<PlayerResult> highScores = getScores();

        DefaultListModel<String> playersList = new DefaultListModel<>();
        DefaultListModel<Integer> scoresList = new DefaultListModel<>();
        int rank = 1;
        for (PlayerResult result : highScores) {
            playersList.addElement(rank++ + ". " + result.getName());
            scoresList.addElement(result.getScore());
        }

        //create score lists
        this.playersJList = new JList<>(playersList);
        this.scoresJList = new JList<>(scoresList);

        //aligment to right
        scoresJList.setCellRenderer(new DefaultListCellRenderer() {
            {setHorizontalAlignment(SwingConstants.RIGHT);}
        });

        //set font for both lists
        playersJList.setFont(Menu.REXLIA);
        scoresJList.setFont(Menu.REXLIA);

        //set white font
        playersJList.setForeground(Color.WHITE);
        scoresJList.setForeground(Color.WHITE);

        //create a JPanel to hold both lists side by side
        this.panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //background settings
        panel.setBackground(Color.BLACK);
        playersJList.setBackground(Color.BLACK);
        scoresJList.setBackground(Color.BLACK);

        //add each list to the panel
        panel.add(playersJList);
        panel.add(scoresJList);

        //create a JScrollPane for the panel
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        panel.add(playersJList);
        panel.add(scoresJList);
    }

    public static void saveScores(List<PlayerResult> highScores) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("assets\\scores\\scores.dat"));
            outputStream.writeObject(highScores);
            outputStream.close();
        } catch (Exception e) {
            new Error("Error saving high scores: " + e.getMessage());
        }
    }
}
