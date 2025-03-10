package robocop;

import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;

/**
 * Estat inicial del robot Robocop. Escaneja contínuament per detectar enemics.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Estat0 implements Estat {
    private Robocop robot;

    /**
     * Constructor que inicialitza el robot en aquest estat.
     * @param robot Instància del robot Robocop.
     */
    public Estat0(Robocop robot) {
        this.robot = robot;
    }

    /**
     * Executa les accions associades a l'estat actual.
     * Gira el radar per detectar enemics.
     */
    @Override
    public void execute() {
        robot.setTurnRadarRight(10);  // Gira el radar contínuament
        if (robot.enemicDetectat) {
            robot.changeEstat(new Estat1(robot));  // Canvia a Estat1 si es detecta un enemic
        }
        robot.execute();
    }

    /**
     * Gestiona l'esdeveniment de detecció d'un robot enemic.
     * @param e Esdeveniment de detecció d'un robot.
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        double angle = robot.getHeading() + e.getBearing();
        robot.eX = robot.getX() + Math.sin(Math.toRadians(angle)) * e.getDistance(); // Calcula la posició de l'enemic en X
        robot.eY = robot.getY() + Math.cos(Math.toRadians(angle)) * e.getDistance(); // Calcula la posició de l'enemic en Y
        
        // Calcula les distàncies a cada cantonada
        double dist0 = robot.calcularDistancia(0, 0, robot.eX, robot.eY);
        double dist1 = robot.calcularDistancia(robot.battlefieldWidth, 0, robot.eX, robot.eY);
        double dist2 = robot.calcularDistancia(0, robot.battlefieldHeight , robot.eX, robot.eY);
        double dist3 = robot.calcularDistancia(robot.battlefieldWidth , robot.battlefieldHeight , robot.eX, robot.eY);

        // Selecciona la cantonada més allunyada
        if (dist0 > dist1 && dist0 > dist2 && dist0 > dist3) {
            robot.targetX = 20;
            robot.targetY = 20;
        } else if (dist1 > dist2 && dist1 > dist3) {
            robot.targetX = robot.battlefieldWidth - 20;
            robot.targetY = 20;
        } else if (dist2 > dist3) {
            robot.targetX = 20;
            robot.targetY = robot.battlefieldHeight - 20;
        } else {
            robot.targetX = robot.battlefieldWidth - 20;
            robot.targetY = robot.battlefieldHeight - 20;
        }

        robot.enemicDetectat = true;
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        // No es fa res específic en aquest estat quan es colpeja un robot.
    }
}
