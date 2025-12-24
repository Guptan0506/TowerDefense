import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class TowerDefense {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tower Defense");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // CardLayout to switch between menu and game
            CardLayout layout = new CardLayout();
            JPanel container = new JPanel(layout);

            MainMenuPanel menu = new MainMenuPanel(container, layout);
            GamePanel game = new GamePanel(container, layout);

            container.add(menu, "Menu");
            container.add(game, "Game");

            frame.add(container);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

// ===================== MAIN MENU =====================
class MainMenuPanel extends JPanel {

    public MainMenuPanel(JPanel container, CardLayout layout) {
        setPreferredSize(new Dimension(800, 600));
        setLayout(null);
        setBackground(new Color(30, 30, 30));

        JLabel title = new JLabel("TOWER DEFENSE", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        title.setBounds(150, 100, 500, 80);
        add(title);

        JButton start = new JButton("Start Game");
        start.setBounds(325, 250, 150, 50);
        start.setFocusable(false);
        start.addActionListener(e -> layout.show(container, "Game"));
        add(start);

        JButton quit = new JButton("Quit");
        quit.setBounds(325, 330, 150, 50);
        quit.setFocusable(false);
        quit.addActionListener(e -> System.exit(0));
        add(quit);
    }
}

// ===================== GAME PANEL =====================
class GamePanel extends JPanel implements ActionListener {

    private final int WIDTH = 800;
    private final int HEIGHT = 600;

    private Timer timer;
    private Path path;

    private java.util.List<Enemy> enemies = new ArrayList<>();
    private java.util.List<Tower> towers = new ArrayList<>();
    private java.util.List<Bullet> bullets = new ArrayList<>();
    private java.util.List<Explosion> explosions = new ArrayList<>();

    // Spawn / wave control
    private int spawnCounter = 0;
    private int spawnInterval = 60;

    private int wave = 1;
    private int enemiesToSpawn = 5;
    private int enemiesSpawned = 0;
    private int enemiesKilled = 0;

    private int timeBetweenWaves = 180;
    private int waveCooldown = 0;
    private boolean waveActive = true;

    // Economy / lives
    private int money = 100;
    private int lives = 20;
    private int towerCost = 30;

    // Tower hover
    private Tower hoveredTower = null;

    // Game Over / Win
    private boolean gameOver = false;
    private boolean gameWon = false;
    private JButton restartButton;

    private JPanel container;
    private CardLayout layout;

    public GamePanel(JPanel container, CardLayout layout) {
        this.container = container;
        this.layout = layout;

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(40, 100, 40));

        // Mouse: place towers
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                placeTower(e.getX(), e.getY());
            }
        });

        // Mouse: hover tower
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredTower = getTowerAt(e.getX(), e.getY());
                repaint();
            }
        });

        // Restart button
        restartButton = new JButton("Restart");
        restartButton.setFocusable(false);
        restartButton.setBounds(350, 260, 100, 40);
        restartButton.setVisible(false);
        restartButton.addActionListener(e -> restartGame());
        setLayout(null);
        add(restartButton);

        // Path
        path = new Path();
        setupPath();

        // Game loop
        timer = new Timer(16, this);
        timer.start();
    }

    private void setupPath() {
        int offsetY = 40;

        path.addPoint(50, 100 + offsetY);
        path.addPoint(250, 100 + offsetY);
        path.addPoint(450, 200 + offsetY);
        path.addPoint(350, 300 + offsetY);
        path.addPoint(150, 300 + offsetY);
        path.addPoint(150, 450 + offsetY);
        path.addPoint(400, 450 + offsetY);
        path.addPoint(650, 350 + offsetY);
        path.addPoint(750, 500 + offsetY);
    }

    private void restartGame() {
        enemies.clear();
        towers.clear();
        bullets.clear();
        explosions.clear();

        money = 100;
        lives = 20;

        wave = 1;
        enemiesToSpawn = 5;
        enemiesSpawned = 0;
        enemiesKilled = 0;

        Enemy.globalHealthBoost = 0;

        gameOver = false;
        gameWon = false;

        restartButton.setVisible(false);
        waveActive = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (gameOver) {
            repaint();
            return;
        }

        // Wave logic
        if (waveActive) {
            spawnCounter++;

            if (spawnCounter >= spawnInterval && enemiesSpawned < enemiesToSpawn) {
                spawnEnemy();
                enemiesSpawned++;
                spawnCounter = 0;
            }

            if (enemiesSpawned == enemiesToSpawn && enemiesKilled == enemiesToSpawn) {
                waveActive = false;
                waveCooldown = timeBetweenWaves;
            }
        } else {
            waveCooldown--;
            if (waveCooldown <= 0) {
                startNextWave();
            }
        }

        // Update enemies
        java.util.List<Enemy> toRemove = new ArrayList<>();
        for (Enemy enemy : enemies) {
            enemy.update();
            if (enemy.hasReachedEnd()) {

                if (enemy.getHealth() <= 0) {
                    money += 10;
                    enemiesKilled++;

                    Point p = enemy.getPosition();
                    explosions.add(new Explosion(p.x, p.y));

                } else {
                    lives--;
                }

                toRemove.add(enemy);
            }
        }
        enemies.removeAll(toRemove);

        if (lives <= 0) {
            gameOver = true;
            restartButton.setVisible(true);
        }

        // Towers shoot
        for (Tower t : towers) {
            Bullet b = t.tryShoot(enemies);
            if (b != null) bullets.add(b);
        }

        // Update bullets
        List<Bullet> removeBullets = new ArrayList<>();
        for (Bullet b : bullets) {
            if (b.update()) removeBullets.add(b);
        }
        bullets.removeAll(removeBullets);

        // Update explosions
        List<Explosion> removeExplosions = new ArrayList<>();
        for (Explosion ex : explosions) {
            ex.update();
            if (ex.done) removeExplosions.add(ex);
        }
        explosions.removeAll(removeExplosions);

        repaint();
    }

    private void startNextWave() {
        if (wave >= 5) {
            gameWon = true;
            gameOver = true;
            restartButton.setVisible(true);
            return;
        }

        wave++;
        enemiesToSpawn = 5 + (wave * 2);
        enemiesSpawned = 0;
        enemiesKilled = 0;
        waveActive = true;

        Enemy.globalHealthBoost += 10;
    }

    private void spawnEnemy() {
        enemies.add(new Enemy(path));
    }

    private void placeTower(int x, int y) {
        if (isOnPath(x, y)) return;
        if (money < towerCost) return;

        towers.add(new Tower(x, y));
        money -= towerCost;
    }

    private Tower getTowerAt(int x, int y) {
        for (Tower t : towers) {
            double dx = x - t.x;
            double dy = y - t.y;
            if (Math.sqrt(dx * dx + dy * dy) <= t.size / 2.0) return t;
        }
        return null;
    }

    private boolean isOnPath(int x, int y) {
        java.util.List<Point> pts = path.getPoints();

        for (int i = 0; i < pts.size() - 1; i++) {
            Point p1 = pts.get(i);
            Point p2 = pts.get(i + 1);

            double dist = pointToSegmentDistance(x, y, p1.x, p1.y, p2.x, p2.y);
            if (dist < 20) return true;
        }
        return false;
    }

    private double pointToSegmentDistance(double px, double py,
                                          double x1, double y1,
                                          double x2, double y2) {

        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = (lenSq != 0) ? dot / lenSq : -1;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // UI BAR
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(0, 0, getWidth(), 40);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Money: " + money, 20, 25);
        g2d.drawString("Lives: " + lives, 150, 25);
        g2d.drawString("Wave: " + wave, 260, 25);

        if (!waveActive && !gameOver) {
            g2d.drawString("Next wave in: " + (waveCooldown / 60), 360, 25);
        }

        drawPath(g2d);

        for (Enemy enemy : enemies) enemy.draw(g2d);
        for (Tower t : towers) t.draw(g2d);
        for (Bullet b : bullets) b.draw(g2d);
        for (Explosion ex : explosions) ex.draw(g2d);

        if (hoveredTower != null) {
            g2d.setColor(new Color(0, 0, 255, 40));
            g2d.fillOval(
                    hoveredTower.x - hoveredTower.range,
                    hoveredTower.y - hoveredTower.range,
                    hoveredTower.range * 2,
                    hoveredTower.range * 2
            );

            g2d.setColor(new Color(0, 0, 180));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(
                    hoveredTower.x - hoveredTower.range,
                    hoveredTower.y - hoveredTower.range,
                    hoveredTower.range * 2,
                    hoveredTower.range * 2
            );
        }

        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));

            if (gameWon) {
                g2d.drawString("YOU WIN!", 300, 250);
            } else {
                g2d.drawString("GAME OVER", 280, 250);
            }
        }

        g2d.dispose();
    }

    private void drawPath(Graphics2D g2d) {
        java.util.List<Point> pts = path.getPoints();

        g2d.setStroke(new BasicStroke(28, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(100, 100, 100));
        for (int i = 0; i < pts.size() - 1; i++)
            g2d.drawLine(pts.get(i).x, pts.get(i).y, pts.get(i+1).x, pts.get(i+1).y);

        g2d.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(140, 140, 140));
        for (int i = 0; i < pts.size() - 1; i++)
            g2d.drawLine(pts.get(i).x, pts.get(i).y, pts.get(i+1).x, pts.get(i+1).y);

        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.DARK_GRAY);
        for (int i = 0; i < pts.size() - 1; i++)
            g2d.drawLine(pts.get(i).x, pts.get(i).y, pts.get(i+1).x, pts.get(i+1).y);
    }
}

