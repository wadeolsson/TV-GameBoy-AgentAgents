package tvgameboy.games.mole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
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
        final Runnable returnToMenu;

        // player state
        private double px, py; // center position
        private double vy = 0; // vertical velocity
        private final int radius = 12;
        private boolean left, right, up, down, digging, jumping, onSurface = true;
        private final List<Point2D.Double> tunnel = new ArrayList<>();
        private final Image moleImage;
        private final Image wormImage;
        private final List<Worm> worms = new ArrayList<>();
        private int wormsCollected = 0;

        private static final class Worm {
            final Point2D.Double pos; // normalized (0..1)
            final boolean underground;
            boolean collected;

            Worm(Point2D.Double pos, boolean underground) {
                this.pos = pos;
                this.underground = underground;
                this.collected = false;
            }
        }

        GameView(Runnable returnToMenu) {
            this.returnToMenu = returnToMenu;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);

            // generate a simple mole image programmatically
            int imgSize = 48;
            BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = img.createGraphics();
            ig.setColor(new Color(60, 60, 60));
            ig.fillOval(4, 12, 40, 28);
            ig.setColor(new Color(80, 80, 80));
            ig.fillOval(8, 6, 32, 24);
            ig.setColor(new Color(30, 30, 30));
            ig.fillOval(18, 18, 6, 6);
            ig.fillOval(28, 18, 6, 6);
            ig.setColor(new Color(180, 100, 60));
            ig.fillOval(24, 24, 6, 6);
            ig.dispose();
            moleImage = img;
            // generate a small worm image
            int wImgW = 20, wImgH = 8;
            BufferedImage wimg = new BufferedImage(wImgW, wImgH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D wg = wimg.createGraphics();
            wg.setColor(new Color(220, 140, 90));
            wg.fillOval(0, 2, wImgW, wImgH - 2);
            wg.setColor(new Color(200, 110, 70));
            wg.fillOval(2, 1, wImgW/3, wImgH - 4);
            wg.dispose();
            wormImage = wimg;

            addKeyListener(new KeyAdapter() {
                @Override

                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                        case KeyEvent.VK_UP:
                            if (digging) {
                                up = true;
                            } else if (onSurface) {
                                vy = -10; jumping = true; onSurface = false;
                            }
                            break;
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_LEFT:
                            left = true; break;
                        case KeyEvent.VK_S:
                        case KeyEvent.VK_DOWN:
                            // start digging if on surface, otherwise move down while digging
                            if (onSurface) {
                                digging = true;
                                onSurface = false;
                                vy = 0;
                                jumping = false;
                                down = true; // start moving down immediately
                            } else {
                                down = true;
                            }
                            break;
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_RIGHT:
                            right = true; break;
                        case KeyEvent.VK_SPACE:
                            if (onSurface) { vy = -10; jumping = true; onSurface = false; }
                            break;
                        case KeyEvent.VK_ESCAPE:
                            SwingUtilities.invokeLater(returnToMenu); break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                        case KeyEvent.VK_UP:
                            up = false; break;
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_LEFT:
                            left = false; break;
                        case KeyEvent.VK_S:
                        case KeyEvent.VK_DOWN:
                            down = false; break;
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_RIGHT:
                            right = false; break;
                        case KeyEvent.VK_SPACE:
                            // no-op
                            break;
                    }
                }
            });

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    requestFocusInWindow();
                }
            });

            Timer t = new Timer(16, ev -> { step(); repaint(); });
            t.start();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            // place player on the surface (middle height) when component is shown
            int h = getHeight() > 0 ? getHeight() : HEIGHT;
            int w = getWidth() > 0 ? getWidth() : WIDTH;
            px = w / 2.0;
            // surface is at half height
            py = h / 2.0;
            vy = 0;
            jumping = false;
            digging = false;
            onSurface = true;
            requestFocusInWindow();
            spawnWorms();
        }

        private void spawnWorms() {
            worms.clear();
            int surfaceCount = 3;
            int undergroundCount = 12;
            int w = Math.max(100, getWidth());
            int h = Math.max(100, getHeight());
            double groundY = h / 2.0;

            for (int i = 0; i < surfaceCount; i++) {
                // place worms close above the surface so mole can reach them
                double nx = (radius + 6 + Math.random() * (w - (radius + 12))) / (double) w;
                double py = groundY - (radius + 4 + Math.random() * 6);
                double ny = py / (double) h;
                worms.add(new Worm(new Point2D.Double(nx, ny), false));
            }

            for (int i = 0; i < undergroundCount; i++) {
                double nx = (radius + 6 + Math.random() * (w - (radius + 12))) / (double) w;
                double py = groundY + (radius + 6 + Math.random() * Math.max(80, h / 3));
                double ny = py / (double) h;
                worms.add(new Worm(new Point2D.Double(nx, ny), true));
            }
        }

        private void step() {
            // horizontal movement
            double hspeed = 3.5;
            if (left) px -= hspeed;
            if (right) px += hspeed;

            double groundY = getHeight() / 2.0;

            if (digging) {
                // while digging, gravity is disabled; movement is manual only
                double manualDig = 3.0;
                if (down) {
                    py += manualDig;
                }
                if (up) {
                    py -= manualDig;
                }

                // if we've risen to or above the surface, surface again
                if (py <= groundY) {
                    py = groundY;
                    digging = false;
                    onSurface = true;
                    vy = 0;
                    jumping = false;
                } else {
                    onSurface = false;
                }
                // record tunnel position as normalized coords so it scales with component resizing
                double nw = Math.max(1, getWidth());
                double nh = Math.max(1, getHeight());
                if (!tunnel.isEmpty()) {
                    Point2D.Double last = tunnel.get(tunnel.size() - 1);
                    double lastX = last.x * nw;
                    double lastY = last.y * nh;
                    double dx = px - lastX;
                    double dy = py - lastY;
                    if (Math.hypot(dx, dy) >= radius * 0.6) {
                        tunnel.add(new Point2D.Double(px / nw, py / nh));
                    }
                } else {
                    tunnel.add(new Point2D.Double(px / nw, py / nh));
                }
            } else {
                // gravity & vertical movement
                double gravity = 0.6;
                vy += gravity;
                vy = Math.min(vy, 20);
                py += vy;

                // if below ground, clamp to ground
                if (py >= groundY) {
                    py = groundY;
                    vy = 0;
                    jumping = false;
                    onSurface = true;
                } else {
                    onSurface = false;
                }
            }

            int w = Math.max(100, getWidth());
            int h = Math.max(100, getHeight());
            double minX = radius + 2;
            double maxX = w - radius - 2;
            double minY = radius + 2;
            double maxY = h - radius - 2;
            px = Math.max(minX, Math.min(maxX, px));
            py = Math.max(minY, Math.min(maxY, py));

            // check worm collisions
            int wormRadius = 8;
            for (Worm worm : worms) {
                if (worm.collected) continue;
                double wx = worm.pos.x * getWidth();
                double wy = worm.pos.y * getHeight();
                double dx = px - wx;
                double dy = py - wy;
                if (Math.hypot(dx, dy) <= radius + wormRadius) {
                    worm.collected = true;
                    wormsCollected++;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // sky
            int half = getHeight() / 2;
            g2.setColor(new Color(90, 160, 255));
            g2.fillRect(0, 0, getWidth(), half);
            // ground
            g2.setColor(new Color(80, 50, 20));
            g2.fillRect(0, half, getWidth(), getHeight() - half);

            // draw mole centered at px,py
            int mx = (int) Math.round(px);
            int my = (int) Math.round(py);

            // draw tunnel (holes) where digging occurred
            g2.setColor(new Color(30, 20, 10));
            int cw = getWidth();
            int ch = getHeight();
            for (Point2D.Double p : tunnel) {
                int tx = (int) Math.round(p.x * cw) - radius;
                int ty = (int) Math.round(p.y * ch) - radius;
                g2.fillOval(tx, ty, radius * 2, radius * 2);
            }

            // draw worms (image)
            int wormRadius = 8;
            for (Worm worm : worms) {
                if (worm.collected) continue;
                int wx = (int) Math.round(worm.pos.x * cw);
                int wy = (int) Math.round(worm.pos.y * ch);
                int drawW = wormRadius * 2;
                int drawH = wormRadius;
                if (worm.underground) {
                    // darker tint for underground
                    g2.drawImage(wormImage, wx - drawW/2, wy - drawH/2, drawW, drawH, null);
                } else {
                    // brighter for surface (draw same image)
                    g2.drawImage(wormImage, wx - drawW/2, wy - drawH/2, drawW, drawH, null);
                }
            }

            // draw mole using image (scaled to radius)
            g2.drawImage(moleImage, mx - radius, my - radius, radius * 2, radius * 2, null);

            // simple HUD: instructions
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Mole - use W/A/S/D or ↑/←/↓/→ to move, Esc to menu", 10, 18);

            // worm counter in top-middle
            String countText = "Worms: " + wormsCollected;
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(countText);
            int centerX = getWidth() / 2;
            g2.drawString(countText, centerX - textW / 2, 18);
        }
    }
}
