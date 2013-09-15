/***************************************************************************
 *                   (C) Copyright 2013 - Faiumoni e. V.                   *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.server.entity.status;

import games.stendhal.server.core.events.TutorialNotifier;
import games.stendhal.server.entity.RPEntity;
import games.stendhal.server.entity.item.ConsumableItem;
import games.stendhal.server.entity.player.Player;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

/**
 * handles a list of status for an entity
 * 
 * @author hendrik
 */
public class StatusList {
	private WeakReference<RPEntity> entityRef;

	/** Container for statuses inflicted on entity */
	private List<Status> statuses;

	/** Immunites to statuses */
	private EnumSet<StatusType> immunities;

	/**
	 * StatusList for an entity
	 *
	 * @param entity RPEntity which has the statuses managed by this list.
	 */
	public StatusList(RPEntity entity) {
		this.entityRef = new WeakReference<RPEntity>(entity);
		immunities = EnumSet.noneOf(StatusType.class);
		statuses = new LinkedList<Status>();
	}

	/**
	 * Get statuses that are currently inflicted on the entity
	 * 
	 * @return List of statuses
	 */
	public List<Status> getStatuses() {
		return statuses;
	}

	/**
	 * Find the index of the first occurance of the status effect
	 * 
	 * @param statusName
	 *            Status effect to search for
	 * @return List index of status effect
	 */
	public int getFirstStatusIndex(final String statusName) {
		int index;
		for (index = 0; index < statuses.size(); index++) {
			if (statuses.get(index).getName() == statusName) {
				return index;
			}
		}
		return -1;
	}

	/**
	 * Count how many occurances of a status are inflicted on the entity
	 * 
	 * @param statusName
	 *            Name of the status being checked
	 * @return Number of times status is found
	 */
	public int statusOccurrenceCount(final String statusName) {
		int count = 0;
		for (Status status : statuses) {
			if (status.getName().equals(statusName)) {
				count += 1;
			}
		}
		return count;
	}

	/**
	 * Count how many occurances of a status are inflicted on the entity
	 * 
	 * @param statusType type of status being checked
	 * @return number of times status is found
	 */
	public int countStatusByType(StatusType statusType) {
		int count = 0;
		for (Status status : statuses) {
			if (status.getStatusType() == statusType) {
				count += 1;
			}
		}
		return count;
	}

	/**
	 * gets the first status of the specified status subclass
	 *
	 * @param statusClass status subclass
	 * @return Status or <code>null</code>
	 */
	<T extends Status> T getFirstStatusByClass(Class<T> statusClass) {
		for (Status status : statuses) {
			if (status.getClass().equals(statusClass)) {
				return statusClass.cast(status);
			}
		}
		return null;
	}

	/**
	 * gets all statuses of the specified status subclass
	 *
	 * @param statusClass status subclass
	 * @return Status or <code>null</code>
	 */
	<T extends Status> List<T> getAllStatusByClass(Class<T> statusClass) {
		List<T> res = new LinkedList<T>();
		for (Status status : statuses) {
			if (status.getClass().equals(statusClass)) {
				res.add(statusClass.cast(status));
			}
		}
		return res;
	}

	/**
	 * removes all statuses of this class
	 *
	 * @param statusClass status class
	 */
	public <T extends Status> void removeAll(Class<T> statusClass) {
		List<T> interestingStatuses = getAllStatusByClass(statusClass);
		for (Status status : interestingStatuses) {
			remove(status);
		}
	}