// ===================== Path =====================
class Path {
    private java.util.List<Point> controlPoints = new ArrayList<>();

    public void addPoint(int x, int y) {
        controlPoints.add(new Point(x, y));
    }

    public Point getPoint(int index) {
        return controlPoints.get(index);
    }

    public int size() {
        return controlPoints.size();
    }

    public java.util.List<Point> getPoints() {
        return controlPoints;
    }

    public Point interpolate(int segmentIndex, double t) {
        if (segmentIndex < 0) segmentIndex = 0;
        if (segmentIndex >= controlPoints.size() - 1)
            segmentIndex = controlPoints.size() - 2;

        Point p1 = controlPoints.get(segmentIndex);
        Point p2 = controlPoints.get(segmentIndex + 1);

        int x = (int) (p1.x + (p2.x - p1.x) * t);
        int y = (int) (p1.y + (p2.y - p1.y) * t);

        return new Point(x, y);
    }
}

// ===================== Enemy =====================
class Enemy {

    private Path path;
    private int currentSegment = 0;
    private double t = 0.0;
    private double speed = 0.005;

    private int size = 20;
    private boolean reachedEnd = false;

    public static int globalHealthBoost = 0;
    private int maxHealth = 50 + globalHealthBoost;
    private int health = maxHealth;

