import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import javax.swing.text.LabelView;
import java.sql.*;
import java.util.Date;
import java.util.Scanner;

public class Lab extends Agent {

    private AID receiver;

    @Override
    protected void setup() {
        register();
        setPHS();
        addBehaviour(new LabBehaviour(this));
    }

    private void setPHS() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Config.PHS_SERVICE);
        dfd.addServices(sd);

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults((long) -1);
        try {
            DFAgentDescription[] results = DFService.search(this, dfd, sc);
            receiver = results[0].getName();
        }
        catch (FIPAException ex) { ex.printStackTrace(); }
    }

    public ResultSet retrieveUserInfo() throws SQLException {
        // Retrieve users' information from database.
        Connection connection = DriverManager.getConnection(Config.URL, Config.NAME, Config.PWD);
        Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        return statement.executeQuery("SELECT * FROM users WHERE healthCon=1");
    }

    public void processUser() throws SQLException {
        ResultSet resultSet = retrieveUserInfo();
        Scanner scanner = new Scanner(System.in);
        while (resultSet.next()) {
            System.out.println("Lab Agent: Please update the users' condition, 0 for healthy, 2 for confirmed, 3 to skip, 4 to quit");
            System.out.println("Lab Agent: User: " + resultSet.getString("name"));
            System.out.println("Lab Agent: User Condition: " + resultSet.getString("healthCon"));
            int condition = scanner.nextInt();
            switch (condition) {
                case 3:
                    continue;
                case 4:
                    break;
                default:
                    resultSet.updateInt("healthCon", condition);
                    resultSet.updateRow();
                    break;
            }
        }
    }

    protected void register() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        sd.setType(Config.LAB_SERVICE);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException ex) { ex.printStackTrace();
        }
    }

    private class LabBehaviour extends SimpleBehaviour {

        private Lab myAgent;

        public LabBehaviour(Lab lab) { myAgent = lab; }

        @Override
        public void action() {
            myAgent.blockingReceive();
            try {
                retrieveUserInfo();
                processUser();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("Move On");
            msg.setConversationId(Config.PHS_SERVICE);
            //add receivers
            msg.addReceiver(receiver);
            myAgent.send(msg);
        }

        @Override
        public boolean done() {
            return false;
        }
    }
}
