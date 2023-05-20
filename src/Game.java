import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Game extends JFrame {

    private final int MAX_GHOSTS = 10, MAX_LIFES = 5;

    private JTable table;
    private JLabel scoreLabel;
    private JPanel livesPanel;
    private KeyListener keyListener;

    private int blockSize = 32;
    private final int mapWidth, mapHeight;
    private int[][] levelData;
    /*
        0 - empty
        1 - obstacle
        2 - points
        4 - pacman resp
        8 - ghost resp
        16 - pacman
        32 - ghosts

        64 - bonus points
        128 - points multiplier
        256 - bonus life
        512 - slow down ghosts
        1024 - freeze ghosts
    */
    private int[][] levelDataBackUp;
    private Direction pacmanDirection;

    private int ghostNum;
    private volatile int pacmanSpeed, ghostSpeed;
    private static int score, level;
    private int lifes;
    private int bonusPoints, pointsMultiplier;
    private int pacmanX, pacmanY;
    private int pacmanXBackUp, pacmanYBackUp;

    private Thread[] ghostThreads;
    private Semaphore[] ghostSemaphores;
    private volatile boolean ghostMoving;


    private ImageIcon ghostResp, pacmanResp;
    private ImageIcon ghostRight, ghostRight1;
    private ImageIcon up, up1, up2, up3, down, down1, down2, down3, left, left1, left2, left3 , right, right1, right2, right3;
    private ImageIcon life, scaledLife, point;
    private ImageIcon bonus, heart, multiplier, freeze, slowdown;

    public Game(int mapHeight, int mapWidth) {
        //window settings
        setTitle("Pac-Man");

        int screenWidth = mapWidth * blockSize;
        int screenHeight = mapHeight * blockSize;

        setSize(screenWidth, screenHeight + 100);
        setLocationRelativeTo(null);
        setResizable(true);
        getContentPane().setBackground(new Color(0, 23, 178));

        keyListener = new CustomKeyListener();
        addKeyListener(keyListener);
        setFocusable(true);
        requestFocus();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                returnToMenu();
            }
        });

        //initialize variable
        this.mapHeight = mapHeight;
        this.mapWidth = mapWidth;
        loadImages();
        loadGhosts();
        pacmanDirection = Direction.RIGHT;
        this.ghostSemaphores  = new Semaphore[ghostNum];
        this.ghostThreads = new Thread[ghostNum];
        this.isMoving = false;
        this.ghostMoving = true;
        this.pacmanSpeed = 300;
        this.ghostSpeed = 300;
        this.score = 0;
        this.level = 1;
        this.lifes = 3;
        this.pointsMultiplier = 1;
        this.levelData = genMap(preGenMap());
        ghostsUpdate();
        this.levelDataBackUp = new int[levelData.length][levelData[0].length];
        for (int row = 0; row < levelDataBackUp.length; row++) {
            System.arraycopy(levelData[row], 0, levelDataBackUp[row], 0, levelDataBackUp[row].length);
        }

        //timer
        JLabel timerLabel = new JLabel("00:00");
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setFont(Menu.REXLIA);
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        //table settings
        this.table = new MyJTable(new MapTableModel(levelData));
        table.setDefaultRenderer(Integer.class, new MapTableCellRenderer());
        table.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
        table.setOpaque(true);
        table.setShowGrid(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAlignmentX(JLabel.CENTER);
        table.setAlignmentY(JLabel.CENTER);
        table.setEnabled(false);
        table.setBackground(new Color(0, 23, 178));

        Dimension tableSize = new Dimension(screenWidth, screenHeight);
        table.setPreferredSize(tableSize);

        JPanel livesAndScorePanel = new JPanel(new BorderLayout());
        livesAndScorePanel.setOpaque(false);

        //create lives panel
        this.livesPanel = new JPanel();
        livesPanel.setOpaque(false);
        livesPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        livesPanel.setBackground(new Color(0, 23, 178));
        loadLifes();

        //create score panel
        JPanel scorePanel = new JPanel();
        scorePanel.setOpaque(false);
        scorePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        scorePanel.setBackground(new Color(0, 23, 178));

        this.scoreLabel = new JLabel("Score: " + score);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(Menu.REXLIA);
        scorePanel.add(scoreLabel);

        livesAndScorePanel.add(livesPanel, BorderLayout.WEST);
        livesAndScorePanel.add(scorePanel, BorderLayout.EAST);


        // Use GridBagLayout to center and resize the table
        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        tablePanel.add(table, gbc);

        add(timerLabel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(livesAndScorePanel, BorderLayout.SOUTH);

        setVisible(true);
        startTimer(timerLabel);
        startMovingGhosts();
        updatePacmanAnimation();
        updateGhostAnimation();

        for (int j = 0; j < table.getColumnCount(); j++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(j);
            MapTableCellRenderer cellRenderer = new MapTableCellRenderer();
            tableColumn.setCellRenderer(cellRenderer);
        }
    }

    public void doublePoints() {
        pointsMultiplier = 2;

        long lastActionTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= 3000 && Math.random() < 0.25) {
            pointsMultiplier = 1;
        }
    }

    public void bonusPoints() {
        score += bonusPoints;
    }

    public void bonusLife() {
        if (lifes < MAX_LIFES) {
            lifes++;
            loadLifes();
        }
    }

    public void freezeGhosts() {
        stopMovingGhosts();

        long lastActionTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= 2000 && Math.random() < 0.25) {
            resumeMovingGhosts();
        }
    }

    public void slowDownGhosts() {
        ghostSpeed = 400;

        long lastActionTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= 2000 && Math.random() < 0.25) {
            ghostSpeed = 300;
        }
    }

    public void speedUpPacman() {
        pacmanSpeed = 200;

        long lastActionTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime >= 5000 && Math.random() < 0.25) {
            pacmanSpeed = 300;
        }
    }

    private void returnToMenu() {
        dispose();
        SwingUtilities.invokeLater(
            () -> {
                new Menu();
            }
        );
    }

    private void loadImages() {
        //misc
        point = new ImageIcon("assets\\images\\point.png");
        pacmanResp = new ImageIcon("assets\\images\\pacman\\resp.png");
        ghostResp = new ImageIcon("assets\\images\\ghost\\resp.png");

        //gui
        life = new ImageIcon("assets\\images\\interface\\life.png");
        scaledLife = new ImageIcon(life.getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT));

        //ghosts
        ghostRight = new ImageIcon("assets\\images\\ghost\\move\\right\\0.png");
        ghostRight1 = new ImageIcon("assets\\images\\ghost\\move\\right\\1.png");

        //pacman
        up = new ImageIcon("assets\\images\\pacman\\up\\up3.png");
        up1 = new ImageIcon("assets\\images\\pacman\\up\\up2.png");
        up2 = new ImageIcon("assets\\images\\pacman\\up\\up1.png");
        up3 = new ImageIcon("assets\\images\\pacman\\up\\up0.png");

        down = new ImageIcon("assets\\images\\pacman\\down\\down3.png");
        down1 = new ImageIcon("assets\\images\\pacman\\down\\down2.png");
        down2 = new ImageIcon("assets\\images\\pacman\\down\\down1.png");
        down3 = new ImageIcon("assets\\images\\pacman\\down\\down0.png");

        left = new ImageIcon("assets\\images\\pacman\\left\\left3.png");
        left1 = new ImageIcon("assets\\images\\pacman\\left\\left2.png");
        left2 = new ImageIcon("assets\\images\\pacman\\left\\left1.png");
        left3 = new ImageIcon("assets\\images\\pacman\\left\\left0.png");

        right = new ImageIcon("assets\\images\\pacman\\right\\right3.png");
        right1 = new ImageIcon("assets\\images\\pacman\\right\\right2.png");
        right2 = new ImageIcon("assets\\images\\pacman\\right\\right1.png");
        right3 = new ImageIcon("assets\\images\\pacman\\right\\right0.png");

        bonus = new ImageIcon("assets\\images\\perks\\bonus.png");
        multiplier = new ImageIcon("assets\\images\\perks\\multiplier.png");
        heart = new ImageIcon("assets\\images\\perks\\heart.png");
        slowdown = new ImageIcon("assets\\images\\perks\\slowdown.png");
        freeze = new ImageIcon("assets\\images\\perks\\freeze.png");
    }

    public boolean checkLevel() {
        int rowCount = table.getRowCount();
        int colCount = table.getColumnCount();

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < colCount; col++) {
                int cellValue = (int) table.getValueAt(row, col);
                if (cellValue == 2) {
                    return false;
                }
            }
        }
        return true;
    }

    public void nextLevel() {
        System.out.println("Next Level " + ++level);
        System.out.println("Score: " + score);
        for (int row = 0; row < levelData.length; row++) {
            System.arraycopy(levelDataBackUp[row], 0, levelData[row], 0, levelData[row].length);
        }

        MapTableModel model = (MapTableModel) table.getModel();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();

        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < colCount; col++) {
                int cellValue = levelData[row][col];
                model.setValueAt(cellValue, row, col);
            }
        }

        pacmanX = pacmanXBackUp;
        pacmanY = pacmanYBackUp;

        if (lifes < MAX_LIFES) {
            lifes++;
            loadLifes();
        }

        if (ghostNum < MAX_GHOSTS) {
            ghostNum++;
        }

        bonusPoints += 100;

        stopMovingGhosts();
        this.ghostSemaphores  = new Semaphore[ghostNum];
        this.ghostThreads = new Thread[ghostNum];
        ghostsUpdate();
        startMovingGhosts();

        table.revalidate();
        table.repaint();
    }

    private void ghostsUpdate() {
        for (int i = 0; i < ghostNum; i++) {
            this.levelData[levelData.length / 2][levelData[0].length / 2] += 32;
        }
    }

    private void loadLifes() {
        livesPanel.removeAll();
        for (int i = 0; i < lifes; i++) {
            JLabel lifeLabel = new JLabel(scaledLife);
            livesPanel.add(lifeLabel);
        }

        livesPanel.add(Box.createVerticalGlue());
        livesPanel.revalidate();
    }

    private void startTimer(JLabel timerLabel) {
        Thread timerThread = new Thread(() -> {
            int time = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    time++;
                    int minutes = time / 60;
                    int seconds = time % 60;
                    String formattedTime = String.format("%02d:%02d", minutes, seconds);
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText(formattedTime);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        timerThread.start();
    }

    private int[][] preGenMap(){
        int[][] preGenMap = new int[mapHeight][mapWidth];
        for (int i = 0; i < preGenMap.length; i++) {
            for (int j = 0; j < preGenMap[0].length; j++) {
                preGenMap[i][j] = 1; //wall
            }
        }
        return preGenMap;
    }

    public int[][] genMap(int[][] mapArr) {
        Random random = new Random();

        int startX = random.nextInt(mapArr[0].length / 2);
        int startY = random.nextInt(mapArr.length / 2);

        generateMazeRecursive(mapArr, startX, startY);
        myMazeGrid(mapArr);
        randomPacmanResp(mapArr);
        return mapArr;
    }

    private void generateMazeRecursive(int[][] mapArr, int x, int y) {
        mapArr[y][x] = 2;

        int[] directions = { 1, 2, 3, 4 }; // 1: up, 2: right, 3: down, 4: left
        shuffleArray(directions);

        for (int direction : directions) {
            int newX = x;
            int newY = y;

            switch (direction) {
                case 1 -> newY -= 2;
                case 2 -> newX += 2;
                case 3 -> newY += 2;
                case 4 -> newX -= 2;
            }

            if (newX > 0 && newX < mapArr[0].length && newY > 0 && newY < mapArr.length && mapArr[newY][newX] == 1) {
                mapArr[newY][newX] = 2;
                mapArr[y + (newY - y) / 2][x + (newX - x) / 2] = 2;
                generateMazeRecursive(mapArr, newX, newY);
            }
        }
    }

    private void shuffleArray(int[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    private void myMazeGrid(int[][] mapArr) {
        for (int i = 0; i < mapArr.length; i++) {
            for (int j = 0; j < mapArr[0].length; j++) {
                if (i == 0 || j == 0 || i == mapArr.length - 1 || j == mapArr[0].length - 1
                    || i == mapArr.length / 2  || j == mapArr[0].length / 2)
                    mapArr[i][j] = 2;  //points
                if (i == mapArr.length / 2 && j == mapArr[0].length / 2)
                    mapArr[i][j] = 8; //ghost resp
            }
        }
    }

    private void randomPacmanResp(int[][] mapArr) {
        Random rand = new Random();
        boolean random = rand.nextBoolean();
        if (random) {
            pacmanX = rand.nextInt(mapArr[0].length - 1);
            pacmanY = rand.nextInt(2) == 0 ? 0 : mapArr.length - 1;
            mapArr[pacmanY][pacmanX] = 20;
        } else {
            pacmanX = rand.nextInt(2) == 0 ? 0 : mapArr.length - 1;
            pacmanY = rand.nextInt(mapArr[0].length - 1);
            mapArr[pacmanY][pacmanX] = 20;
        }

        pacmanXBackUp = pacmanX;
        pacmanYBackUp = pacmanY;
    }

    private void loadGhosts() {
        if (mapHeight * mapWidth < 15 * 15) {
            this.ghostNum = 2;
            this.bonusPoints = 125;
        } else if (mapHeight * mapWidth < 25 * 25) {
            this.ghostNum = 3;
            this.bonusPoints = 250;
        } else if (mapHeight * mapWidth < 50 * 50) {
            this.ghostNum = 4;
            this.bonusPoints = 500;
        } else if (mapHeight * mapWidth < 75 * 75) {
            this.ghostNum = 5;
            this.bonusPoints = 750;
        } else {
            this.ghostNum = 6;
            this.bonusPoints = 1000;
        }
        System.out.println("Ghost num " + ghostNum);
    }

    public void pacmanDead() {
        lifes--;
        loadLifes();
        if (lifes <= 0) {
            new SaveScore(this);
            removeKeyListener(keyListener);
            stopMovingGhosts();
            stopMovingPacman();
        }

        levelData[pacmanY][pacmanX] = levelData[pacmanY][pacmanX] == 48 ? 32 : 0;
        pacmanX = pacmanXBackUp;
        pacmanY = pacmanYBackUp;
        levelData[pacmanY][pacmanX] += 16;

        table.revalidate();
        table.repaint();
    }

    public static int getScore() {
        return score;
    }

    private class MapTableModel extends AbstractTableModel {
        private final int[][] data;

        public MapTableModel(int[][] data) {
            this.data = data;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return data[0].length;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = (int) value;
            fireTableCellUpdated(row, col);
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return Integer.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private class MapTableCellRenderer extends DefaultTableCellRenderer {

        //misc
        private ImageIcon scaledPointIcon, scaledPointIcon1, scaledPacmanRespIcon, scaledGhostRespIcon;

        //pacman
        private ImageIcon scaledUpIcon, scaledUpIcon1, scaledUpIcon2, scaledUpIcon3;
        private ImageIcon scaledDownIcon, scaledDownIcon1, scaledDownIcon2, scaledDownIcon3;
        private ImageIcon scaledLeftIcon, scaledLeftIcon1, scaledLeftIcon2, scaledLeftIcon3;
        private ImageIcon scaledRightIcon, scaledRightIcon1, scaledRightIcon2, scaledRightIcon3;

        //ghost
        private ImageIcon scaledGhostUp, scaledGhostUp1;
        private ImageIcon scaledGhostDown, scaledGhostDown1;
        private ImageIcon scaledGhostLeft, scaledGhostLeft1;
        private ImageIcon scaledGhostRight, scaledGhostRight1;

        //perks
        private ImageIcon scaledBonusIcon, scaledMultiplierIcon, scaledHeartIcon, scaledSlowdownIcon, scaledFreezeIcon;

        public MapTableCellRenderer() {
            Dimension cellSize = table.getCellRect(pacmanY, pacmanX, false).getSize();

            //misc
            scaledPointIcon = scaleImage(point.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledPointIcon1 = scaleImage(point.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledPacmanRespIcon = scaleImage(pacmanResp.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledGhostRespIcon = scaleImage(ghostResp.getImage(), cellSize.width / 2, cellSize.height / 2);

            //ghost
            scaledGhostRight = scaleImage(ghostRight.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledGhostRight1 = scaleImage(ghostRight1.getImage(), cellSize.width / 2, cellSize.height / 2);

            //pacman
            scaledUpIcon = scaleImage(up.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledUpIcon1 = scaleImage(up1.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledUpIcon2 = scaleImage(up2.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledUpIcon3 = scaleImage(up3.getImage(), cellSize.width / 2, cellSize.height / 2);

            scaledDownIcon = scaleImage(down.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledDownIcon1 = scaleImage(down1.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledDownIcon2 = scaleImage(down2.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledDownIcon3 = scaleImage(down3.getImage(), cellSize.width / 2, cellSize.height / 2);

            scaledLeftIcon = scaleImage(left.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledLeftIcon1 = scaleImage(left1.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledLeftIcon2 = scaleImage(left2.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledLeftIcon3 = scaleImage(left3.getImage(), cellSize.width / 2, cellSize.height / 2);

            scaledRightIcon = scaleImage(right.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledRightIcon1 = scaleImage(right1.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledRightIcon2 = scaleImage(right2.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledRightIcon3 = scaleImage(right3.getImage(), cellSize.width / 2, cellSize.height / 2);

            //perks
            scaledBonusIcon = scaleImage(bonus.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledMultiplierIcon = scaleImage(multiplier.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledHeartIcon = scaleImage(heart.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledSlowdownIcon = scaleImage(slowdown.getImage(), cellSize.width / 2, cellSize.height / 2);
            scaledFreezeIcon = scaleImage(freeze.getImage(), cellSize.width / 2, cellSize.height / 2);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            c.setBackground(Color.BLACK);
            setText(null);

            switch ((int) value) {
                case 0:
                    c.setBackground(Color.BLACK);
                    ((JLabel)c).setIcon(null);
                    break;
                case 1:
                    c.setBackground(new Color(255, 186, 0));
                    ((JLabel)c).setIcon(null);
                    break;
                case 2:
                    ((JLabel)c).setIcon(getScaledPointIcon());
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 4:
                    ((JLabel)c).setIcon(scaledPacmanRespIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 8:
                    ((JLabel)c).setIcon(scaledGhostRespIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 16, 18, 20, 24,
                    80, 144, 272, 528, 1040:
                    switch (pacmanDirection) {
                        case UP -> ((JLabel) c).setIcon(getPacmanUpIcon());
                        case DOWN -> ((JLabel) c).setIcon(getPacmanDownIcon());
                        case LEFT -> ((JLabel) c).setIcon(getPacmanLeftIcon());
                        case RIGHT -> ((JLabel) c).setIcon(getPacmanRightIcon());
                    }
                    ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 32, 34, 36, 38, 40, 48, 66, 72, 104, 136, 168, 200, 232, 264, 296, 328,
                    96, 160, 288, 544, 1056:
                    ((JLabel)c).setIcon(getGhostRightIcon());
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 64:
                    ((JLabel)c).setIcon(scaledBonusIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 128:
                    ((JLabel)c).setIcon(scaledMultiplierIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 256:
                    ((JLabel)c).setIcon(scaledHeartIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 512:
                    ((JLabel)c).setIcon(scaledSlowdownIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                case 1024:
                    ((JLabel)c).setIcon(scaledFreezeIcon);
                    ((JLabel)c).setHorizontalAlignment(JLabel.CENTER);
                    break;
                default:
                    c.setBackground(Color.RED);
                    System.out.println((int) value);
                    break;
            }
            return c;
        }

        private ImageIcon scaleImage(Image image, int width, int height) {
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }

        public void setPacmanDirection(Direction direction) {
            pacmanDirection = direction;
        }

        private ImageIcon getPacmanUpIcon() {
            return switch (pacmanAnimationState) {
                case 1 -> scaledUpIcon1;
                case 2 -> scaledUpIcon2;
                case 3 -> scaledUpIcon3;
                default -> scaledUpIcon;
            };
        }

        private ImageIcon getPacmanDownIcon() {
            return switch (pacmanAnimationState) {
                case 1 -> scaledDownIcon1;
                case 2 -> scaledDownIcon2;
                case 3 -> scaledDownIcon3;
                default -> scaledDownIcon;
            };
        }

        private ImageIcon getPacmanLeftIcon() {
            return switch (pacmanAnimationState) {
                case 1 -> scaledLeftIcon1;
                case 2 -> scaledLeftIcon2;
                case 3 -> scaledLeftIcon3;
                default -> scaledLeftIcon;
            };
        }

        private ImageIcon getPacmanRightIcon() {
            return switch (pacmanAnimationState) {
                case 1 -> scaledRightIcon1;
                case 2 -> scaledRightIcon2;
                case 3 -> scaledRightIcon3;
                default -> scaledRightIcon;
            };
        }

        private ImageIcon getGhostUpIcon() {
            return ghostAnimationState == 0 ? scaledGhostRight : scaledGhostRight1;
        }

        private ImageIcon getGhostDownIcon() {
            return ghostAnimationState == 0 ? scaledGhostRight : scaledGhostRight1;
        }

        private ImageIcon getGhostLeftIcon() {
            return ghostAnimationState == 0 ? scaledGhostRight : scaledGhostRight1;
        }
        private ImageIcon getGhostRightIcon() {
            return ghostAnimationState == 0 ? scaledGhostRight : scaledGhostRight1;
        }

        private ImageIcon getScaledPointIcon() {
            return pointAnimationState == 0 ? scaledPointIcon : scaledPointIcon1;
        }
    }

    private int pacmanAnimationState = 0;
    private void updatePacmanAnimationState() {
        pacmanAnimationState = (pacmanAnimationState + 1) % 4;
    }

    private void updatePacmanAnimation() {
        Thread pacmanAnim = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000/5);
                    updatePacmanAnimationState();
                    SwingUtilities.invokeLater(() -> {
                        table.repaint();
                        table.revalidate();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        pacmanAnim.start();
    }

    private enum Direction {UP, DOWN, LEFT, RIGHT}
    private volatile int currentKeyCode;

    private class CustomKeyListener implements KeyListener {
        private boolean ctrl;
        private boolean shift;
        private boolean q;

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_CONTROL) {
                ctrl = true;
            } else if (keyCode == KeyEvent.VK_SHIFT) {
                shift = true;
            } else if (keyCode == KeyEvent.VK_Q) {
                q = true;
            }

            if (ctrl && shift && q) {
                returnToMenu();
            }

            if (!isMoving && (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W
                || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S
                || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A
                || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D)
            ) {
                isMoving = true;
                currentKeyCode = keyCode;
                startMovingPacman();
            }

            if ((keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) && (pacmanY - 1 >= 0 && levelData[pacmanY - 1][pacmanX] != 1)) {
                currentKeyCode = keyCode;
            }
            if ((keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) && (pacmanY + 1 < mapHeight && levelData[pacmanY + 1][pacmanX] != 1)) {
                currentKeyCode = keyCode;
            }
            if ((keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) && (pacmanX - 1 >= 0 && levelData[pacmanY][pacmanX - 1] != 1)) {
                currentKeyCode = keyCode;
            }
            if ((keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) && (pacmanX + 1 < mapWidth && levelData[pacmanY][pacmanX + 1] != 1)) {
                currentKeyCode = keyCode;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_CONTROL) {
                ctrl = false;
            } else if (keyCode == KeyEvent.VK_SHIFT) {
                shift = false;
            } else if (keyCode == KeyEvent.VK_Q) {
                q = false;
            }
        }
    }

    public void movePacman(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP, KeyEvent.VK_W:
                if (pacmanY - 1 >= 0 && levelData[pacmanY - 1][pacmanX] != 1) {
                    if (levelData[pacmanY][pacmanX] == 20 || levelData[pacmanY][pacmanX] == 24 || levelData[pacmanY][pacmanX] == 16)
                        levelData[pacmanY][pacmanX] -= 16;
                    pacmanY--;
                    if (levelData[pacmanY][pacmanX] == 2) {
                        levelData[pacmanY][pacmanX] = 16;
                        score += pointsMultiplier;
                        scoreLabel.setText("Score: " + score);
                        if (checkLevel())
                            nextLevel();
                    }
                    if (levelData[pacmanY][pacmanX] == 64) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusPoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 128) {
                        levelData[pacmanY][pacmanX] = 16;
                        doublePoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 256) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusLife();
                    }
                    if (levelData[pacmanY][pacmanX] == 512) {
                        levelData[pacmanY][pacmanX] = 16;
                        slowDownGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 1024) {
                        levelData[pacmanY][pacmanX] = 16;
                        freezeGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 4 || levelData[pacmanY][pacmanX] == 8 || levelData[pacmanY][pacmanX] == 0)
                        levelData[pacmanY][pacmanX] += 16;
                    moveP(pacmanY, pacmanX, Direction.UP);
                }
                break;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S:
                if (pacmanY + 1 < mapHeight && levelData[pacmanY + 1][pacmanX] != 1) {
                    if (levelData[pacmanY][pacmanX] == 20 || levelData[pacmanY][pacmanX] == 24 || levelData[pacmanY][pacmanX] == 16)
                        levelData[pacmanY][pacmanX] -= 16;
                    pacmanY++;
                    if (levelData[pacmanY][pacmanX] == 2) {
                        levelData[pacmanY][pacmanX] = 16;
                        score += pointsMultiplier;
                        scoreLabel.setText("Score: " + score);
                        if (checkLevel())
                            nextLevel();
                    }
                    if (levelData[pacmanY][pacmanX] == 64) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusPoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 128) {
                        levelData[pacmanY][pacmanX] = 16;
                        doublePoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 256) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusLife();
                    }
                    if (levelData[pacmanY][pacmanX] == 512) {
                        levelData[pacmanY][pacmanX] = 16;
                        slowDownGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 1024) {
                        levelData[pacmanY][pacmanX] = 16;
                        freezeGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 4 || levelData[pacmanY][pacmanX] == 8 || levelData[pacmanY][pacmanX] == 0)
                        levelData[pacmanY][pacmanX] += 16;
                    moveP(pacmanY, pacmanX, Direction.DOWN);
                }
                break;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A:
                if (pacmanX - 1 >= 0 && levelData[pacmanY][pacmanX - 1] != 1) {
                    if (levelData[pacmanY][pacmanX] == 20 || levelData[pacmanY][pacmanX] == 24 || levelData[pacmanY][pacmanX] == 16)
                        levelData[pacmanY][pacmanX] -= 16;
                    pacmanX--;
                    if (levelData[pacmanY][pacmanX] == 2) {
                        levelData[pacmanY][pacmanX] = 16;
                        score += pointsMultiplier;
                        scoreLabel.setText("Score: " + score);
                        if (checkLevel())
                            nextLevel();
                    }
                    if (levelData[pacmanY][pacmanX] == 64) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusPoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 128) {
                        levelData[pacmanY][pacmanX] = 16;
                        doublePoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 256) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusLife();
                    }
                    if (levelData[pacmanY][pacmanX] == 512) {
                        levelData[pacmanY][pacmanX] = 16;
                        slowDownGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 1024) {
                        levelData[pacmanY][pacmanX] = 16;
                        freezeGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 4 || levelData[pacmanY][pacmanX] == 8 || levelData[pacmanY][pacmanX] == 0)
                        levelData[pacmanY][pacmanX] += 16;
                    moveP(pacmanY, pacmanX, Direction.LEFT);
                }
                break;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D:
                if (pacmanX + 1 < mapWidth && levelData[pacmanY][pacmanX + 1] != 1) {
                    if (levelData[pacmanY][pacmanX] == 20 || levelData[pacmanY][pacmanX] == 24 || levelData[pacmanY][pacmanX] == 16)
                        levelData[pacmanY][pacmanX] -= 16;
                    pacmanX++;
                    if (levelData[pacmanY][pacmanX] == 2) {
                        levelData[pacmanY][pacmanX] = 16;
                        score += pointsMultiplier;
                        scoreLabel.setText("Score: " + score);
                        if (checkLevel())
                            nextLevel();
                    }
                    if (levelData[pacmanY][pacmanX] == 64) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusPoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 128) {
                        levelData[pacmanY][pacmanX] = 16;
                        doublePoints();
                    }
                    if (levelData[pacmanY][pacmanX] == 256) {
                        levelData[pacmanY][pacmanX] = 16;
                        bonusLife();
                    }
                    if (levelData[pacmanY][pacmanX] == 512) {
                        levelData[pacmanY][pacmanX] = 16;
                        slowDownGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 1024) {
                        levelData[pacmanY][pacmanX] = 16;
                        freezeGhosts();
                    }
                    if (levelData[pacmanY][pacmanX] == 4 || levelData[pacmanY][pacmanX] == 8 || levelData[pacmanY][pacmanX] == 0)
                        levelData[pacmanY][pacmanX] += 16;
                    moveP(pacmanY, pacmanX, Direction.RIGHT);
                }
                break;
        }
    }

    public void moveP(int row, int column, Direction direction) {
        if (row >= 0 && row < table.getRowCount() && column >= 0 && column < table.getColumnCount()) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            MapTableCellRenderer cellRenderer = new MapTableCellRenderer();

            cellRenderer.setPacmanDirection(direction);
            tableColumn.setCellRenderer(cellRenderer);
        }
    }

    private Semaphore semaphore = new Semaphore(1);
    private volatile boolean isMoving;

    public void startMovingPacman() {
        Thread moveThread = new Thread(() -> {
            while (isMoving) {
                try {
                    table.repaint();
                    table.revalidate();

                    semaphore.acquire();
                    movePacman(currentKeyCode);
                    semaphore.release();

                    Thread.sleep(pacmanSpeed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        moveThread.start();
    }

    public void stopMovingPacman() {
        isMoving = false;
        semaphore.release();
    }

    public int[] moveGhost(int ghostX, int ghostY) {
        List<Integer> possibleDirections = new ArrayList<>();

        if (ghostY - 1 >= 0 && levelData[ghostY - 1][ghostX] != 1 && levelData[ghostY - 1][ghostX] < 32) {
            possibleDirections.add(0);
        }
        if (ghostY + 1 < levelData.length && levelData[ghostY + 1][ghostX] != 1 && levelData[ghostY + 1][ghostX] < 32) {
            possibleDirections.add(1);
        }
        if (ghostX - 1 >= 0 && levelData[ghostY][ghostX - 1] != 1 && levelData[ghostY][ghostX - 1] < 32) {
            possibleDirections.add(2);
        }
        if (ghostX + 1 < levelData[ghostY].length && levelData[ghostY][ghostX + 1] != 1 && levelData[ghostY][ghostX + 1] < 32) {
            possibleDirections.add(3);
        }

        int mGhost;
        if (!possibleDirections.isEmpty()) {
            Random random = new Random();
            mGhost = possibleDirections.get(random.nextInt(possibleDirections.size()));

            switch (mGhost) {
                case 0:
                    if ((levelData[ghostY][ghostX] > 32 && levelData[ghostY][ghostX] < 74)
                        || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                        || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024)
                        levelData[ghostY][ghostX] -= 32;
                    if (levelData[ghostY][ghostX] == 32)
                        levelData[ghostY][ghostX] = 0;
                    ghostY--;
                    if (levelData[ghostY][ghostX] == 16 || levelData[ghostY][ghostX] == 18 || levelData[ghostY][ghostX] == 20 || levelData[ghostY][ghostX] == 24) {
                        pacmanDead();
                    } else if ((levelData[ghostY][ghostX] < 32 && levelData[ghostY][ghostX] != 20)
                        || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                        || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024
                    )
                        levelData[ghostY][ghostX] += 32;
                    break;
                case 1:
                    if ((levelData[ghostY][ghostX] > 32 && levelData[ghostY][ghostX] < 74)
                            || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                            || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024)
                        levelData[ghostY][ghostX] -= 32;
                    if (levelData[ghostY][ghostX] == 32)
                        levelData[ghostY][ghostX] = 0;
                    ghostY++;
                    if (levelData[ghostY][ghostX] == 16 || levelData[ghostY][ghostX] == 18 || levelData[ghostY][ghostX] == 20 || levelData[ghostY][ghostX] == 24) {
                        levelData[ghostY][ghostX] += 32;
                        pacmanDead();
                    } else if ((levelData[ghostY][ghostX] < 32 && levelData[ghostY][ghostX] != 20)
                            || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                            || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024
                    )
                        levelData[ghostY][ghostX] += 32;
                    break;
                case 2:
                    if ((levelData[ghostY][ghostX] > 32 && levelData[ghostY][ghostX] < 74)
                            || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                            || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024)
                        levelData[ghostY][ghostX] -= 32;
                    if (levelData[ghostY][ghostX] == 32)
                        levelData[ghostY][ghostX] = 0;
                    ghostX--;
                    if (levelData[ghostY][ghostX] == 16 || levelData[ghostY][ghostX] == 18 || levelData[ghostY][ghostX] == 20 || levelData[ghostY][ghostX] == 24) {
                        levelData[ghostY][ghostX] += 32;
                        pacmanDead();
                    } else if ((levelData[ghostY][ghostX] < 32 && levelData[ghostY][ghostX] != 20)
                            || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                            || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024
                    )
                        levelData[ghostY][ghostX] += 32;
                    break;
                case 3:
                    if ((levelData[ghostY][ghostX] > 32 && levelData[ghostY][ghostX] < 74)
                            || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                            || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024)
                        levelData[ghostY][ghostX] -= 32;
                    if (levelData[ghostY][ghostX] == 32)
                        levelData[ghostY][ghostX] = 0;
                    ghostX++;
                    if (levelData[ghostY][ghostX] == 16 || levelData[ghostY][ghostX] == 18 || levelData[ghostY][ghostX] == 20 || levelData[ghostY][ghostX] == 24) {
                        levelData[ghostY][ghostX] += 32;
                        pacmanDead();
                    } else if ((levelData[ghostY][ghostX] < 32 && levelData[ghostY][ghostX] != 20)
                            || levelData[ghostY][ghostX] == 64 || levelData[ghostY][ghostX] == 128
                            || levelData[ghostY][ghostX] == 256 || levelData[ghostY][ghostX] == 512 || levelData[ghostY][ghostX] == 1024
                    )
                        levelData[ghostY][ghostX] += 32;
                    break;
            }
        }
        table.revalidate();
        table.repaint();
        return new int[]{ghostX, ghostY};
    }

    public void startMovingGhosts() {
        for (int i = 0; i < ghostNum; i++) {
            Semaphore semaphore = new Semaphore(1);
            ghostSemaphores[i] = semaphore;

            Thread ghostThread = new Thread(() -> {
                int ghostX = levelData[0].length / 2;
                int ghostY = levelData.length / 2;
                long lastActionTime = System.currentTimeMillis();

                while (ghostMoving) {
                    try {
                        int ghostXLast = ghostX;
                        int ghostYLast = ghostY;

                        semaphore.acquire();
                        int[] newGhostPosition = moveGhost(ghostX, ghostY);
                        ghostX = newGhostPosition[0];
                        ghostY = newGhostPosition[1];
                        semaphore.release();

                        Thread.sleep(ghostSpeed);

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastActionTime >= 5000 && Math.random() < 0.25) {
                            if (levelData[ghostY][ghostX] != 4 || levelData[ghostY][ghostX] != 8)
                                doRandomAction(ghostXLast, ghostYLast);
                            lastActionTime = currentTime;
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            ghostThreads[i] = ghostThread;
            ghostThread.start();
        }
    }

    public void doRandomAction(int ghostX, int ghostY) {
        Random random = new Random();
        switch (random.nextInt(5)) {
            case 0 -> levelData[ghostY][ghostX] = 64;
            case 1 -> levelData[ghostY][ghostX] = 128;
            case 2 -> levelData[ghostY][ghostX] = 256;
            case 3 -> levelData[ghostY][ghostX] = 512;
            case 4 -> levelData[ghostY][ghostX] = 1024;
        }
        table.revalidate();
        table.repaint();
    }

    public void stopMovingGhosts() {
        for (Semaphore semaphore : ghostSemaphores) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void resumeMovingGhosts() {
        for (Semaphore semaphore : ghostSemaphores) {
            semaphore.release();
        }
    }

    private int ghostAnimationState = 0;
    private void updateGhostAnimationState() {
        ghostAnimationState = (ghostAnimationState + 1) % 2;
    }

    private void updateGhostAnimation() {
        Thread ghostAnim = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000/5);
                    updateGhostAnimationState();
                    SwingUtilities.invokeLater(() -> {
                        table.repaint();
                        table.revalidate();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ghostAnim.start();
    }

    private int pointAnimationState = 0;
    private void updatePointAnimationState() {
        pointAnimationState = (pointAnimationState + 1) % 2;
    }
    private void updatePointAnimation() {
        Thread pointAnim = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000/5);
                    updatePointAnimationState();
                    SwingUtilities.invokeLater(() -> {
                        table.repaint();
                        table.revalidate();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        pointAnim.start();
    }
}
