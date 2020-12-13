import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.sql.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class PHS extends Agent {

    private AID aid;
    public AID getAid() {
        return aid;
    }
    private AID receiver;
    private PHSBehaviour behaviour;
    public void setup() {
        behaviour = new PHSBehaviour(this);
        register();
        setReceiver();
        addBehaviour(behaviour);
    }

    public void getUpdatedRecords() throws SQLException {
        Connection connection = DriverManager.getConnection(Config.URL, Config.NAME, Config.PWD);
        Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ResultSet resultSet = statement.executeQuery("SELECT id, loc, addr, healthCon, checked FROM users WHERE checked = 0");
        while (resultSet.next()) {
            System.out.println(resultSet.getString("loc") + " " + resultSet.getString("addr") + " " + resultSet.getInt("healthCon"));
            resultSet.updateInt("checked", 1);
            resultSet.updateRow();
        }
    }

    protected void register() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        sd.setType(Config.PHS_SERVICE);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }
    }

    public void setReceiver() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Config.ALERT_SERVICE);
        dfd.addServices(sd);

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults((long) -1);
        try {
            DFAgentDescription[] results = DFService.search(this, dfd, sc);
            receiver = results[0].getName();
        }
        catch (FIPAException ex) { ex.printStackTrace(); }
    }


    private class PHSBehaviour extends SimpleBehaviour {

        private PHS myAgent;

        private void sendAlert(String content, String conversationId, int type, AID receiver) {
            ACLMessage msg = new ACLMessage(type);
            msg.setContent(content);
            msg.setConversationId(conversationId);
            //add receivers
            msg.addReceiver(receiver);
            myAgent.send(msg);
        }

        public PHSBehaviour(PHS phs) {
            myAgent = phs;
        }

        @Override
        public void action() {
            myAgent.blockingReceive();
            try {
                getUpdatedRecords();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // PHS issues alert
            Scanner myObj = new Scanner(System.in);
            System.out.println("PHS Agent: Enter the area to be notified. Enter 'q' to exit: ");
            String location = myObj.nextLine();
            System.out.println("PHS Agent: Enter the status of this area. 1-Alert, 0-Safe. Enter 'q' to exit: ");
            String status = myObj.nextLine();
            while (!location.equals("q") && !status.equals("q")) {
                if (status.equals(Config.ALERT)) {
                    sendAlert(location, Config.ALERT, ACLMessage.INFORM, myAgent.receiver);
                } else {
                    sendAlert(location, Config.SAFE, ACLMessage.INFORM, myAgent.receiver);
                }
                System.out.println("PHS Agent: Enter the location, enter q to quit");
                location = myObj.nextLine();
                System.out.println("PHS Agent: Enter the status, enter q to quit");
                status = myObj.nextLine();
            }
            System.out.println("PHS agent closed");
        }

        @Override
        public boolean done() {
            return false;
        }
    }
}
