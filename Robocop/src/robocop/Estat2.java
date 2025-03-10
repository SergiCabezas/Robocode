package robocop;

import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;

/**
 * Classe que implementa l'estat específic del robot.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Estat2 implements Estat {
    private Robocop robot;

    /**
     * Constructor de l'estat que rep el robot associat.
     * @param robot El robot associat a aquest estat.
     */
    public Estat2(Robocop robot) {
        this.robot = robot;
    }

    /**
     * Executa les accions associades a l'estat. En aquest cas, gira el radar buscant enemics.
     */
    @Override
    public void execute() {
        robot.setTurnRadarRight(40);  // Girem el radar en busca d'enemics 
        robot.execute();
    }

    /**
     * Gestió de l'event quan es detecta un altre robot.
     * Calcula els angles per apuntar el radar i el canó, i dispara a l'enemic.
     * @param e L'event de detecció d'un altre robot.
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        robot.angleRadar = robot.getHeading() + e.getBearing() - robot.getRadarHeading(); // Angle que ha de girar el radar per apuntar a l'enemic
        robot.angleCanon = robot.getHeading() + e.getBearing() - robot.getGunHeading(); // Angle que ha de girar el canó per apuntar a l'enemic
        robot.distancia = e.getDistance();

        robot.setTurnRadarRight(robot.normAngle(robot.angleRadar)); // Fixem el radar a l'enemic
        robot.setTurnGunRight(robot.normAngle(robot.angleCanon)); // Fixem el canó a l'enemic

        // Disparar
        double potenciaDispar = Math.min(3, 500 / robot.distancia);
        robot.fire(potenciaDispar);
    }

    /**
     * Gestió de l'event quan es col·lisiona amb un altre robot.
     * En aquest estat específic no es realitza cap acció.
     * @param e L'event de col·lisió amb un altre robot.
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        // No es fa res en cas de col·lisió
    }
}
