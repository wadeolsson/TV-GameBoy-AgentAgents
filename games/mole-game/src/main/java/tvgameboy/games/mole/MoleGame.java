package tvgameboy.games.mole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import tvgameboy.shared.Game;

public final class MoleGame implements Game {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    @Override
    public JComponent getView(Runnable returnToMenu) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 30, 40));

        GameView view = new GameView(returnToMenu);
        panel.add(view, BorderLayout.CENTER);
        return panel;
    }

    private static final class GameView extends JComponent {
        // player
        double px = WIDTH/2.0, py = HEIGHT/2.0;
        double vx = 0, vy = 0;
        boolean left, right, jumping, digging, onSurface = true;

        final Runnable returnToMenu;

        GameView(Runnable returnToMenu) {
            this.returnToMenu = returnToMenu;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_LEFT:
                            left = true; break;
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_RIGHT:
                            right = true; break;
                        case KeyEvent.VK_SPACE:
                            if (onSurface) { vy = -8; jumping = true; onSurface = false; }
                            break;
                        case KeyEvent.VK_S:
                        case KeyEvent.VK_DOWN:
                            if (onSurface) { digging = true; onSurface = false; }
                            break;
                        case KeyEvent.VK_ESCAPE:
                            SwingUtilities.invokeLater(returnToMenu);
                            break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_LEFT:
                            left = false; break;
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_RIGHT:
                            right = false; break;
                        case KeyEvent.VK_S:
                        case KeyEvent.VK_DOWN:
                            // stop digging only when we allow surfacing
                            digging = false; break;
                    }
                }
            });

            Timer t = new Timer(16, ev -> {
                step();
                repaint();
            });
            t.start();
        }

        private void step() {
            // simple horizontal movement
            double ax = 0;
            if (left) ax -= 0.6;
            if (right) ax += 0.6;
            vx += ax;
            vx *= 0.85; // friction
            px += vx;

            if (digging) {
                // underground free movement (no gravity)
                double speed = 2.5;
                if (left) px -= speed;
                if (right) px += speed;
                if (!left && !right) { /* slow */ }
                // simple vertical for underground control
                if (jumping) py -= 2; else py += 0;
            } else {
                // surface / air
                vy += 0.5; // gravity
                vy = Math.min(vy, 12);
                py += vy;
            }

            // ground line at HEIGHT/2
            double groundY = HEIGHT/2.0;

            if (!digging && py >= groundY) {
                py = groundY;
                vy = 0;
                jumping = false;
                onSurface = true;
            }

            // keep in bounds
            px = Math.max(10, Math.min(WIDTH-10, px));
            py = Math.max(10, Math.min(HEIGHT-10, py));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // sky
            g2.setColor(new Color(90, 160, 255));
            g2.fillRect(0, 0, getWidth(), getHeight()/2);
            // ground
            g2.setColor(new Color(80, 50, 20));
            g2.fillRect(0, getHeight()/2, getWidth(), getHeight()/2);

            // draw mole
            int mx = (int) px;
            int my = (int) py;
            g2.setColor(Color.DARK_GRAY);
            g2.fillOval(mx-12, my-12, 24, 24);

            // simple HUD
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Mole - use A/D or ←/→ to move, Space to jump, ↓ to dig, Esc to menu", 10, 18);
        }
    }
}
