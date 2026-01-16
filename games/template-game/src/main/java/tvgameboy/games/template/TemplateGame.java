package tvgameboy.games.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import tvgameboy.shared.Game;

public final class TemplateGame implements Game {
    @Override
    public JComponent getView(Runnable returnToMenu) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(10, 12, 14));

        JButton menuButton = new JButton("Menu");
        menuButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuButton.setBackground(new Color(0, 100, 0));
        menuButton.setForeground(new Color(245, 246, 248));
        menuButton.setFocusPainted(false);
        menuButton.addActionListener(event -> returnToMenu.run());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(10, 12, 14));
        topBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        topBar.add(menuButton, BorderLayout.WEST);

        JLabel label = new JLabel("Replace this with your game.", SwingConstants.CENTER);
        label.setForeground(new Color(245, 246, 248));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