	/**
	 * Find if the entity has a specified status
	 * 
	 * @param statusType the status type to check for
	 * @return true, if the entity has status; false otherwise
	 */
	public boolean hasStatus(StatusType statusType) {
		for (Status status : statuses) {
			if (status.getStatusType() == statusType) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add status effect to entity
	 * 
	 * @param status
	 *            Status to be added
	 */
	public void inflictStatus(final Status status) {
		inflictStatus(status, null);
	}

	/**
	 * Add status effect to entity
	 * 
	 * @param status
	 *            Status to be added
	 * @param attacker
	 *            Entity that is inflicting status
	 */
	public void inflictStatus(final Status status, final RPEntity attacker) {
		status.getStatusType().getStatusHandler().inflict(status, this, attacker);
	}

	/**
	 * Check if entity is immune to specified status attack.
	 * 
	 * @param statusType type of status
	 * @return Entity is immune
	 */
	public boolean isImmune(final StatusType statusType) {
		return immunities.contains(statusType);
	}

	/**
	 * Remove any immunity of specified status effect from entity.
	 * 
	 * @param statusType type of status
	 */
	public void removeImmunity(StatusType statusType) {
		RPEntity entity = entityRef.get();
		if (entity == null) {
			return;
		}
		immunities.remove(statusType);
		entity.sendPrivateText("You are not immune to being " + statusType.getName() + " anymore.");
	}

	/**
	 * Make entity immune to a specified status attack.
	 * 
	 * @param attacker
	 *            Status attack type
	 */
	public void setImmune(final StatusAttacker attacker) {
		RPEntity entity = entityRef.get();
		if (entity == null) {
			return;
		}

		final String statusName = attacker.getName();

		// Remove any current instances of the status attribute
		if (entity.has("status_" + statusName)) {
			entity.remove("status_" + statusName);
		}

		// FIXME: should clear any consumable statuses
		attacker.clearConsumables(entity);

		// Add to list of immunities
		immunities.add(attacker.getStatusType());
	}

	/**
	 * activates a status attribute for the client without overriding a potential existing value
	 *
	 * @param attributeName name of attribute
	 */
	void activateStatusAttribute(String attributeName) {
		RPEntity entity = entityRef.get();
		if (entity == null) {
			return;
		}

		// do not override an existing value (e. g. the amount of hp lost by another poison instance)
		if (!entity.has(attributeName)) {
			entity.put(attributeName, 0);
			entity.notifyWorldAboutChanges();
		}
	}








	/**
	 * Poisons the player with a poisonous item. Note that this method is also
	 * used when a player has been poisoned while fighting against a poisonous
	 * creature.
	 *
	 * @param item
	 *            the poisonous item
	 * @return true iff the poisoning was effective, i.e. iff the player is not
	 *         immune
	 */
	public boolean poison(final ConsumableItem item) {
		RPEntity entity = entityRef.get();
		if (entity == null) {
			return false;
		}
		
		if (isImmune(StatusType.POISONED)) {
			return false;
		}

		// Send the client the new status, but avoid overwriting
		// the real value in case the player was already poisoned.
		activateStatusAttribute("poison");
		PoisonStatus status = new PoisonStatus(item.getAmount(), item.getFrecuency(), item.getRegen());
		status.getStatusType().getStatusHandler().inflict(status, this, null);
		if (entity instanceof Player) {
			TutorialNotifier.poisoned((Player) entity);
		}
		return true;
	}

	private static final int COUNT_CHOKING = 5;
	public void eat(final ConsumableItem item) {
		RPEntity entity = entityRef.get();
		if (entity == null) {
			return;
		}

		// Send the client the new status, but avoid overwriting
		// the real value in case the player was already poisoned.
		if (countStatusByType(StatusType.EATING) > COUNT_CHOKING) {
			activateStatusAttribute("choking");
		} else {
			activateStatusAttribute("eating");
		}

		EatStatus status = new EatStatus(item.getAmount(), item.getFrecuency(), item.getRegen());
		status.getStatusType().getStatusHandler().inflict(status, this, null);

		List<String> alcoholicDrinks = Arrays.asList("beer", "pina colada", "wine", "strong koboldish torcibud", "vsop koboldish torcibud");
		if (alcoholicDrinks.contains(item.getName())) {
			DrunkStatus drunkStatus = new DrunkStatus();
			drunkStatus.getStatusType().getStatusHandler().inflict(drunkStatus, this, null);
		}
	}



	public void clear() {
		// TODO: notify handler
		statuses.clear();
	}

	/**
	 * removes a status
	 *
	 * @param status Status to remove
	 */
	public void remove(Status status) {
		status.getStatusType().getStatusHandler().remove(status, this);
	}

	/**
	 * interally adds a status to the list of statuses
	 *
	 * @param status status to add
	 */
	void addInternal(Status status) {
		statuses.add(status);
	}

	/**
	 * internally removes a status from the list of statuses
	 *
	 * @param status status to remove
	 */
	void removeInternal(Status status) {
		statuses.remove(status);
	}

	/**
	 * gets the entity for this StatusList
	 *
	 * @return RPEntity or <code>null</code>
	 */
	RPEntity getEntity() {
		return entityRef.get();
	}


}