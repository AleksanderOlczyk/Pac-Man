import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.Semaphore;

public class AnimatedBackgroundPanel extends JPanel implements Runnable {

    private static Image[] images;
    private int animIdx;
    private final int delay = 1000/50;

    public AnimatedBackgroundPanel() {
        this.animIdx = 0;
        if (images == null)
            loadImages();

        //start tread
        new Thread(this).start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(images[animIdx], 0, 0, getWidth(), getHeight(), this);
    }

    public void loadImages() {
        File directory = new File("assets\\images\\menu_background");
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("The provided path does not point to a directory.");
        }
        File[] files = directory.listFiles();
        images = new Image[files.length];
        Semaphore semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
        int numTasks = files.length / Runtime.getRuntime().availableProcessors() + 1;
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            final int startIndex = i * numTasks;
            final int endIndex = Math.min(startIndex + numTasks, files.length);
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    for (int j = startIndex; j < endIndex; j++) {
                        if (files[j].isFile()) {
                            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(files[j]))) {
                                BufferedImage bufferedImage = ImageIO.read(inputStream);
                                images[j] = bufferedImage;
                            } catch (IOException e) {
                                System.err.println("Error while reading image from file " + files[j].getName());
                                e.printStackTrace();
                            } finally {
                                images[j].flush();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println("Error while acquiring semaphore: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            }).start();
        }
        try {
            semaphore.acquire(Runtime.getRuntime().availableProcessors());
        } catch (InterruptedException e) {
            System.err.println("Error while acquiring semaphore: " + e.getMessage());
            e.printStackTrace();
        }

        //freeing resources of images
        for (Image image : images) {
            if (image instanceof BufferedImage) {
                ((BufferedImage) image).flush();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (animIdx == images.length - 1)
                    Thread.sleep(500);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            animIdx = (animIdx + 1) % images.length;
            repaint();
        }
    }
}
