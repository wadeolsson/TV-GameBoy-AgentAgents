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

        // previous Y to detect surface <-> ground transitions
        private float prevPlayerY;

        // small dirt particles when surfacing/burrowing
        private static final class Particle {
            float x, y, vx, vy, life;
            Particle(float x, float y, float vx, float vy, float life) { this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; }
        }
        private final java.util.List<Particle> particles = new java.util.ArrayList<>();

        // visual mole radius accessible from step/painting
        private int moleRadius = 14;

        // simple worms
        private static final class Worm { float x,y,vx; Worm(float x,float y,float vx){this.x=x;this.y=y;this.vx=vx;} }
        private final java.util.List<Worm> worms = new java.util.ArrayList<>();
        private int spawnTimer = 0;
        // image used to draw worms
        private BufferedImage wormImage;

        private float cameraX, cameraY;

        private boolean up, down, left, right;
        // whether the mole is standing on the surface and can run/jump
        private boolean grounded = false;
        // prevent repeated digging while holding the down key
        private boolean digPressedLast = false;
        // configurable jump velocity (negative = upward)
        private float jumpVelocity = -14f;

        private final Timer tick;

        WorldView() {
            setPreferredSize(new Dimension(800, 600));
            setFocusable(true);

            // generate a small worm image (simple sprite)
            wormImage = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = wormImage.createGraphics();
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ig.setColor(new Color(220, 100, 120));
            ig.fillOval(0, 0, 20, 10);
            ig.setColor(new Color(180, 60, 80));
            ig.fillOval(6, 3, 8, 4);
            ig.dispose();

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
                        case KeyEvent.VK_W: case KeyEvent.VK_UP:
                            // jump on press if grounded, otherwise act as 'up' for underground movement
                            if (!up && grounded) {
                                vy = jumpVelocity;
                                grounded = false;
                            }
                            up = true; break;
                        case KeyEvent.VK_S: case KeyEvent.VK_DOWN: down = true; break;
                        case KeyEvent.VK_A: case KeyEvent.VK_LEFT: left = true; break;
                        case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right = true; break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W: case KeyEvent.VK_UP: up = false; break;
                        case KeyEvent.VK_S: case KeyEvent.VK_DOWN: down = false; break;
                        case KeyEvent.VK_A: case KeyEvent.VK_LEFT: left = false; break;
                        case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: right = false; break;
                    }
                }
            });

            // remove mouse digging: click only focuses the view
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
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

                // player start standing on the surface
                playerX = worldW / 2f;
                playerY = worldH / 2f - moleRadius;
                grounded = true;
                cameraX = playerX;
                cameraY = playerY;
            }
        }

        private void step() {
            ensureWorldSize();

            // remember previous vertical position to detect surface transitions
            prevPlayerY = playerY;

            // surface line Y (mole may go above it) - compute early so gravity can use it
            float surfaceY = worldH / 2f;
            float groundY = surfaceY - moleRadius;

            // movement
            float accel = 0.45f;
            if (left) vx -= accel;
            if (right) vx += accel;
            // vertical input only affects movement while underground
            boolean isCurrentlyBelow = playerY > surfaceY + 0.5f;
            if (isCurrentlyBelow) {
                if (up) vy -= accel;
                if (down) vy += accel;
            }

            // dig from surface: pressing down while grounded tunnels the mole underground
            if (down && grounded && !digPressedLast) {
                // create a hole slightly below the surface near the player
                int digY = (int)(surfaceY + moleRadius + 6);
                digAt((int)playerX, digY, moleRadius * 2);
                // place the mole just below the surface
                playerY = surfaceY + moleRadius + 8;
                vy = 1f;
                grounded = false;
                digPressedLast = true;
            }
            if (!down) {
                digPressedLast = false;
            }

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

            // gravity when above the surface: pull mole down toward surface
            float gravity = 0.6f;
            float maxFall = 10f;

            // integrate
            playerX += vx;
            // apply gravity only when above surface (not grounded)
            if (playerY < groundY) {
                vy += gravity;
                if (vy > maxFall) vy = maxFall;
            }
            playerY += vy;

            // landing detection: if we hit the surface from above, snap to groundY
            if (playerY >= groundY && playerY <= surfaceY + 0.5f) {
                playerY = groundY;
                vy = 0f;
                grounded = true;
            } else {
                // if we're below the visible surface line we are not grounded
                if (playerY > surfaceY + 0.5f) grounded = false;
                else if (playerY < groundY) grounded = false;
            }

            // bounds
            playerX = Math.max(16, Math.min(worldW - 16, playerX));
            playerY = Math.max(16, Math.min(worldH - 16, playerY));

            // surface line Y (mole may go above it) - already computed above
            // float surfaceY = worldH / 2f;

            // always dig the terrain where the player is (leave tunnel) — only when underground
            boolean isBelow = playerY > surfaceY + 0.5f;
            if (isBelow) digAt((int)playerX, (int)playerY, 18);

            // spawn/update simple worms (only underground)
            spawnTimer++;
            if (spawnTimer > 200) {
                spawnTimer = 0;
                float spawnX = (float)(Math.random() * worldW);
                float minY = worldH / 2f + moleRadius + 6f;
                float spawnY = minY + (float)(Math.random() * (worldH - minY - 20));
                worms.add(new Worm(spawnX, spawnY, (float)(Math.random() * 1.2 - 0.6)));
            }

            // update worms
            for (int i = worms.size() - 1; i >= 0; i--) {
                Worm w = worms.get(i);
                w.x += w.vx;
                if (w.x < 0) { w.x = 0; w.vx = Math.abs(w.vx); }
                if (w.x > worldW) { w.x = worldW; w.vx = -Math.abs(w.vx); }
            }

            // detect crossing surface -> spawn dirt particles when burrowing/emerging
            boolean wasBelow = prevPlayerY > surfaceY + 0.5f;
            if (wasBelow != isBelow) {
                int count = isBelow ? 12 : 18;
                for (int i = 0; i < count; i++) {
                    float angle = (float)(Math.random() * Math.PI * 2);
                    float pSpeed = (float)(Math.random() * 3 + 0.8);
                    float pvx = (float)Math.cos(angle) * pSpeed;
                    float pvy = (float)Math.sin(angle) * pSpeed - (isBelow ? 0.4f : 1.2f);
                    particles.add(new Particle(playerX + (float)(Math.random()*6-3), playerY + (float)(Math.random()*6-3), pvx, pvy, (float)(Math.random()*0.6 + 0.6f)));
                }
            }

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
            // paint tunnel as a darker shade of the dirt instead of making it transparent
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(new Color(95, 50, 12));
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

            // draw mole at center relative to camera (larger visual)
            int px = (int)(playerX - sx);
            int py = (int)(playerY - sy);
            g2.setColor(new Color(80, 40, 20));
            // use class-level moleRadius
            g2.fillOval(px - moleRadius, py - moleRadius, moleRadius * 2, moleRadius * 2);
            g2.setColor(Color.BLACK);
            g2.fillOval(px + 6, py - 6, 6, 6);

            // draw worms using sprite image
            if (wormImage != null) {
                int iw = wormImage.getWidth();
                int ih = wormImage.getHeight();
                for (Worm w : worms) {
                    int wxp = (int)(w.x - sx);
                    int wyp = (int)(w.y - sy);
                    g2.drawImage(wormImage, wxp - iw/2, wyp - ih/2, null);
                }
            } else {
                for (Worm w : worms) {
                    int wxp = (int)(w.x - sx);
                    int wyp = (int)(w.y - sy);
                    g2.setColor(new Color(220, 100, 120));
                    g2.fillOval(wxp - 6, wyp - 4, 12, 8);
                    g2.setColor(new Color(180, 60, 80));
                    g2.fillOval(wxp - 3, wyp - 2, 6, 4);
                }
            }

            // update and draw particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.18f; // gravity
                p.life -= 0.03f;
                int drawX = (int)(p.x - sx);
                int drawY = (int)(p.y - sy);
                int alpha = (int)(Math.max(0f, Math.min(1f, p.life)) * 200);
                g2.setColor(new Color(110, 68, 28, alpha));
                g2.fillOval(drawX - 2, drawY - 2, 4, 4);
                if (p.life <= 0f) particles.remove(i);
            }

            g2.dispose();
        }
    }
}
