package nure.com.agents.lb2.wumpusworld.core.my;

import com.fasterxml.jackson.databind.ObjectMapper;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.ControllerException;
import lombok.SneakyThrows;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusAction;

public class SpeleologistAgent extends Agent {
	private AID environmentAid;
	private AID navigatorAid;

	@Override
	protected void setup() {
		registerMe();
		System.out.println("Hello! Speleologist-agent " + getAID().getName() + " is ready.");
		addBehaviour(new SpeleologistBehaviour());
	}

	private void registerMe() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("speleologist");
		sd.setName("wumpus-world");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch(FIPAException fe) {
			fe.printStackTrace();
		}
	}

	class SpeleologistBehaviour extends Behaviour {
		WumpusAction wumpusAction;
		ObjectMapper objectMapper = new ObjectMapper();
		private MessageTemplate mt; // The template to receive replies
		private State currentState;
		private int step = 0;

		private void discover() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("environment");
			template.addServices(sd);

			DFAgentDescription template2 = new DFAgentDescription();
			ServiceDescription sd2 = new ServiceDescription();
			sd2.setType("navigator");
			template2.addServices(sd2);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				environmentAid = result[0].getName();
				DFAgentDescription[] result2 = DFService.search(myAgent, template2);
				navigatorAid = result2[0].getName();
			} catch(FIPAException fe) {
				fe.printStackTrace();
			}
		}

		@SneakyThrows
		@Override
		public void action() {
			switch(step) {
				case 0:
					discover();
					// Send the cfp to all sellers
					ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
					request.setContent("Current state");
					request.addReceiver(environmentAid);
					myAgent.send(request);
					// Prepare the template to get proposals
					mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
					System.out.println("Speleologist: Request for current state sent.");
					step = 1;
					break;
				case 1:
					// Receive answer from environment
					ACLMessage reply = myAgent.receive(mt);
					if(reply != null) {
						// Reply received
						if(reply.getPerformative() == ACLMessage.INFORM) {
							String state = reply.getContent();
							System.out.println("Speleologist: Info about state received from environment. State = " + state);
							currentState = objectMapper.readValue(state, State.class);
						}
						step = 2;
					} else {
						block();
					}
					break;
				case 2:
					ACLMessage state = new ACLMessage(ACLMessage.INFORM);
					state.addReceiver(navigatorAid);
					state.setContent(objectMapper.writeValueAsString(currentState));
					myAgent.send(state);
					mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
					System.out.println("Speleologist: State sent to navigator.");
					step = 3;
					break;
				case 3:
					// Receive answer from navigator
					ACLMessage reply2 = myAgent.receive(mt);
					if(reply2 != null) {
						// Reply received
						String action = reply2.getContent();
						wumpusAction = objectMapper.readValue(action, WumpusAction.class);
						System.out.println("Speleologist: Action received from navigator.");

						step = 4;
					} else {
						block();
					}
					break;
				case 4:
					ACLMessage action = new ACLMessage(ACLMessage.CFP);
					action.setConversationId("environment");
					action.addReceiver(environmentAid);
					action.setContent(wumpusAction.getSymbol());
					myAgent.send(action);
					mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
					step = 5;
					break;

				case 5:
					ACLMessage envReply = myAgent.receive(mt);
					if(envReply != null) {
						// Reply received
						if(envReply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
							// This is an answer
							if(WumpusAction.CLIMB.equals(wumpusAction)) {
								System.out.println("Speleologist: Climbed out of the cave.");
								step = 6;
							} else {
								System.out.println("Speleologist: Going to step 0.");
								step = 0;
							}

						}
					} else {
						block();
					}
					break;
				case 6:
					System.out.println("Game over");
					step = 7;
					try {
						myAgent.doDelete();
						myAgent.getContainerController().getPlatformController().kill();
					} catch(final ControllerException e) {
						System.out.println("Failed to end simulation.");
					}
					break;

			}
		}

		@Override
		public boolean done() {
			return step == 7;
		}

	}
}
