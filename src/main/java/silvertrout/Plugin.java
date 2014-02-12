/*   _______ __ __                    _______                    __   
 *  |     __|__|  |.--.--.-----.----.|_     _|.----.-----.--.--.|  |_ 
 *  |__     |  |  ||  |  |  -__|   _|  |   |  |   _|  _  |  |  ||   _|
 *  |_______|__|__| \___/|_____|__|    |___|  |__| |_____|_____||____|
 * 
 *  Copyright 2008 - Gustav Tiger, Henrik Steen and Gustav "Gussoh" Sohtell
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package silvertrout;

import java.util.Map;

/**
 * Abstract plugin class. All new plugins should inherit this class and 
 * overload the functions they want. As default the on* handlers do nothing.
 *
 * @see silvertrout.Network#loadPlugin
 * @see silvertrout.Network#unloadPlugin
 *
 *
 */
public abstract class Plugin {

    /**
     * Network that loaded the plugin.
     */
    private Network network;

    /**
     * Set Network that loaded the plugin
     * @param network
     */
    protected void setNetwork(Network network) {
        this.network = network;
    }

    /**
     * Return Network that loaded the plugin
     * @return
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * This is the onPart handling function. The function gets called whenever a
     * user parts in one of the channels that are joined.
     *
     * @param  user         User that parted
     * @param  channel      Channel the user parted from
     * @param  partMessage  Part message
     */
    public void onPart(User user, Channel channel, String partMessage) {
    }

    /**
     * This is the onJoin handling function. The function gets called whenever a
     * user joins a channel.
     *
     * @param  user         User that joined
     * @param  channel      Channel the user joined
     */
    public void onJoin(User user, Channel channel) {
    }

    /**
     * This is the onQuit handling function. The function gets called whenever a
     * user quits from IRC.
     *
     * @param  user         User that quit
     * @param  quitMessage  Quit message
     */
    public void onQuit(User user, String quitMessage) {
    }

    // TODO:
    /**
     *
     * @param nick
     * @param channelName
     */
    public void onInvite(User nick, String channelName) {
    }
    /* public void onBan(...) { } */

    /**
     * This is the onKick handling function. The function gets called whenever a
     * operator kicks someone from a channel.
     *
     * @param  user         User that initiated the kick
     * @param  channel      Channel the user was kicked from
     * @param  kicked       User that where kicked
     * @param  kickReason   Reason for the kick / comment
     */
    public void onKick(User user, Channel channel, User kicked,
            String kickReason) {
    }

    /**
     * This is the onPrivmsg handling function. The function gets called whenever
     * a private message is sent to you privetly or to a channel you have joined.
     * <p>
     * NOTE: If channel is NULL then it is a private message directly to you. If
     *       not then the message was written in a channel.
     *
     * NOTE: This function is also called when silvertrout sends a message to a
     *       channel, so if you do not want this check that user isn't yourself
     *
     * @param  user         User that wrote the message
     * @param  channel      Channel the message was written in (if any)
     * @param  message      Message
     */
    public void onPrivmsg(User user, Channel channel, String message) {
    }

    /**
     * This is the onNotice handling function. The function gets called whenever
     * a user notifies a channel or user.
     * <p>
     * NOTE: The IRC protocol clearly states that "automatic replies MUST NEVER
     * be sent in response to a NOTICE message", so don't do that.
     *
     * @param  user         User that parted
     * @param  channel      Channel the user pareted from
     * @param  message      Message
     */
    public void onNotice(User user, Channel channel, String message) {
    }

    /**
     * This is the onTick handling function. The function gets called every
     * second.
     *
     * @param  ticks  The number of seconds since start
     */
    public void onTick(int ticks) {
    }

    /**
     * This is the onTopic handling function. The function gets called every
     * time a topic is changed in a channel.
     * <p>
     * NOTE: You can get the new topic by calling channel.getTopic().
     *
     * @param  user      User that change the topic
     * @param  channel   Channel whose topic was changed
     * @param  oldTopic  The old topic
     */
    public void onTopic(User user, Channel channel, String oldTopic) {
    }

    /**
     * This is the onNick handling function. The function gets called every
     * time a user change its nick.
     * <p>
     * NOTE: You can get the new nick by calling user.getNick().
     *
     * @param  user         User that change its nick
     * @param  oldNickname  The old nickname
     */
    public void onNick(User user, String oldNickname) {
    }

    /*
    TODO: onMode
     */
    // TODO: change from string to User
    /**
     * This is the onPing handling function. The function gets called every
     * time you are pinged.
     *
     * @param id
     */
    public void onPing(String id) {
    }

    /**
     * This is the onLoad handling function. The function gets called every time
     * the plugin is loaded.
     * @param settings
     */
    public void onLoad(Map<String, String> settings) {
    }

    /**
     * This is the onUnload handling function. The function gets called every
     * time the plugin is unloaded.
     */
    public void onUnload() {
    }

    /**
     * This is the onConnected handling function. The function gets called every
     * time we get connected.
     */
    public void onConnected() {
    }

    /**
     * This is the onDisconnected handling function. The function gets called
     * every time we get disconnected.
     */
    public void onDisconnected() {
    }

    /**
     * When giver gives mode (+mode) to receiver
     * @param giver
     * @param channel
     * @param receiver
     * @param mode
     */
    public void onGiveMode(User giver, Channel channel, User receiver, char mode) {
    }

    /**
     * We joined a channel
     * @param c the channel we joined
     */
    public void onJoin(Channel c) {
    }

    /**
     * Private notice from user.
     * Note that user may not be known in network.
     * @param user
     * @param message
     */
    public void onNotice(User user, String message) {
        onNotice(user, null, message);
    }

    /**
     * We parted a channel
     * @param channel the channel we parted
     * @param message our message
     */
    public void onPart(Channel channel, String message) {
        onPart(null, channel, message);
    }

    /**
     * Private message from user received
     * @param user
     * @param message
     */
    public void onPrivmsg(User user, String message) {
        onPrivmsg(user, null, message);
    }
    
    /**
     * Private message was sent from the bot to a user 
     * @param user The user that the message for
     * @param message
     */
    public void onSendmsg(User user, String message) {
    }

    /**
     * When giver takes mode (-mode) from receiver
     * @param giver
     * @param channel
     * @param affectedUser 
     * @param mode
     */
    public void onTakeMode(User giver, Channel channel, User affectedUser, char mode) {
    }
}
