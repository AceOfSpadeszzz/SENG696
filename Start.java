import jade.core.Agent;
import jade.core.AID;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Start extends Agent{
    public void setup() {
        createAgent("User1", "User");
        // createAgent("User2", "User");
        createAgent("PHS", "PHS");
        createAgent("Alert", "Alert");
        createAgent("Lab", "Lab");

    }
    private void createAgent(String name, String className) {
        AID agentID = new AID(name, AID.ISLOCALNAME);
        AgentContainer controller = getContainerController();
        try {
            AgentController agent = controller.createNewAgent(name, className, null);
            agent.start();
            System.out.println("+++ Created: " + agentID);
        }
        catch (Exception e) { e.printStackTrace(); }
    }
}
