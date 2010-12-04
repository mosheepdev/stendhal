/* $Id$ */
/***************************************************************************
 *                   (C) Copyright 2003-2010 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.events;

import games.stendhal.client.ClientSingletonRepository;
import games.stendhal.client.entity.RPEntity;
import games.stendhal.client.gui.chatlog.HeaderLessEventLine;
import games.stendhal.common.NotificationType;

/**
 * The group has changed (players added, removed, etc)
 *
 * @author hendrik
 */
public class GroupChangeEvent extends Event<RPEntity> {

	/**
	 * executes the event
	 */
	@Override
	public void execute() {
		String message = "Current group status:  leader: " 
			+ event.get("leader") 
			+ "; members: " + event.get("members").replace("\t", ", ");
		ClientSingletonRepository.getUserInterface().addEventLine(
			new HeaderLessEventLine(message, NotificationType.CLIENT));
	}

}
