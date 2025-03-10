package robocop;

import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;

/**
 * Estat en què el robot es mou cap a una cantonada mentre esquiva obstacles.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Estat1 implements Estat {
    private Robocop robot;
    public boolean esquivar = false;
    public boolean esquivarCompletado = false;
    public double dis;
    public double distanciaTangente;
    public double esquivarTargetX, esquivarTargetY;
    public boolean chocar = false;
    public double chocaDir = 0;

    /**
     * Constructor que inicialitza el robot en aquest estat.
     * @param robot Instància del robot Robocop.
     */
    public Estat1(robocop.Robocop robot) {
        this.robot = robot;
    }

    /**
     * Executa les accions associades a l'estat actual.
     * El robot es mou cap a la cantonada objectiu o esquiva si cal.
     */
    @Override
    public void execute() {
        robot.enemicDetectat = false;

        if (esquivar) {
            double angleEsquiva = Math.toDegrees(Math.atan2(esquivarTargetX - robot.getX(), esquivarTargetY - robot.getY())) - robot.getHeading();
            robot.setTurnRight(robot.normAngle(angleEsquiva));
            robot.setAhead(Math.hypot(esquivarTargetX - robot.getX(), esquivarTargetY - robot.getY()) - 10);

            if (Math.abs(robot.getX() - esquivarTargetX) < 20 && Math.abs(robot.getY() - esquivarTargetY) < 20) {
                esquivar = false;
                esquivarCompletado = true;
                robot.dirigirACantonada(robot.targetX, robot.targetY);
            }
        } else {
            robot.dirigirACantonada(robot.targetX, robot.targetY);
        }

        if (chocar) {
            if (chocaDir > 0) {
                robot.setTurnLeft(90); // Gira a l'esquerra si l'enemic és a la dreta
            } else {
                robot.setTurnRight(90); // Gira a la dreta si l'enemic és a l'esquerra
            }
            robot.setAhead(100);
            chocar = false; 
        }

        robot.setTurnRadarRight(robot.normAngle(robot.getHeading() - robot.getRadarHeading()));

        if (robot.cantonada(robot.targetX, robot.targetY)) {
            robot.changeEstat(new Estat2(robot)); // Canvia a l'estat següent quan arriba a la cantonada
        }

        robot.execute();
    }

    /**
     * Gestiona l'esdeveniment de detecció d'un robot enemic.
     * Activa la maniobra d'esquivar si un enemic està davant i a prop.
     * @param e Esdeveniment de detecció d'un robot.
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (Math.abs(e.getBearing()) < 10 && e.getDistance() <= 200 && !esquivarCompletado) {
            esquivar = true;
            dis = e.getDistance();

            double angleEsquivarDerecha = 40;
            distanciaTangente = Math.tan(Math.toRadians(angleEsquivarDerecha)) * dis;

            double angleDerecha = robot.getHeading() + 40;
            esquivarTargetX = robot.getX() + Math.sin(Math.toRadians(angleDerecha)) * distanciaTangente;
            esquivarTargetY = robot.getY() + Math.cos(Math.toRadians(angleDerecha)) * distanciaTangente;

            // Comprova si les coordenades d'esquiva estan dins dels límits del camp de batalla
            if (esquivarTargetX < 20 || esquivarTargetX > robot.battlefieldWidth - 20 || 
                esquivarTargetY < 20 || esquivarTargetY > robot.battlefieldHeight - 20) {
                double angleIzquierda = robot.getHeading() - 40;
                esquivarTargetX = robot.getX() + Math.sin(Math.toRadians(angleIzquierda)) * distanciaTangente;
                esquivarTargetY = robot.getY() + Math.cos(Math.toRadians(angleIzquierda)) * distanciaTangente;
                robot.setTurnLeft(40); // Gira a l'esquerra si l'esquiva a la dreta surt del camp de batalla
            } else {
                robot.setTurnRight(40); // Esquiva cap a la dreta
            }

            robot.setAhead(distanciaTangente); // Avança cap al punt d'esquiva
        }
    }

    /**
     * Gestiona l'esdeveniment de col·lisió amb un altre robot.
     * Gira el robot en funció de la direcció de l'impacte per evitar el bloqueig.
     * @param e Esdeveniment de col·lisió amb un altre robot.
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        chocar = true;
        chocaDir = e.getBearing();  // Guarda la direcció de l'enemic durant la col·lisió
    }
}
