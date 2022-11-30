package nure.com.agents.lb1;


import java.util.Arrays;
import java.util.stream.IntStream;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BookBuyerAgent extends Agent {
	private String targetBookTitle;
	private AID[] sellerAgents = {new AID("seller1", AID.ISLOCALNAME),
			new AID("seller2", AID.ISLOCALNAME)};


	protected void setup() {
		// Printout a welcome message
		System.out.println("Hello! Buyer-agent " + getAID().getName() + " is ready.");
		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		if(args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Trying to buy " + targetBookTitle);
			addBehaviour(new TickerBehaviour(this, 20000) {
				protected void onTick() {
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						sellerAgents = new AID[result.length];
						IntStream.range(0, result.length).forEach(i -> sellerAgents[i] = result[i].getName());
						System.out.println("Registered seller agents =" + Arrays.toString(sellerAgents));
					} catch(FIPAException fe) {
						fe.printStackTrace();
					}
					myAgent.addBehaviour(new RequestPerformer());
				}
			});
		} else {
			// Make the agent terminate
			System.out.println("No target book title specified");
			doDelete();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
	}


	/**
	 * Inner class RequestPerformer.
	 * This is the behaviour used by Book-buyer agents to request seller
	 * agents the target book.
	 */
	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // The agent who provides the best offer
		private int bestPrice; // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch(step) {
				case 0:
					// Send the cfp to all sellers
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					for(AID sellerAgent : sellerAgents) {
						cfp.addReceiver(sellerAgent);
					}
					cfp.setContent(targetBookTitle);
					cfp.setConversationId("book-trade");
					cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
					myAgent.send(cfp);
					// Prepare the template to get proposals
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
							MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 1;
					break;
				case 1:
					// Receive all proposals/refusals from seller agents
					ACLMessage reply = myAgent.receive(mt);
					if(reply != null) {
						// Reply received
						if(reply.getPerformative() == ACLMessage.PROPOSE) {
							// This is an offer
							int price = Integer.parseInt(reply.getContent());
							if(bestSeller == null || price < bestPrice) {
								// This is the best offer at present
								bestPrice = price;
								bestSeller = reply.getSender();
							}
						}
						repliesCnt++;
						if(repliesCnt >= sellerAgents.length) {
							// We received all replies
							step = 2;
						}
					} else {
						block();
					}
					break;
				case 2:
					// Send the purchase order to the seller that provided the best offer
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(bestSeller);
					order.setContent(targetBookTitle);
					order.setConversationId("book-trade");
					order.setReplyWith("order" + System.currentTimeMillis());
					myAgent.send(order);
					// Prepare the template to get the purchase order reply
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
							MessageTemplate.MatchInReplyTo(order.getReplyWith()));
					step = 3;
					break;
				case 3:
					// Receive the purchase order reply
					reply = myAgent.receive(mt);
					if(reply != null) {
						// Purchase order reply received
						if(reply.getPerformative() == ACLMessage.INFORM) {
							// Purchase successful. We can terminate
							System.out.println(targetBookTitle + " successfully purchased.");
							System.out.println("Price = " + bestPrice);
							myAgent.doDelete();
						}
						step = 4;
					} else {
						block();
					}
					break;
			}
		}

		public boolean done() {
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	} // End of inner class RequestPerformer
}
