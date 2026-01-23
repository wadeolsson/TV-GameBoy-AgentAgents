package tvgameboy.games.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
package tvgameboy.games.template;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import tvgameboy.shared.Game;

/**
 * MoleGame — continuous digging with a smooth camera.
 * The world is a large continuous image where the top half is sky and the bottom half is dirt.
 * The mole moves freely with WASD and leaves a tunnel (erased pixels) behind.
 */
public final class MoleGame implements Game {
    @Override
    public JComponent getView(Runnable returnToMenu) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(10, 12, 14));

        JPanel top = new JPanel(null);
        top.setBackground(new Color(10, 12, 14));
        top.setPreferredSize(new Dimension(0, 44));

        JButton menuButton = new JButton("Menu");
        menuButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuButton.setBounds(8, 8, 80, 28);
        menuButton.addActionListener(e -> returnToMenu.run());
        top.add(menuButton);

        JLabel label = new JLabel("Mole — Dig with W/A/S/D or click to dig", SwingConstants.CENTER);
        label.setForeground(new Color(245, 246, 248));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setBounds(100, 8, 360, 28);
        top.add(label);

        panel.add(top, BorderLayout.NORTH);

        WorldView world = new WorldView();
        panel.add(world, BorderLayout.CENTER);

        return panel;
    }

    private static final class WorldView extends JComponent {
        private BufferedImage terrain;
        private int worldW = 1600; // will expand based on component size
        private int worldH = 600;

        private float playerX;
        private float playerY;
        private float vx, vy;

        private float cameraX, cameraY;

        private boolean up, down, left, right;

        private final Timer tick;

        WorldView() {
            setPreferredSize(new Dimension(800, 600));
            setFocusable(true);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    ensureWorldSize();
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W: up = true; break;
                        case KeyEvent.VK_S: down = true; break;
                        case KeyEvent.VK_A: left = true; break;
                        case KeyEvent.VK_D: right = true; break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W: up = false; break;
                        case KeyEvent.VK_S: down = false; break;
                        case KeyEvent.VK_A: left = false; break;
                        case KeyEvent.VK_D: right = false; break;
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int sx = (int)(cameraX - getWidth() / 2f);
                    int sy = (int)(cameraY - getHeight() / 2f);
                    int wx = sx + e.getX();
                    int wy = sy + e.getY();
                    digAt(wx, wy, 22);
                    requestFocusInWindow();
                    repaint();
                }
            });

            tick = new Timer(16, e -> {
                step();
                repaint();
            });
            tick.start();
        }

        private void ensureWorldSize() {
            int w = Math.max(1600, getWidth() * 3);
            int h = Math.max(600, getHeight());
            if (terrain == null || w != worldW || h != worldH) {
                worldW = w;
                worldH = h;
                terrain = new BufferedImage(worldW, worldH, BufferedImage.TYPE_INT_ARGB);
                // fill bottom half with dirt
                Graphics2D g = terrain.createGraphics();
                g.setColor(new Color(135, 206, 235)); // sky (will show where transparent)
                g.fillRect(0, 0, worldW, worldH / 2);
                g.setColor(new Color(120, 72, 0)); // dirt
                g.fillRect(0, worldH / 2, worldW, worldH - worldH / 2);
                g.dispose();

                // player start roughly at surface center
                playerX = worldW / 2f;
                playerY = worldH / 2f - 16;
                cameraX = playerX;
                cameraY = playerY;
            }
        }

        private void step() {
            ensureWorldSize();

            // movement
            float accel = 0.45f;
            if (left) vx -= accel;
            if (right) vx += accel;
            if (up) vy -= accel;
            if (down) vy += accel;

            // clamp speed
            float maxSpeed = 6f;
            float speed = (float)Math.hypot(vx, vy);
            if (speed > maxSpeed) {
                float scale = maxSpeed / speed;
                vx *= scale;
                vy *= scale;
            }

            // apply friction
            vx *= 0.85f;
            vy *= 0.85f;

            playerX += vx;
            playerY += vy;

            // bounds
            playerX = Math.max(16, Math.min(worldW - 16, playerX));
            playerY = Math.max(16, Math.min(worldH - 16, playerY));

            // surface restriction: cannot go above surface line (worldH/2)
            float surfaceY = worldH / 2f;
            if (playerY < surfaceY) {
                playerY = surfaceY;
                if (vy < 0) vy = 0;
            }

            // always dig the terrain where the player is (leave tunnel)
            digAt((int)playerX, (int)playerY, 18);

            // smooth camera easing
            float ease = 0.08f;
            cameraX += (playerX - cameraX) * ease;
            cameraY += (playerY - cameraY) * ease;

            // clamp camera inside world so we don't show void
            float halfW = getWidth() / 2f;
            float halfH = getHeight() / 2f;
            cameraX = Math.max(halfW, Math.min(worldW - halfW, cameraX));
            cameraY = Math.max(halfH, Math.min(worldH - halfH, cameraY));
        }

        private void digAt(int wx, int wy, int radius) {
            if (terrain == null) return;
            int x0 = wx - radius;
            int y0 = wy - radius;
            Graphics2D g = terrain.createGraphics();
            g.setComposite(AlphaComposite.Clear);
            g.fillOval(x0, y0, radius * 2, radius * 2);
            g.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ensureWorldSize();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // clear background (sky)
            g2.setColor(new Color(135, 206, 235));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // compute camera window
            int sx = (int)(cameraX - getWidth() / 2f);
            int sy = (int)(cameraY - getHeight() / 2f);

            // draw terrain (dirt image) offset by camera
            g2.drawImage(terrain, -sx, -sy, null);

            // draw surface line indicator
            int surfaceScreenY = (int)((worldH / 2f) - sy);
            g2.setColor(new Color(80, 140, 60, 180));
            g2.fillRect(0, surfaceScreenY, getWidth(), 4);

            // draw mole at center relative to camera
            int px = (int)(playerX - sx);
            int py = (int)(playerY - sy);
            g2.setColor(new Color(80, 40, 20));
            g2.fillOval(px - 10, py - 10, 20, 20);
            g2.setColor(Color.BLACK);
            g2.fillOval(px + 4, py - 4, 4, 4);

            g2.dispose();
        }
    }
}
