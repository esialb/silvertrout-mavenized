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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Gussoh
 */
public class IRCConnection {

    /**
     *
     */
    protected Network network;
    /**
     *
     */
    protected Socket socket;
    private   SenderThread senderThread;
    private   ReceiverThread receiverThread;
    private   static final int FLOOD_BURST = 5;
    private   static final int FLOOD_MS_PER_MSG = 200;
    private   final AtomicBoolean disconectionNotificationSent = new AtomicBoolean(false);

    /**
     * Create a new IRCConnection and connect to the server specified in 
     * network.getNetworkSettings()
     *
     * @param   network
     * @throws  java.io.IOException
     */
    public IRCConnection(Network network) throws IOException {
        this.network = network;
        System.err.println("connection..");
        connect();
        System.err.println("setup..");
        setupConnection();
    }

    /**
     * Setup the connection, register and start the threads
     * 
     * @throws java.io.IOException
     */
    protected void connect() throws IOException {
        System.err.println(" - Setting up socket");
        socket = new Socket(network.getNetworkSettings().getHost(), 
                network.getNetworkSettings().getPort());
    }

    /**
     * Register and create the threads
     * @throws java.io.IOException
     */
    private void setupConnection() throws IOException {
        System.err.println(" - Creating reader");
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), 
                network.getNetworkSettings().getCharset()));
        System.err.println(" - Creating writer");
        Writer writer = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(), 
                network.getNetworkSettings().getCharset()));

        // Do registration before we start any threads. This will enable some 
        // more notifications of connection failiures.
        System.err.println(" - Registering user");
        register(writer);

        // No error yet, probably means the connection was successful, 
        // start threads!
        System.err.println(" - Starting sender thread");
        senderThread = new SenderThread(writer);
        senderThread.start();
        System.err.println(" - Starting receiver thread");
        receiverThread = new ReceiverThread(reader);
        receiverThread.start();
    }

    /**
     * Register silvertrout as a user on the IRC network. This 
     *
     * TODO: No check for what happens if we get a nickname that is already
     *       in use. This should be supported somehow.
     *
     * @param writer
     * @throws java.io.IOException
     */
    protected void register(Writer writer) throws IOException {
        if (network.getNetworkSettings().getPassword() != null) {
            writer.write("PASS " + network.getNetworkSettings().getPassword() 
                    + "\r\n");
        }
        writer.write("NICK " + network.getMyUser().getNickname() + "\r\n");
        writer.write("USER " + network.getMyUser().getUsername() + " 0 * :" 
                + network.getNetworkSettings().getRealname() + "\r\n");
        writer.flush();
    }

    /**
     * Kick a user
     * @param channel
     * @param user
     * @param reason
     */
    public void kick(Channel channel, User user, String reason) {
        kick(channel.getName(), user.getNickname(), reason);
    }

    /**
     * /me is coding
     * @param channel
     * @param action
     */
    public void sendAction(Channel channel, String action) {
        sendAction(channel.getName(), action);
    }

    /**
     * Send a message to a channel
     * @param channel
     * @param message
     */
    public void sendPrivmsg(Channel channel, String message) {
        sendPrivmsg(channel.getName(), message);
    }

    /**
     * Send a private message to a user
     * @param user
     * @param message
     */
    public void sendPrivmsg(User user, String message) {
        sendPrivmsg(user.getNickname(), message);
    }
    
    /**
     * Send a notice to a user
     * @param user
     * @param message
     */
    public void sendNotice(User user, String message){
    	sendNotice(user.getNickname(), message);
    }

    /**
     * Set mode of user in channel
     * @param channel
     * @param user
     * @param mode
     */
    public void setMode(Channel channel, User user, String mode) {
        sendRaw("MODE " + channel.getName() + " " + mode + " " + user.getNickname());
    }

    /**
     * Leave a channel
     * @param channel
     */
    public void part(Channel channel) {
        part(channel.getName());
    }

    /**
     * Notify network that we have been disconnected
     * Regardless of how many invokations we have of this method,
     * the notification will/should only be sent once!
     * This method is thread safe.
     */
    private void notifyDisconnect() {
        if (!disconectionNotificationSent.getAndSet(true)) { // only do once!
        
            try {
            senderThread.close();
            receiverThread.close();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            network.getWorkerThread().invokeLater(new Runnable() {

                @Override
                public void run() {
                    network.onDisconnect();
                }
            });
        }
    }

    /**
     * Force a close on the connection.
     *
     * I don't know when to use this. Might be good for something... :)
     * Difference between this and close is that close waits for the current 
     * transmission if finshed (if there is any)
     */
    public void forceClose() {
        try {
            socket.close();
        } catch (IOException e) {
        }

        notifyDisconnect();
    }

    /**
     * Close a connection without sending QUIT
     * Waits for current transmission to finish but does not wai for output queue to finish
     * This method is thread safe.
     */
    public void close() {
        try {
            senderThread.close();
            receiverThread.close();
            socket.close();
        } catch (InterruptedException e) {
        } catch (IOException e) {
        }

        notifyDisconnect();
    }

    /**
     * Send quit to network
     */
    public void quit() {
        sendRaw("QUIT");
    }

    /**
     * Send a quit with a message
     * @param message
     */
    public void quit(String message) {
        sendRaw("QUIT " + message);
    }

    /**
     * Send an action to either a user or a channel. Trigged on the command /me action
     *
     * @param to - The nick/name of the user/channel to send to
     * @param message - The action the user (you) are performing
     */
    public void sendAction(String to, String message) {
        sendPrivmsg(to, "ACTION " + message + "");
    }

    /**
     * Send a private message to either a user or a channel.
     * @param to The nick/name of the user/channel to send to
     * @param message The string to send
     */
    public synchronized void sendPrivmsg(String to, String message) {
        sendRaw("PRIVMSG " + to + " :" + message);
        network.onPrivmsg(network.getMyUser().getNickname(), to, message);
    }
    
    /**
     * Sends a notice to a user.
     * @param to The nick to sent to
     * @param message The message to send
     */
    public synchronized void sendNotice(String to, String message) {
    	sendRaw("NOTICE " + to + " :" + message);
    }

    /**
     * Kick a user from a channel
     *
     * @param channel - The name of the Channel
     * @param who - The nick of the user to kick from the channel
     * @param message - The reason why the user is kicked
     */
    public void kick(String channel, String who, String message) {
        sendRaw("KICK " + channel + " " + who + " :" + message);
    }

    /**
     * Sets the topic of a channel
     * @param channel The channel to change
     * @param topic The new topic
     */
    void changeTopic(Channel channel, String topic) {
        sendRaw("TOPIC " + channel.getName() + " " + topic);
    }

    /**
     * Join a channel with the specified name
     *
     * @param channel - The name of the channel to join
     */
    public void join(String channel) {
        sendRaw("JOIN " + channel);
    }

    /**
     * Join a channel with the specified name
     *
     * @param channel - The name of the channel to join
     * @param password the password of the channel, null of not exist
     */
    public void join(String channel, String password) {
        if(password == null) join(channel);
        else sendRaw("JOIN " + channel + " " + password);
    }

    /**
     * Part from a channel with the specified name
     *
     * @param channel - The name of the channel to part from
     */
    public void part(String channel) {
        sendRaw("PART " + channel);
    }

    /**
     *Send a raw message to the network.
     *
     * @param message - the string to send to the Network
     */
    public void sendRaw(String message) {
        System.out.println("OUT -> | " + message);
        senderThread.sendRaw(message);
    }

    private class SenderThread extends Thread {

        private final Semaphore outputQueueSempaphore = new Semaphore(0);
        private final ConcurrentLinkedQueue<String> outputQueue = new ConcurrentLinkedQueue<String>();
        private final Writer writer;
        private final AtomicBoolean close = new AtomicBoolean(false);
        private final Semaphore closingSemaphore = new Semaphore(0);
        private final Semaphore floodProtectionSemaphore = new Semaphore(FLOOD_BURST);
        private final Timer floodTimer = new Timer("Flood timer");

        public void sendRaw(String message) {
            outputQueue.add(message + "\r\n");
            outputQueueSempaphore.release();
        }

        private SenderThread(Writer writer) {
            this.writer = writer;

            floodTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    if (floodProtectionSemaphore.availablePermits() < FLOOD_BURST) {
                        floodProtectionSemaphore.release();
                    }
                }
            }, 0, FLOOD_MS_PER_MSG);
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    // Make this aquire first to not enable more burst messages 
                    // than intended!
                    outputQueueSempaphore.acquire();
                    // If this is done first the floodTimer might have time to
                    // add one more permit before an actual sending
                    floodProtectionSemaphore.acquire();
                    
                    writer.write(outputQueue.poll());
                    writer.flush();
                } catch (Exception ex) {
                    if (!close.get()) {
                        Logger.getLogger(IRCConnection.class.getName()).log(Level.SEVERE, null, ex);
                        notifyDisconnect();
                    } else {
                        try {
                            writer.close();
                        } catch (IOException ex1) {
                        } finally {
                            closingSemaphore.release(); // let close return
                        }
                    }
                }
            }
        }

        private void close() throws InterruptedException {
            if (!close.getAndSet(true)) { // only call once!
                interrupt();
                closingSemaphore.acquire(); // wait for writer to close.
                floodTimer.cancel(); // why not do it here? doesn't matter I guess
            }
        }
    }

    private class ReceiverThread extends Thread {

        private final BufferedReader reader;
        private AtomicBoolean close = new AtomicBoolean(false);
        private final Semaphore closingSemaphore = new Semaphore(0);

        private ReceiverThread(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    String line = reader.readLine();
                    if(line == null) {
                      throw new IOException("Socket closed: end of stream reached in bufferreader");
                    } if(network.getWorkerThread() != null) {
                      network.getWorkerThread().process(new Message(line));
                    }
                } catch (Exception ex) {
                    if (!close.get()) {
                        Logger.getLogger(IRCConnection.class.getName()).log(Level.SEVERE, null, ex);
                        notifyDisconnect();
                        return;
                    } else {
                        try {
                            reader.close();
                        } catch (IOException ex1) {
                        } finally {
                            closingSemaphore.release(); // let close return
                            return;
                        }
                    }
                }
            }
        }

        private void close() throws InterruptedException {
            if (!close.getAndSet(true)) { // only call once!
                interrupt();
                closingSemaphore.acquire(); // wait for reader to close
            }
        }
    }
}
