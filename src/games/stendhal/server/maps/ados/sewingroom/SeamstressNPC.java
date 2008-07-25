package games.stendhal.server.maps.ados.sewingroom;

import games.stendhal.server.core.config.ZoneConfigurator;
import games.stendhal.server.core.engine.SingletonRepository;
import games.stendhal.server.core.engine.StendhalRPZone;
import games.stendhal.server.core.pathfinder.FixedPath;
import games.stendhal.server.core.pathfinder.Node;
import games.stendhal.server.entity.npc.ShopList;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.behaviour.adder.BuyerAdder;
import games.stendhal.server.entity.npc.behaviour.impl.BuyerBehaviour;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
 * Ados City, house with a woman who makes sails for the ships
 */
public class SeamstressNPC implements ZoneConfigurator {
	private final ShopList shops = SingletonRepository.getShopList();

	/**
	 * Configure a zone.
	 *
	 * @param	zone		The zone to be configured.
	 * @param	attributes	Configuration attributes.
	 */
	public void configureZone(final StendhalRPZone zone, final Map<String, String> attributes) {
		buildSeamstress(zone);
	}

	private void buildSeamstress(final StendhalRPZone zone) {
		final SpeakerNPC seamstress = new SpeakerNPC("Ida") {

			@Override
			protected void createPath() {
				final List<Node> nodes = new LinkedList<Node>();
				nodes.add(new Node(7, 7));
				nodes.add(new Node(7, 14));
				nodes.add(new Node(12, 14));
				nodes.add(new Node(12, 7));
				setPath(new FixedPath(nodes, true));
			}

			@Override
			protected void createDialog() {
				addGreeting("Hello there.");
				addJob("I'm a seamstress. I make sails for ships, like the Athor ferry. If you could #offer me material I'd be grateful.");
				addHelp("If you want to go to the island Athor on the ferry, just go south once you've departed from Ados, and look for the pier.");
				new BuyerAdder().add(this, new BuyerBehaviour(shops.get("buycloaks")), false);
				addOffer("I buy cloaks, because we are short of material to make sails with. The better the material, the more I pay. My notebook on the table has the price list.");
				addGoodbye("Bye, thanks for stepping in.");
			}
		};

		seamstress.setEntityClass("woman_002_npc");
		seamstress.setPosition(7, 7);
		seamstress.initHP(100);
		zone.add(seamstress);
	}
}
