package org.rsbot.script.methods;

import org.rsbot.gui.AccountManager;

/**
 * Selected account information.
 */
public class Account extends MethodProvider {
	public Account(final MethodContext ctx) {
		super(ctx);
	}

	/**
	 * The account name.
	 *
	 * @return The currently selected account's name.
	 */
	public String getName() {
		return methods.bot.getAccountName();
	}

	/**
	 * The account password.
	 *
	 * @return The currently selected account's password.
	 */
	public String getPassword() {
		return AccountManager.getPassword(getName());
	}

	/**
	 * The account pin.
	 *
	 * @return The currently selected account's pin.
	 */
	public String getPin() {
		return AccountManager.getPin(getName());
	}

	/**
	 * The account reward.
	 *
	 * @return The currently selected account's reward.
	 */
	public String getReward() {
		return AccountManager.getReward(getName());
	}
}
