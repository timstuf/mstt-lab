package nure.com.agents.lb2.wumpusworld.core.my;

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
import nure.com.agents.lb2.wumpusworld.core.my.naturallanguage.NavigatorSpeech;
import nure.com.agents.lb2.wumpusworld.core.my.naturallanguage.SimpleNavigatorSpeech;

public class NavigatorAgent extends Agent {
	EfficientHybridWumpusAgent agent;
	NavigatorSpeech speech;
	private AID speleologistAid;

	@Override
	protected void takeDown() {
		System.out.println("Navigator-agent " + getAID().getName() + " terminating.");
	}

	@Override
	protected void setup() {
		agent = new EfficientHybridWumpusAgent(4, 4, new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH));
		speech = new SimpleNavigatorSpeech();
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
		@SneakyThrows
		@Override
		public void action() {
			//query - propose
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				String state = msg.getContent();
				WumpusPercept percept = speech.recognize(state);
				System.out.println("Navigator: received feelings. Feelings = " + state);
				addBehaviour(new FindActionBehaviour(percept));
			} else {
				block();
			}
		}
	}

	private class FindActionBehaviour extends OneShotBehaviour {
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
			String actionSentence = speech.tellAction(action);
			reply.setLanguage("English");
			reply.setOntology("WumpusWorld");
			reply.setContent(actionSentence);
			reply.addReplyTo(speleologistAid);
			reply.addReceiver(speleologistAid);
			myAgent.send(reply);
		}
	}
}