    public Enemy(Path path) {
        this.path = path;
    }

    public void update() {
        if (reachedEnd) return;

        t += speed;

        if (t >= 1.0) {
            t = 0.0;
            currentSegment++;

            if (currentSegment >= path.size() - 1)
                reachedEnd = true;
        }
    }

    public boolean hasReachedEnd() {
        return reachedEnd;
    }

    public void takeDamage(int dmg) {
        if (reachedEnd) return;

        health -= dmg;
        if (health <= 0) {
            health = 0;
            reachedEnd = true;
        }
    }

    public int getHealth() {
        return health;
    }

    public Point getPosition() {
        return path.interpolate(currentSegment, t);
    }

    public void draw(Graphics2D g2d) {
        if (reachedEnd) return;

        Point pos = getPosition();

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval(pos.x - size/2, pos.y - size/2 + 6, size, size/2);

        // Body gradient
        GradientPaint gp = new GradientPaint(
                pos.x - size/2, pos.y - size/2, new Color(200, 50, 50),
                pos.x + size/2, pos.y + size/2, new Color(120, 0, 0)
        );
        g2d.setPaint(gp);
        g2d.fillOval(pos.x - size/2, pos.y - size/2, size, size);

        // Outline
        g2d.setColor(Color.BLACK);
        g2d.drawOval(pos.x - size/2, pos.y - size/2, size, size);

        // Health bar
        g2d.setColor(Color.RED);
        g2d.fillRect(pos.x - 15, pos.y - size/2 - 10, 30, 5);

        g2d.setColor(Color.GREEN);
        int barWidth = (int) (30 * (health / (double) maxHealth));
        g2d.fillRect(pos.x - 15, pos.y - size/2 - 10, barWidth, 5);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(pos.x - 15, pos.y - size/2 - 10, 30, 5);
    }
}

