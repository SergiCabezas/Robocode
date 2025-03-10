package robocop;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;

/**
 * Classe principal del robot Robocop, que extén AdvancedRobot.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Robocop extends AdvancedRobot {
    public double eX, eY;
    public double battlefieldWidth;
    public double battlefieldHeight;
    public double targetX, targetY;
    public boolean enemicDetectat = false;
    
    public double angleRadar;
    public double distancia;
    public double angleCanon;

    private Estat estatActual;

    /**
     * Mètode principal del robot, que inicialitza el camp de batalla i ajusta els components.
     * El robot canvia d'estat segons les condicions del camp de batalla.
     */
    @Override
    public void run() {
        battlefieldWidth = getBattleFieldWidth();
        battlefieldHeight = getBattleFieldHeight();

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        estatActual = new Estat0(this);  // Estat inicial
        while (true) {
            estatActual.execute();  // Executa l'estat actual
            execute(); 
        }
    }

    /**
     * Gestiona l'esdeveniment quan es detecta un altre robot al radar.
     * @param e Esdeveniment de detecció d'un robot.
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        estatActual.onScannedRobot(e);
    }

    /**
     * Gestiona l'esdeveniment quan el robot colpeja un altre robot.
     * @param e Esdeveniment de col·lisió amb un altre robot.
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        estatActual.onHitRobot(e);
    }

    /**
     * Canvia l'estat actual del robot.
     * @param newEstat Nou estat a aplicar.
     */
    public void changeEstat(Estat newEstat) {
        this.estatActual = newEstat;
    }

    /**
     * Dirigeix el robot cap a una cantonada específica del camp de batalla.
     * @param targetX Coordenada X de la cantonada objectiu.
     * @param targetY Coordenada Y de la cantonada objectiu.
     */
    public void dirigirACantonada(double targetX, double targetY) {
        double currentX = getX();
        double currentY = getY();
        double angleCapACantonada = Math.toDegrees(Math.atan2(targetX - currentX, targetY - currentY)) - getHeading();
        setTurnRight(normAngle(angleCapACantonada));  // Gira cap a la cantonada
        setAhead(Math.hypot(targetX - currentX, targetY - currentY) - 25);  // Avança cap a la cantonada
    }

    /**
     * Normalitza l'angle per assegurar-se que està dins del rang [-180, 180].
     * @param angle Angle a normalitzar.
     * @return L'angle normalitzat.
     */
    public double normAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Comprova si el robot està proper a la cantonada objectiu.
     * @param targetX Coordenada X de la cantonada.
     * @param targetY Coordenada Y de la cantonada.
     * @return Cert si el robot es troba a prop de la cantonada.
     */
    public boolean cantonada(double targetX, double targetY) {
        return Math.abs(getX() - targetX) < 50 && Math.abs(getY() - targetY) < 50;  
    }

    /**
     * Calcula la distància entre dos punts del camp de batalla.
     * @param x1 Coordenada X del primer punt.
     * @param y1 Coordenada Y del primer punt.
     * @param x2 Coordenada X del segon punt.
     * @param y2 Coordenada Y del segon punt.
     * @return La distància entre els dos punts.
     */
    public double calcularDistancia(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }
}
