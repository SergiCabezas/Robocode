package roboleader;

import java.awt.Graphics2D;
import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;

/**
 * Interface que representa un estat del robot líder.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public interface Estat {
    /**
     * Executa les accions associades a l'estat.
     */
    void execute();

    /**
     * Gestió de l'event quan es detecta un altre robot.
     * @param e L'event de detecció d'un altre robot.
     */
    void onScannedRobot(ScannedRobotEvent e);

    /**
     * Gestió de l'event quan es col·lisiona amb un altre robot.
     * @param e L'event de col·lisió amb un altre robot.
     */
    void onHitRobot(HitRobotEvent e);

    /**
     * Gestió de l'event quan es rep un missatge.
     * @param e L'event de recepció d'un missatge.
     */
    void onMessageReceived(MessageEvent e);

    /**
     * Mètode de pintura per mostrar informació a la pantalla.
     * @param g El context gràfic.
     */
    public void onPaint(Graphics2D g);

    /**
     * Gestió de l'event quan un robot mor.
     * @param e L'event de mort d'un robot.
     */
    void onRobotDeath(RobotDeathEvent e);
}
