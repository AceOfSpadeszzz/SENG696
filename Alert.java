import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.*;

public class Alert extends Agent {

    private Map<String, List<AID>> usrMap = new HashMap<>();

    @Override
    protected void setup() {
        addBehaviour(new AlertBehaviour(this));
        register();
    }

    protected void register() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName());
        sd.setType(Config.ALERT_SERVICE);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException ex) { ex.printStackTrace();
        }
    }

    private class AlertBehaviour extends SimpleBehaviour {

        private Alert myAgent;


        public AlertBehaviour(Alert alert) {
            myAgent = alert;
        }

        private void sendAlert(String content, String conversationId, int type, AID receiver) {
            ACLMessage msg = new ACLMessage(type);
            msg.setContent(content);
            msg.setConversationId(conversationId);
            //add receivers
            msg.addReceiver(receiver);
            myAgent.send(msg);
        }

        @Override
        public void action() {
            ACLMessage msg;
            System.out.println("Alert agent started!");
            System.out.println(myAgent.getAID());
            msg = myAgent.blockingReceive();
            while (msg != null) {
                System.out.println("msg received!");
                String location = msg.getContent();
                if (msg.getSender().getLocalName().equals("PHS")) {
                    List<AID> users = myAgent.usrMap.get(location);
                    if (msg.getConversationId().equals(Config.ALERT)) {
                        for (AID user: users) {
                            sendAlert(Config.ALERT, Config.ALERT, ACLMessage.REQUEST, user);
                        }
                    } else {
                        for (AID user: users) {
                            sendAlert(Config.ALERT, Config.SAFE, ACLMessage.REQUEST, user);
                        }
                    }
                } else {
                    List<AID> list = myAgent.usrMap.getOrDefault(location, new ArrayList<>());
                    list.add(msg.getSender());
                    myAgent.usrMap.put(location, list);
                }
                msg = myAgent.blockingReceive();
            }
            // done();
        }

        @Override
        public boolean done() {
            return false;
        }
    }
}
