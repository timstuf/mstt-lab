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
import nure.com.agents.lb2.wumpusworld.core.my.naturallanguage.SimpleSpeleologistSpeech;
import nure.com.agents.lb2.wumpusworld.core.my.naturallanguage.SpeleologistSpeech;

public class SpeleologistAgent extends Agent {
	private AID environmentAid;
	private AID navigatorAid;
	SpeleologistSpeech speech;

	@Override
	protected void setup() {
		speech = new SimpleSpeleologistSpeech();
		registerMe();
		System.out.println("Hello! Speleologist-agent " + getAID().getName() + " is ready.");
		addBehaviour(new SpeleologistBehaviour());
	}

	@Override
	protected void takeDown() {
		System.out.println("Speleologist-agent " + getAID().getName() + " terminating.");
		try {
			getContainerController().getAgent(navigatorAid.getLocalName()).kill();
			getContainerController().getAgent(environmentAid.getLocalName()).kill();
		} catch(ControllerException e) {
			throw new RuntimeException(e);
		}
		System.exit(0);
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
		private MessageTemplate mt;
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
					mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
							MessageTemplate.MatchReplyTo(new AID[]{myAgent.getAID()}));
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
					// send feeling to navigator
					ACLMessage state = new ACLMessage(ACLMessage.INFORM);
					state.setLanguage("English");
					state.setOntology("WumpusWorld");
					state.addReceiver(navigatorAid);
					String feelings = speech.tellPercept(currentState.percept);
					state.setContent(feelings);
					myAgent.send(state);
					mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
							MessageTemplate.MatchReplyTo(new AID[]{myAgent.getAID()}));
					System.out.println("Speleologist: State sent to navigator.");
					step = 3;
					break;
				case 3:
					// Receive answer from navigator
					ACLMessage reply2 = myAgent.receive(mt);
					if(reply2 != null) {
						// Reply received
						String action = reply2.getContent();
						wumpusAction = speech.recognize(action);
						System.out.println("Speleologist: Action received from navigator. Action = " + action);
						step = 4;
					} else {
						block();
					}
					break;
				case 4:
					//send action to environment
					ACLMessage action = new ACLMessage(ACLMessage.CFP);
					action.setConversationId("environment");
					action.addReceiver(environmentAid);
					action.setContent(wumpusAction.getSymbol());
					myAgent.send(action);
					mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
							MessageTemplate.MatchReplyTo(new AID[]{myAgent.getAID()}));
					step = 5;
					break;

				case 5:
					//receive ok from environment
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
					myAgent.doDelete();
					break;

			}
		}

		@Override
		public boolean done() {
			return step == 7;
		}

	}
}
