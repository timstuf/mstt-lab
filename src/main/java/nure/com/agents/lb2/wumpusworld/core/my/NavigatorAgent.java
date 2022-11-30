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
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusAction;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusPercept;

public class NavigatorAgent extends Agent {
	EfficientHybridWumpusAgent agent;
	private AID speleologistAid;

	@Override
	protected void setup() {
		agent = new EfficientHybridWumpusAgent(4, 4, new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH));
		registerMe();
		discover();
		System.out.println("Hello! Navigator-agent " + getAID().getName() + " is ready.");
		addBehaviour(new ListenBehavior());
	}

	private void registerMe() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("navigator");
		sd.setName("wumpus-world");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private void discover() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("speleologist");
		template.addServices(sd);


		try {
			DFAgentDescription[] result = DFService.search(this, template);
			speleologistAid = result[0].getName();
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private class ListenBehavior extends CyclicBehaviour {
		ObjectMapper objectMapper = new ObjectMapper();


		@SneakyThrows
		public void action() {
			//query - propose
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				String stateString = msg.getContent();
				State state = objectMapper.readValue(stateString, State.class);
				System.out.println("Navigator: received state.");
				addBehaviour(new FindActionBehaviour(state.percept));

			} else {
				block();
			}
		}
	}

	private class FindActionBehaviour extends OneShotBehaviour {
		ObjectMapper objectMapper = new ObjectMapper();
		WumpusPercept percept;


		FindActionBehaviour(WumpusPercept percept) {
			this.percept = percept;
		}

		@SneakyThrows
		@Override
		public void action() {
			WumpusAction action = agent.act(percept).orElseThrow();
			ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
			System.out.println("Navigator: decided on action. Action = " + action);
			reply.setContent(objectMapper.writeValueAsString(action));
			reply.addReceiver(speleologistAid);
			myAgent.send(reply);
		}
	}

}
