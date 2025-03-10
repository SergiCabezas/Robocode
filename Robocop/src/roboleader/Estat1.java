package roboleader;

import java.io.IOException;
import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;
import robocode.MessageEvent;
import robocode.util.Utils;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import robocode.RobotDeathEvent;

/**
 * Classe que representa l'estat 1 del robot líder, on es realitzen tasques de seguiment i combat.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Estat1 implements Estat {
    private Roboleader robot;
    private List<String> jerarquia; 
    private int miPosicion;
    private boolean sentidoHorario = true; 
    private long tiempoCambio;
    private boolean ant = false;

    // Esquivar i col·lisions
    public boolean chocar = false;
    public boolean esquivar = false;
    public boolean esquivarCompletado = false;
    public double esquivarTargetX, esquivarTargetY;
    private double esquivarAngle = 40;  // Angle per esquivar a dreta o esquerra
    public double dis;
    public double distanciaTangente;
    public double chocaDir = 0;  // Direcció de col·lisió

    // Coordenades i moviment
    private double[][] esquinas;
    private int esquinaActual = -1; 
    private boolean esquinaInicialSeleccionada = false;
    private boolean enemic = false;
    private final double distanciaMinima = 100;
    private final double distanciaMaxima = 200;
    private double enemigoX;
    private double enemigoY;
    private String enemigo = null;
    private long tiempoUltimoEscaneo;
    private final long intervaloEscaneo = 2000;

    /**
     * Constructor de l'estat 1.
     * @param robot El robot líder.
     * @param jerarquia La jerarquia dels robots.
     */
    public Estat1(Roboleader robot, List<String> jerarquia) {
        this.robot = robot;
        this.jerarquia = jerarquia;
        this.miPosicion = jerarquia.indexOf(robot.getName()); 

        double marginX = robot.battlefieldWidth * 0.1;
        double marginY = robot.battlefieldHeight * 0.1;

        esquinas = new double[][]{
            {marginX, marginY},                                        
            {robot.battlefieldWidth - marginX, marginY},                
            {robot.battlefieldWidth - marginX, robot.battlefieldHeight - marginY}, 
            {marginX, robot.battlefieldHeight - marginY}                
        };

        tiempoCambio = 100;
    }

    /**
     * Executa les accions associades a l'estat.
     */
    @Override
    public void execute() {
        if ((System.currentTimeMillis() - tiempoCambio) >= 15000) {
            invertirRolesYSentido();
            tiempoCambio = System.currentTimeMillis();
        }

        if (miPosicion == 0) {
            robot.setTurnRadarRight(robot.normAngle(robot.getHeading() - robot.getRadarHeading()));

            if (!esquinaInicialSeleccionada) {
                seleccionarEsquinaMasCercana(); 
                esquinaInicialSeleccionada = true; 
            }
            continuarTrayectoria();
            if (System.currentTimeMillis() - tiempoUltimoEscaneo >= intervaloEscaneo && !enemic) {
                robot.setTurnRadarRight(360);
                tiempoUltimoEscaneo = System.currentTimeMillis();
            }
        } else {
            if (ant) {
                seguirAntecesor();    
            }
        }

        // Comprobar si hay un enemigo
        if (enemigo != null) {
            atacarEnemigo();
        }

        if (esquivar) {
            double angleEsquiva = Math.toDegrees(Math.atan2(esquivarTargetX - robot.getX(), esquivarTargetY - robot.getY())) - robot.getHeading();
            robot.setTurnRight(robot.normAngle(angleEsquiva));
            robot.setAhead(Math.hypot(esquivarTargetX - robot.getX(), esquivarTargetY - robot.getY()) - 10);

            // Comprobació de finalització del esquive
            if (Math.abs(robot.getX() - esquivarTargetX) < 20 && Math.abs(robot.getY() - esquivarTargetY) < 20) {
                esquivar = false;
                esquivarCompletado = true;
                
                if(miPosicion == 0){
                    continuarTrayectoria();
                }else{
                    seguirAntecesor();
                }
            }
        }

        // Gestió de col·lisions amb companys
        if (chocar) {
            if (chocaDir > 0) {
                robot.setTurnLeft(esquivarAngle);
            } else {
                robot.setTurnRight(esquivarAngle);
            }
            robot.setAhead(50); 
            chocar = false; 
        }

        robot.execute();
    }

    /**
     * Detecta si el robot ha arribat a la cantonada actual.
     * @param targetX Coordenada X objectiu.
     * @param targetY Coordenada Y objectiu.
     * @return Cert si ha arribat a la cantonada, fals altrament.
     */
    private boolean haLlegadoAEsquina(double targetX, double targetY) {
        return robot.calcularDistancia(robot.getX(), robot.getY(), targetX, targetY) < 25;
    }

    /**
     * Selecciona la cantonada més propera al robot al inici.
     */
    private void seleccionarEsquinaMasCercana() {
        double dist0 = robot.calcularDistancia(esquinas[0][0], esquinas[0][1], robot.getX(), robot.getY());
        double dist1 = robot.calcularDistancia(esquinas[1][0], esquinas[1][1], robot.getX(), robot.getY());
        double dist2 = robot.calcularDistancia(esquinas[2][0], esquinas[2][1], robot.getX(), robot.getY());
        double dist3 = robot.calcularDistancia(esquinas[3][0], esquinas[3][1], robot.getX(), robot.getY());

        if (dist0 < dist1 && dist0 < dist2 && dist0 < dist3) {
            esquinaActual = 0;
        } else if (dist1 < dist2 && dist1 < dist3) {
            esquinaActual = 1;
        } else if (dist2 < dist3) {
            esquinaActual = 2;
        } else {
            esquinaActual = 3;
        }
        robot.setDebugProperty("Esquina inicial", "Seleccionada esquina " + esquinaActual);
    }

    /**
     * Continua la trajectòria cap a la cantonada actual.
     */
    private void continuarTrayectoria() {
        robot.setDebugProperty("Líder", "Moviéndose a la esquina " + esquinaActual);

        // Moure's cap a la cantonada actual
        double targetX = esquinas[esquinaActual][0];
        double targetY = esquinas[esquinaActual][1];

        // Calcular l'angle cap a la cantonada
        double angle = Math.atan2(targetX - robot.getX(), targetY - robot.getY());
        double angleToTurn = Utils.normalRelativeAngle(angle - robot.getHeadingRadians());

        // Primer girem sense avançar
        if (Math.abs(angleToTurn) > Math.toRadians(1)) {
            robot.setTurnRightRadians(angleToTurn);
            // No avancem mentre girem
            return;
        }

        // Una vegada orientats correctament, avancem cap a la cantonada
        robot.setAhead(robot.calcularDistancia(targetX, targetY, robot.getX(), robot.getY()));

        // Comprovar si ha arribat a la cantonada actual
        if (haLlegadoAEsquina(targetX, targetY)) {
            robot.setDebugProperty("Líder", "Esquina alcanzada: " + esquinaActual);

            // Enviar missatge al següent robot en la jerarquia
            String siguienteRobot = jerarquia.get(miPosicion + 1);
            try {
                robot.sendMessage(siguienteRobot, "Cantonada");
            } catch (IOException ex) {
                Logger.getLogger(Estat1.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Canviar a la següent cantonada
            if (!sentidoHorario) {
                esquinaActual = (esquinaActual - 1 + 4) % 4; // Sentit horari
            } else {
                esquinaActual = (esquinaActual + 1) % 4; // Sentit antihorari
            }
        }
    }

    /**
     * Mètode per seguir el robot immediatament superior en la jerarquia.
     */
    private void seguirAntecesor() {
        String antecesor = jerarquia.get(miPosicion - 1); // Obtenir el robot anterior
        Double[] antecesorPos = robot.getPosicion(antecesor); // Obtenir la posició del antecessor

        if (antecesorPos != null) {
            double antecesorX = antecesorPos[0];
            double antecesorY = antecesorPos[1];

            double distancia = robot.calcularDistancia(antecesorX, antecesorY, robot.getX(), robot.getY());

            if (distancia > distanciaMaxima) {
                // Si està massa lluny, accelerar per assolir-lo
                double angle = Math.atan2(antecesorX - robot.getX(), antecesorY - robot.getY());
                robot.setTurnRightRadians(Utils.normalRelativeAngle(angle - robot.getHeadingRadians()));
                robot.setAhead(distancia - distanciaMinima);
            } else if (distancia < distanciaMinima) {
                // Si està massa a prop, retrocedir lleugerament
                robot.setAhead(-distanciaMinima);
            } else {
                if (miPosicion + 1 < jerarquia.size()) {
                    String siguienteRobot = jerarquia.get(miPosicion + 1);
                    try {
                        robot.sendMessage(siguienteRobot, "Cantonada");
                    } catch (IOException ex) {
                        Logger.getLogger(Estat1.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                double angle = Math.atan2(antecesorX - robot.getX(), antecesorY - robot.getY());
                robot.setTurnRightRadians(Utils.normalRelativeAngle(angle - robot.getHeadingRadians()));
                robot.setAhead(distancia - distanciaMinima);
            }
        } else {
            // Si no es té la posició del antecessor, escanejar en busca del antecessor
            robot.setTurnRadarRight(360); // Girar el radar per buscar al antecessor
        }
    }

    /**
     * Mètode que inverteix la jerarquia i el sentit de rotació.
     */
    private void invertirRolesYSentido() {
        // Invertir el sentit de rotació
        sentidoHorario = !sentidoHorario;

        // Invertir la jerarquia
        java.util.Collections.reverse(jerarquia);

        // Actualitzar la posició del robot
        miPosicion = jerarquia.indexOf(robot.getName());

        // Reiniciar el seguiment de cantonades
        esquinaInicialSeleccionada = false; // Forçar la selecció d'una nova cantonada
        robot.setDebugProperty("Cambio de roles", "Invertido sentido y roles. Nueva posición: " + miPosicion);
    }

    /**
     * Gestió de l'event quan es detecta un altre robot.
     * @param e L'event de detecció d'un altre robot.
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Ignorar robots del mateix equip
        if (robot.isTeammate(e.getName())) {
            return;
        }

        // Esquivar si l'enemic està en trajectòria i a prop
        if (Math.abs(e.getBearing()) < 10 && e.getDistance() <= 200 && !esquivarCompletado) {
            esquivar = true;
            dis = e.getDistance();

            double angleEsquivarDerecha = esquivarAngle;
            distanciaTangente = Math.tan(Math.toRadians(angleEsquivarDerecha)) * dis;

            double angleDerecha = robot.getHeading() + angleEsquivarDerecha;
            esquivarTargetX = robot.getX() + Math.sin(Math.toRadians(angleDerecha)) * distanciaTangente;
            esquivarTargetY = robot.getY() + Math.cos(Math.toRadians(angleDerecha)) * distanciaTangente;

            // Verificar si el punt d'esquiva està fora del camp de batalla
            if (esquivarTargetX < 20 || esquivarTargetX > robot.battlefieldWidth - 20 || 
                esquivarTargetY < 20 || esquivarTargetY > robot.battlefieldHeight - 20) {
                double angleIzquierda = robot.getHeading() - esquivarAngle;
                esquivarTargetX = robot.getX() + Math.sin(Math.toRadians(angleIzquierda)) * distanciaTangente;
                esquivarTargetY = robot.getY() + Math.cos(Math.toRadians(angleIzquierda)) * distanciaTangente;
                robot.setTurnLeft(esquivarAngle);
            } else {
                robot.setTurnRight(esquivarAngle);
            }

            // Avançar cap al objectiu d'esquiva
            robot.setAhead(distanciaTangente);
        }

        // Lògica de disparo
        if (!enemic) {
            enemigo = e.getName();
            enemic = true;

            // Calcular les coordenades de l'enemic
            double angle = robot.getHeading() + e.getBearing();
            enemigoX = robot.getX() + Math.sin(Math.toRadians(angle)) * e.getDistance(); 
            enemigoY = robot.getY() + Math.cos(Math.toRadians(angle)) * e.getDistance();

            // Enviar les coordenades de l'enemic a l'equip
            try {
                robot.broadcastMessage("enemic: " + e.getName());
                robot.broadcastMessage("x: " + enemigoX);
                robot.broadcastMessage("y: " + enemigoY);
            } catch (IOException ex) {
                Logger.getLogger(Estat1.class.getName()).log(Level.SEVERE, null, ex);
            }
            robot.setDebugProperty("Nuevo objetivo", "Fijado enemigo: " + enemigo);
        }

        // Crida a la funció d'atacar enemic
        atacarEnemigo();
    }

    /**
     * Mètode per atacar a l'enemic.
     */
    private void atacarEnemigo() {
        // Calculem l'angle cap a l'enemic utilitzant les coordenades
        double targetX = enemigoX;
        double targetY = enemigoY;

        // Calcular l'angle absolut cap a l'enemic
        double angleToEnemy = Math.atan2(targetX - robot.getX(), targetY - robot.getY());
        double gunTurnAngle = Utils.normalRelativeAngle(angleToEnemy - robot.getGunHeadingRadians());

        // Depurar la direcció actual del canó i l'angle de gir necessari
        robot.setDebugProperty("Apuntando", "Angulo hacia enemigo: " + Math.toDegrees(angleToEnemy) + 
                               " | Angulo del cañon: " + Math.toDegrees(robot.getGunHeadingRadians()) + 
                               " | Necesario girar: " + Math.toDegrees(gunTurnAngle));

        // Girar el canó cap a l'enemic
        robot.setTurnGunRightRadians(gunTurnAngle);

        // Esperar a que el canó estigui alineat abans de disparar
        if (Math.abs(gunTurnAngle) < Math.toRadians(10)) {
            robot.setFire(1); // Disparar a l'enemic quan el canó està alineat
        }
    }

    /**
     * Gestió de l'event quan es col·lisiona amb un altre robot.
     * @param e L'event de col·lisió amb un altre robot.
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        chocar = true;  // Activa la senyal de col·lisió
        chocaDir = e.getBearing();  // Guardem la direcció de col·lisió
    }

    /**
     * Gestió de l'event quan es rep un missatge.
     * @param e L'event de recepció d'un missatge.
     */
    @Override
    public void onMessageReceived(MessageEvent e) {
        if (e.getMessage() instanceof String && e.getMessage().equals("Cantonada")) {
             ant=true;
            robot.setDebugProperty("LC", "lidercantonada");
         }
        if (e.getMessage() instanceof String) {
            String mensaje = (String) e.getMessage();
            if (mensaje.startsWith("enemic:")) {
                enemigo = mensaje.split(":")[1];
            }
            if (mensaje.startsWith("x:")) {
                enemigoX = Double.parseDouble(mensaje.split(":")[1].trim());
            }
            if (mensaje.startsWith("y:")) {
                enemigoY = Double.parseDouble(mensaje.split(":")[1].trim());
            }
        }
    }

    /**
     * Mètode de pintura per mostrar informació a la pantalla.
     * @param g El context gràfic.
     */
    @Override
    public void onPaint(java.awt.Graphics2D g) {
        // Pintar el rectangle del camí del líder
        g.setColor(java.awt.Color.YELLOW);
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            g.drawLine((int)esquinas[i][0], (int)esquinas[i][1], (int)esquinas[next][0], (int)esquinas[next][1]);
        }
    }

    /**
     * Gestió de l'event quan un robot mor.
     * @param e L'event de mort d'un robot.
     */
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        // Si el robot que ha mort és un enemic (no un company)
        if (!robot.isTeammate(e.getName())) {
            robot.setDebugProperty("Estado", "Enemigo " + e.getName() + " destruido. Escaneando nuevo objetivo.");
            enemigo = null;  // Reiniciar l'enemic actual
            enemic = false;  // No hi ha enemic actiu

            // Realitzar un escaneig complet amb el radar per buscar un altre enemic
            robot.setTurnRadarRight(360);
        }

        // Si un company ha mort, actualitzar la jerarquia
        if (robot.isTeammate(e.getName())) {
            int indiceMuerto = jerarquia.indexOf(e.getName());
            if (miPosicion > indiceMuerto) {
                miPosicion--;
            }
        }
    }
}
