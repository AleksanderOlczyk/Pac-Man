import javax.swing.*;
import java.awt.*;

public class Error extends JFrame {
    public Error(String mess) {
        //window settings
        setTitle("Error");
        setSize(300, 100);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setResizable(false);
        setVisible(true);

        //panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(panel);

        //label
        JLabel label = new JLabel(mess);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setOpaque(false);
        label.setForeground(Color.WHITE);
        label.setBackground(Color.BLACK);
        label.setFont(Menu.REXLIA);
        panel.add(label);

        //close button
        JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(Color.BLACK);
        closeButton.setFont(Menu.REXLIA);
        closeButton.addActionListener(e -> {
            dispose();
        });
        panel.add(Box.createVerticalStrut(5));
        panel.add(closeButton);
    }
}
