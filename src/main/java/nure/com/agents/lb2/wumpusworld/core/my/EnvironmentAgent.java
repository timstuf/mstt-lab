package nure.com.agents.lb2.wumpusworld.core.my;

import com.fasterxml.jackson.databind.ObjectMapper;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.SneakyThrows;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.AgentPosition;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.EfficientHybridWumpusAgent;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.HybridWumpusAgent;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusAction;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusCave;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusEnvironment;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusPercept;

public class EnvironmentAgent extends Agent {
	private WumpusEnvironment wumpusEnvironment;
	private AID speleologistAID;
	private HybridWumpusAgent speleologist;
	private WumpusPercept percept;
	private int tick = 0;

	@Override
	protected void setup() {
		registerMe();
		wumpusEnvironment = new WumpusEnvironment(new WumpusCave(4, 4, ""
				+ ". W G P "
				+ ". . P . "
				+ ". . . . "
				+ "S . P . "));
		speleologist = new EfficientHybridWumpusAgent(4, 4, new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH));
		percept = new WumpusPercept();
		wumpusEnvironment.addAgent(speleologist);


		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("speleologist");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			speleologistAID = result[0].getName();
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new ListenBehavior());
		System.out.println("Hello! Environment-agent " + getAID().getName() + " is ready.");
	}

	private void registerMe() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("environment");
		sd.setName("wumpus-world");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private class ListenBehavior extends CyclicBehaviour {
		public void action() {
			//query - propose
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchPerformative(ACLMessage.CFP));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				if(ACLMessage.REQUEST == msg.getPerformative()) {
					addBehaviour(new QueryBehaviour());
				} else if(ACLMessage.CFP == msg.getPerformative()) {
					String move = msg.getContent();
					wumpusEnvironment.execute(speleologist, WumpusAction.fromString(move));
					addBehaviour(new AcceptBehaviour());
				} else {
					block();
				}
			} else {
				block();
			}
		}
	}

	private class QueryBehaviour extends OneShotBehaviour {
		ObjectMapper objectMapper = new ObjectMapper();

		@SneakyThrows
		@Override
		public void action() {
			AgentPosition agentPosition = wumpusEnvironment.getAgentPosition(speleologist);
			percept = wumpusEnvironment.getPerceptSeenBy(speleologist);
			ACLMessage report = new ACLMessage(ACLMessage.INFORM);
			report.setContent(objectMapper.writeValueAsString(new State(percept, tick++)));
			report.addReceiver(speleologistAID);
			myAgent.send(report);
			System.out.println("Environment: percept sent to speleologist");
			System.out.println("Environment: current position: " + agentPosition);
		}
	}

	private class AcceptBehaviour extends OneShotBehaviour {
		@Override
		public void action() {
			ACLMessage report = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			report.setContent("OK");
			report.addReceiver(speleologistAID);
			System.out.println("Environment: step performed.");
			myAgent.send(report);
		}
	}
}
