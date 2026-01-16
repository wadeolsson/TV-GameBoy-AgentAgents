package tvgameboy.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import tvgameboy.shared.Game;

public final class LauncherApp {
    private static final int TILE_ROWS = 2;
    private static final int TILE_COLUMNS = 3;
    private static final int TILE_COUNT = TILE_ROWS * TILE_COLUMNS;

    private final JFrame frame;
    private final JPanel menuPanel;
    private final JPanel contentPanel;
    private Point dragOffset;
    private JButton maximizeButton;
    private Rectangle normalBounds;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LauncherApp::new);
    }

    public LauncherApp() {
        applyTheme();

        frame = new JFrame("TV GameBoy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setMinimumSize(new Dimension(900, 600));
        applyWindowIcon();

        menuPanel = buildMenuPanel();
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(10, 12, 14));

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(contentPanel, BorderLayout.CENTER);
        frame.setContentPane(root);

        showPanel(menuPanel);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildMenuPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        outer.setBackground(new Color(10, 12, 14));

        JPanel tiles = new JPanel(new GridLayout(TILE_ROWS, TILE_COLUMNS, 16, 16));
        tiles.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        tiles.setBackground(new Color(10, 12, 14));

        List<GameEntry> games = GameRegistry.getGames();
        for (int i = 0; i < TILE_COUNT; i++) {
            JButton button = createTileButton(i < games.size() ? games.get(i) : null);
            tiles.add(button);
        }

        outer.add(tiles, BorderLayout.CENTER);
        return outer;
    }

    private JButton createTileButton(GameEntry entry) {
        Color tileBackground = new Color(0, 100, 0);
        Color tileBorder = new Color(0, 128, 0);
        Color tileText = new Color(245, 246, 248);
        Color tileHover = new Color(0, 114, 0);
        Font tileFont = new Font("Segoe UI", Font.BOLD, 18);

        if (entry == null) {
            JButton button = new JButton("Empty Slot");
            button.setFont(tileFont);
            button.setOpaque(true);
            button.setBackground(tileBackground);
            button.setForeground(new Color(190, 195, 202));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tileBorder),
                    BorderFactory.createEmptyBorder(20, 16, 20, 16)
            ));
            button.setEnabled(false);
            return button;
        }

        JButton button = new JButton(entry.getDisplayName(), entry.getIcon());
        button.setFont(tileFont);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBackground(tileBackground);
        button.setForeground(tileText);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tileBorder),
                BorderFactory.createEmptyBorder(20, 16, 20, 16)
        ));
        button.setFocusPainted(false);
        button.setHorizontalTextPosition(JButton.CENTER);
        button.setVerticalTextPosition(JButton.BOTTOM);
        button.setIconTextGap(12);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(tileHover);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(tileBackground);
                button.repaint();
            }
        });
        button.addActionListener(event -> openGame(entry));
        return button;
    }

    private void openGame(GameEntry entry) {
        Game game = entry.getFactory().get();
        showPanel(game.getView(this::showMenu));
        SwingUtilities.invokeLater(this::bringToFront);
    }

    private void showMenu() {
        showPanel(menuPanel);
    }

    private void showPanel(JComponent panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private static void applyTheme() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            return;
        }

        UIManager.put("control", new Color(10, 12, 14));
        UIManager.put("info", new Color(10, 12, 14));
        UIManager.put("nimbusBase", new Color(0, 100, 0));
        UIManager.put("nimbusAlertYellow", new Color(245, 246, 248));
        UIManager.put("nimbusDisabledText", new Color(112, 160, 84));
        UIManager.put("nimbusFocus", new Color(56, 176, 0));
        UIManager.put("nimbusLightBackground", new Color(10, 12, 14));
        UIManager.put("nimbusSelectionBackground", new Color(0, 128, 0));
        UIManager.put("nimbusSelectedText", new Color(245, 246, 248));
        UIManager.put("text", new Color(245, 246, 248));
        UIManager.put("Panel.background", new Color(10, 12, 14));
        UIManager.put("Button.background", new Color(0, 100, 0));
        UIManager.put("Button.foreground", new Color(245, 246, 248));
        UIManager.put("Label.foreground", new Color(245, 246, 248));
    }

    private JPanel buildTitleBar() {
        Color barBackground = new Color(10, 12, 14);
        Color barBorder = new Color(0, 75, 35);
        Color titleText = new Color(245, 246, 248);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 14);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(barBackground);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, barBorder));

        JLabel title = new JLabel("TV GameBoy");
        title.setForeground(titleText);
        title.setFont(titleFont);
        title.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel iconLabel = new JLabel();
        iconLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 6));
        ImageIcon appIcon = loadAppIcon();
        if (appIcon != null) {
            iconLabel.setIcon(scaleIcon(appIcon, 18, 18));
        }

        JPanel titleArea = new JPanel(new BorderLayout());
        titleArea.setBackground(barBackground);
        titleArea.add(iconLabel, BorderLayout.WEST);
        titleArea.add(title, BorderLayout.CENTER);

        JButton minimizeButton = createTitleBarButton("_", 18, 12);
        minimizeButton.addActionListener(event -> frame.setState(Frame.ICONIFIED));

        maximizeButton = createTitleBarButton("MAX", 12, 10);
        maximizeButton.addActionListener(event -> toggleMaximize());

        JButton closeButton = createTitleBarButton("X", 12, 10);
        closeButton.addActionListener(event -> frame.dispose());

        JPanel controls = new JPanel(new GridLayout(1, 3, 6, 0));
        controls.setBackground(barBackground);
        controls.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        controls.add(minimizeButton);
        controls.add(maximizeButton);
        controls.add(closeButton);

        titleBar.add(titleArea, BorderLayout.WEST);
        titleBar.add(controls, BorderLayout.EAST);

        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                dragOffset = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragOffset == null) {
                    return;
                }
                Point screen = event.getLocationOnScreen();
                frame.setLocation(screen.x - dragOffset.x, screen.y - dragOffset.y);
            }
        };

        titleBar.addMouseListener(dragHandler);
        titleBar.addMouseMotionListener(dragHandler);
        title.addMouseListener(dragHandler);
        title.addMouseMotionListener(dragHandler);
        iconLabel.addMouseListener(dragHandler);
        iconLabel.addMouseMotionListener(dragHandler);

        return titleBar;
    }

    private JButton createTitleBarButton(String label, int fontSize, int horizontalPadding) {
        Color base = new Color(0, 114, 0);
        Color hover = new Color(0, 128, 0);
        JButton button = new JButton(label);
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setBackground(base);
        button.setForeground(new Color(245, 246, 248));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, horizontalPadding, 4, horizontalPadding));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(hover);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(base);
                button.repaint();
            }
        });
        return button;
    }

    private void toggleMaximize() {
        if (normalBounds != null) {
            setWindowBounds(normalBounds);
            normalBounds = null;
            maximizeButton.setText("MAX");
            return;
        }

        normalBounds = frame.getBounds();
        Rectangle usable = getUsableScreenBounds();
        setWindowBounds(usable);
        maximizeButton.setText("RESTORE");
    }

    private void setWindowBounds(Rectangle bounds) {
        frame.getContentPane().setVisible(false);
        frame.setBounds(bounds);
        frame.getContentPane().setVisible(true);
        frame.revalidate();
        frame.repaint();
    }

    private void bringToFront() {
        frame.setExtendedState(Frame.NORMAL);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();

        Timer retry = new Timer(150, new ActionListener() {
            private int attempts = 0;

            @Override
            public void actionPerformed(ActionEvent event) {
                if (frame.isFocused() || attempts >= 5) {
                    ((Timer) event.getSource()).stop();
                    frame.setAlwaysOnTop(false);
                    return;
                }
                frame.setAlwaysOnTop(true);
                frame.toFront();
                frame.requestFocus();
                attempts++;
            }
        });
        retry.setRepeats(true);
        retry.start();
    }

    private Rectangle getUsableScreenBounds() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
        );
        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom
        );
    }

    private void applyWindowIcon() {
        ImageIcon appIcon = loadAppIcon();
        if (appIcon != null) {
            frame.setIconImage(appIcon.getImage());
        }
    }

    private ImageIcon loadAppIcon() {
        try (InputStream stream = LauncherApp.class.getResourceAsStream("/icons/app-logo.png")) {
            if (stream == null) {
                return null;
            }
            Image image = ImageIO.read(stream);
            return new ImageIcon(image);
        } catch (IOException ignored) {
            return null;
        }
    }

    private ImageIcon scaleIcon(ImageIcon icon, int width, int height) {
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}

