package roboleader;

import java.awt.Color;
import java.io.IOException;
import robocode.MessageEvent;
import robocode.ScannedRobotEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;

/**
 * Estat0 gestiona la elecció del líder entre els robots i calcula les distàncies cap al líder.
 * @author Oscar Màrquez i Sergi Cabezas
 */
public class Estat0 implements Estat {
    private Roboleader robot;
    private boolean isLeaderElected = false;
    private int randomNumber;
    private String leaderName = null;
    private Map<String, Integer> randomNum;
    private Map<String, Double> distancias;
    private int size = 5; // Tamaño del equipo
    private double nummax = 0;
    private double leaderX, leaderY;
    private boolean hallegado = false;
    private List<String> jerarquia;

    /**
     * Constructor que inicialitza l'estat amb el robot corresponent.
     * @param robot Instància del robot Roboleader.
     */
    public Estat0(Roboleader robot) {
        this.robot = robot;
        this.randomNumber = new Random().nextInt(100);
        this.randomNum = new HashMap<>();
        this.distancias = new HashMap<>();
        this.jerarquia = new ArrayList<>();
        randomNum.put(robot.getName(), randomNumber);
    }

    /**
     * Executa les accions associades a l'estat actual.
     * Gestiona la selecció del líder i el càlcul de distàncies.
     */
    @Override
    public void execute() {
        robot.setDebugProperty("ESTAT", String.valueOf(robot.lider));

        // Enviar el número aleatori si no s'ha elegit líder
        if (!isLeaderElected) {
            try {
                robot.broadcastMessage(randomNumber);
            } catch (IOException ex) {
                Logger.getLogger(Estat0.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Elecció del líder
        if (randomNum.size() == size && !isLeaderElected) {
            nummax = -1;
            for (Map.Entry<String, Integer> entry : randomNum.entrySet()) {
                if (entry.getValue() > nummax) {
                    nummax = entry.getValue();
                    leaderName = entry.getKey();
                }
            }
            robot.lider = leaderName.equals(robot.getName());
            isLeaderElected = true;
            robot.setDebugProperty("leader", String.valueOf(leaderName));

            // Difondre el nom del líder
            try {
                robot.broadcastMessage("LEADER:" + leaderName);
            } catch (IOException ex) {
                Logger.getLogger(Estat0.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Si aquest robot és el líder, difon la seva posició
            if (robot.lider) {
                leaderX = robot.getX();
                leaderY = robot.getY();
                try {
                    robot.broadcastMessage("X:" + leaderX);
                    robot.broadcastMessage("Y:" + leaderY);
                } catch (IOException ex) {
                    Logger.getLogger(Estat0.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        // Calcular distàncies si el líder ha estat elegit
        if (isLeaderElected && hallegado) {
            double distancia = robot.calcularDistancia(robot.getX(), robot.getY(), leaderX, leaderY);
            try {
                robot.broadcastMessage("Distancia:" + distancia);
            } catch (IOException ex) {
                Logger.getLogger(Estat0.class.getName()).log(Level.SEVERE, null, ex);
            }
            distancias.put(robot.getName(), distancia);
        }

        // Ordenar robots un cop s'han rebut totes les distàncies
        if (distancias.size() == (size - 1)) {
            List<Map.Entry<String, Double>> distanciaList = new ArrayList<>(distancias.entrySet());
            distanciaList.sort(Map.Entry.comparingByValue());

            jerarquia.add(leaderName);  // Afegir líder
            for (Map.Entry<String, Double> entry : distanciaList) {
                jerarquia.add(entry.getKey());  // Afegir la resta
            }

            // Mostrar jerarquia en debug
            robot.setDebugProperty("Nombres ordenats per distància", String.join(", ", jerarquia));

            // Canviar a Estat 1
            robot.canviEstat(new Estat1(robot, jerarquia));
        }
        robot.execute();
    }

    /**
     * Gestiona la recepció de missatges.
     * @param e Esdeveniment de missatge rebut.
     */
    @Override
    public void onMessageReceived(MessageEvent e) {
        if (e.getMessage() instanceof Integer) {
            randomNum.put(e.getSender(), (Integer) e.getMessage());
        }

        if (e.getMessage() instanceof String) {
            String mensaje = (String) e.getMessage();

            if (mensaje.startsWith("LEADER:")) {
                leaderName = mensaje.split(":")[1];
                robot.lider = leaderName.equals(robot.getName());
                isLeaderElected = true;
                robot.setDebugProperty("leader", leaderName);
            }

            if (mensaje.startsWith("X:")) {
                leaderX = Double.parseDouble(mensaje.split(":")[1]);
            }
            if (mensaje.startsWith("Y:")) {
                leaderY = Double.parseDouble(mensaje.split(":")[1]);
                hallegado = true;
            }
            if (mensaje.startsWith("Distancia:")) {
                double dis = Double.parseDouble(mensaje.split(":")[1]);
                distancias.put(e.getSender(), dis);
            }
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // No s'implementa res aquí en aquesta fase
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        // No s'implementa res aquí en aquesta fase
    }
    
    public void onRobotDeath(RobotDeathEvent e) {
        // No s'implementa res aquí en aquesta fase
    }

    /**
     * Dibuixa un cercle per ressaltar al líder.
     * @param g Gràfics per a la pintura.
     */
    @Override
    public void onPaint(java.awt.Graphics2D g) {
        if (robot.lider) {
            g.setColor(Color.YELLOW);
            int diameter = 100;
            int x = (int) (leaderX - diameter / 2);
            int y = (int) (leaderY - diameter / 2);
            g.drawOval(x, y, diameter, diameter);
            g.drawString("Jo sóc el líder!", (int) robot.getX(), (int) robot.getY() + 15);
        }
    }
}
