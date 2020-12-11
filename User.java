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
import java.util.Scanner;

public class User extends Agent {
    private int uId;
    private int healthCondition;
    private Timestamp timestamp;
    private String name;
    private String address;
    private String location;
    private String password;
    private short state = 0;
    private AID receiver;
    public boolean initialed = false;

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

    protected void register() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        sd.setType(Config.USR_SERVICE);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException ex) {
            ex.printStackTrace();
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public AID getAid() {
        return aid;
    }

    private AID aid;
    private boolean alerted = false;

    public void setup() {
        addBehaviour(new UserBehaviour(this));
        register();
    }

    public void showAlert() {
        if (alerted) {
            System.out.println( location + " area is now on ACTIVE CASE ALERT!\n");
        } else {
            System.out.println( "The active case alert of area " + location + " is now DISMISSED.\n");
        }
    }

    public void login() throws SQLException {
        // Class.forName("com.mysql.jdbc.Driver");
        // User log in for further operations.
        Scanner scanner = new Scanner(System.in);
        System.out.println("User Agent: Enter your Health ID: ");
        uId = scanner.nextInt();
        scanner.nextLine();
        System.out.println("User Agent: Enter your Password: ");
        password = scanner.nextLine();
        Connection connection = DriverManager.getConnection(Config.URL, Config.NAME, Config.PWD);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT Id FROM users WHERE Id=" + uId + " AND pwd='" + password + "';");
        while (!resultSet.next()) {
            System.out.println("User Agent: Log in FAILED!, Try again!");
            System.out.println("User Agent: Enter your Health ID: ");
            uId = scanner.nextInt();
            scanner.nextLine();
            System.out.println("User Agent: Enter your Password: ");
            password = scanner.nextLine();
            resultSet = statement.executeQuery("SELECT Id FROM users WHERE Id=" + uId + " AND pwd='" + password + "';");
        }
        System.out.println("User Agent: Enter your COVID status: (0-healthy, 1-suspected, 2-positive confirmed) ");
        healthCondition = scanner.nextInt();
        scanner.nextLine();
        System.out.println("User Agent: Enter your current location: ");
        location = scanner.nextLine();
        uploadInfo(statement);
        statement.close();
        connection.close();
        System.out.println("User Agent: COVID information updated!");
    }

    public void uploadInfo(Statement statement) throws SQLException {
        // Upload user information to the database.
        timestamp = new Timestamp(System.currentTimeMillis());
        String sql = "UPDATE users SET checked=0, healthCon=" + healthCondition + ", loc='" + location + "' WHERE Id=" + uId + ";";
        statement.executeUpdate(sql);
    }

    private class UserBehaviour extends SimpleBehaviour {

        private User myAgent;

        public UserBehaviour(User user) {
            myAgent = user;
        }

        private void sendInfo(String content, String conversationId, int type, AID receiver) {
            ACLMessage msg = new ACLMessage(type);
            msg.setContent(content);
            msg.setConversationId(conversationId);
            //add receivers
            msg.addReceiver(receiver);
            myAgent.send(msg);
        }


        @Override
        public void action() {
            try {
                myAgent.login();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            setReceiver();
            sendInfo(location, Config.ALERT_SERVICE, ACLMessage.INFORM, myAgent.receiver);
            setLab();
            sendInfo(location, Config.LAB_SERVICE, ACLMessage.INFORM, myAgent.receiver);
            initialed = true;
            ACLMessage msg;
            msg = myAgent.blockingReceive();
            while (msg != null) {
                alerted = msg.getConversationId().equals(Config.ALERT);
                showAlert();
                msg = myAgent.blockingReceive();
            }
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    private void setLab() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Config.LAB_SERVICE);
        dfd.addServices(sd);

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults((long) -1);
        try {
            DFAgentDescription[] results = DFService.search(this, dfd, sc);
            receiver = results[0].getName();
        }
        catch (FIPAException ex) { ex.printStackTrace(); }
    }
}