// ===================== Tower =====================
class Tower {
    int x, y;
    int size = 30;
    int range = 120;

    int fireRate = 40;
    int cooldown = 0;

    public Tower(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics2D g2d) {

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval(x - size/2, y - size/2 + 6, size, size/2);

        // Metallic gradient
        GradientPaint gp = new GradientPaint(
                x - size/2, y - size/2, new Color(80, 80, 200),
                x + size/2, y + size/2, new Color(20, 20, 120)
        );
        g2d.setPaint(gp);
        g2d.fillOval(x - size/2, y - size/2, size, size);

        // Outline
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x - size/2, y - size/2, size, size);
    }

    public boolean isInRange(int ex, int ey) {
        double dx = ex - x;
        double dy = ey - y;
        return Math.sqrt(dx * dx + dy * dy) <= range;
    }

    public Enemy findTarget(List<Enemy> enemies) {
        for (Enemy e : enemies) {
            if (!e.hasReachedEnd()) {
                Point pos = e.getPosition();
                if (isInRange(pos.x, pos.y)) return e;
            }
        }
        return null;
    }

    public Bullet tryShoot(List<Enemy> enemies) {
        if (cooldown > 0) {
            cooldown--;
            return null;
        }

        Enemy target = findTarget(enemies);
        if (target != null && target.getHealth() > 0) {
            cooldown = fireRate;
            return new Bullet(x, y, target);
        }

        return null;
    }
}

// ===================== Bullet =====================
class Bullet {
    double x, y;
    double speed = 4.0;
    int size = 8;
    Enemy target;
    int damage = 10;

    public Bullet(double x, double y, Enemy target) {
        this.x = x;
        this.y = y;
        this.target = target;
    }

    public boolean update() {
        if (target == null || target.getHealth() <= 0 || target.hasReachedEnd()) {
            return true; // remove bullet
        }

        Point pos = target.getPosition();
        double dx = pos.x - x;
        double dy = pos.y - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 5) {
            target.takeDamage(damage);
            return true; // hit
        }

        x += (dx / dist) * speed;
        y += (dy / dist) * speed;

        return false; // keep bullet
    }

    public void draw(Graphics2D g2d) {
        // Glow
        g2d.setColor(new Color(255, 255, 100, 120));
        g2d.fillOval((int)x - size, (int)y - size, size * 2, size * 2);

        // Core
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)x - size/2, (int)y - size/2, size, size);

        // Outline
        g2d.setColor(Color.ORANGE);
        g2d.drawOval((int)x - size/2, (int)y - size/2, size, size);
    }
}

// ===================== Explosion =====================
class Explosion {
    double x, y;
    int radius = 0;
    int alpha = 255;
    boolean done = false;

    public Explosion(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        radius += 2;
        alpha -= 10;

        if (alpha <= 0) {
            done = true;
        }
    }

    public void draw(Graphics2D g2d) {
        if (done) return;

        // Outer glow
        g2d.setColor(new Color(255, 150, 0, Math.max(alpha, 0)));
        g2d.fillOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);

        // Inner ring
        g2d.setColor(new Color(255, 80, 0, Math.max(alpha, 0)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)(x - radius), (int)(y - radius), radius * 2, radius * 2);
    }
}