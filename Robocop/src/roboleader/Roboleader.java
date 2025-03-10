package roboleader;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.TeamRobot;


/**
 * Classe principal que representa el robot líder d'un equip.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Roboleader extends TeamRobot {
    private Estat estatActual; // Estat actual del robot
    public double battlefieldWidth;
    public double battlefieldHeight;
    public boolean lider = false;
    public double targetX, targetY; // Coordenades objectiu

    // Mapa per emmagatzemar posicions dels robots
    private Map<String, Double[]> posiciones;

    /**
     * Constructor del robot líder.
     */
    public Roboleader() {
        posiciones = new HashMap<>();
    }

    /**
     * Mètode principal d'execució del robot.
     */
    @Override
    public void run() {
        battlefieldWidth = getBattleFieldWidth();
        battlefieldHeight = getBattleFieldHeight();
        estatActual = new Estat0(this); // Inicialitza l'estat
        while (true) {
            estatActual.execute(); // Executa la lògica de l'estat
            enviarPosicion();      // Envia la posició a altres robots
            execute();             // Crida al mètode execute de TeamRobot
        }
    }

    /**
     * Gestió de l'event quan es detecta un altre robot.
     * @param e L'event de detecció d'un altre robot.
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        estatActual.onScannedRobot(e); // Redirigeix l'event a l'estat
    }

    /**
     * Gestió de l'event quan es col·lisiona amb un altre robot.
     * @param e L'event de col·lisió amb un altre robot.
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        estatActual.onHitRobot(e); // Redirigeix l'event a l'estat
    }

    /**
     * Gestió de l'event quan un robot mor.
     * @param e L'event de mort d'un robot.
     */
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        estatActual.onRobotDeath(e);
    }

    /**
     * Gestió de l'event quan es rep un missatge.
     * @param e L'event de recepció d'un missatge.
     */
    @Override
    public void onMessageReceived(MessageEvent e) {
        estatActual.onMessageReceived(e); // Redirigeix l'event a l'estat
        
        // Gestionar actualització de posició
        if (e.getMessage() instanceof String && ((String) e.getMessage()).startsWith("POS:")) {
            String[] parts = ((String) e.getMessage()).split(":");
            String robotName = parts[1];
            double posX = Double.parseDouble(parts[2]);
            double posY = Double.parseDouble(parts[3]);
            actualizarPosicion(robotName, posX, posY);
        }
    }

    /**
     * Mètode per calcular distàncies.
     * @param x1 Coordenada X del primer punt.
     * @param y1 Coordenada Y del primer punt.
     * @param x2 Coordenada X del segon punt.
     * @param y2 Coordenada Y del segon punt.
     * @return La distància entre els dos punts.
     */
    public double calcularDistancia(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    /**
     * Mètode per dirigir el robot a una posició específica.
     * @param targetX Coordenada X objectiu.
     * @param targetY Coordenada Y objectiu.
     */
    public void dirigirACantonada(double targetX, double targetY) {
        double currentX = getX();
        double currentY = getY();
        double angleCapACantonada = Math.toDegrees(Math.atan2(targetX - currentX, targetY - currentY)) - getHeading();
        setTurnRight(normAngle(angleCapACantonada));
        setAhead(Math.hypot(targetX - currentX, targetY - currentY) - 25);  
    }

    /**
     * Mètode per normalitzar un angle.
     * @param angle L'angle a normalitzar.
     * @return L'angle normalitzat.
     */
    public double normAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Canvia l'estat del robot.
     * @param nouEstat El nou estat.
     */
    public void canviEstat(Estat nouEstat) {
        this.estatActual = nouEstat;
    }

    /**
     * Mètode per pintar a la pantalla.
     * @param g El context gràfic.
     */
    @Override
    public void onPaint(Graphics2D g) {
        estatActual.onPaint(g); // Crida al mètode onPaint de l'estat
    }

    /**
     * Actualitza la posició d'un altre robot.
     * @param robotName Nom del robot.
     * @param x Coordenada X del robot.
     * @param y Coordenada Y del robot.
     */
    public void actualizarPosicion(String robotName, double x, double y) {
        posiciones.put(robotName, new Double[]{x, y});
    }

    /**
     * Obté la posició d'un altre robot.
     * @param robotName Nom del robot.
     * @return La posició del robot.
     */
    public Double[] getPosicion(String robotName) {
        return posiciones.get(robotName);
    }

    /**
     * Envia la posició actual del robot a la resta de l'equip.
     */
    public void enviarPosicion() {
        try {
            String mensaje = "POS:" + getName() + ":" + getX() + ":" + getY();
            broadcastMessage(mensaje);
        } catch (IOException e) {
            Logger.getLogger(Roboleader.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Verifica si el robot ha arribat a la posició objectiu.
     * @param targetX Coordenada X objectiu.
     * @param targetY Coordenada Y objectiu.
     * @return Cert si ha arribat a la posició objectiu, fals altrament.
     */
    public boolean cantonada(double targetX, double targetY) {
        double margin = 25; // Marge d'error per considerar que ha arribat a la posició
        double currentX = getX();
        double currentY = getY();
        return Math.abs(currentX - targetX) <= margin && Math.abs(currentY - targetY) <= margin;
    }
}
