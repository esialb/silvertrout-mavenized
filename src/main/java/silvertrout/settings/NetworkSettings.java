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
package silvertrout.settings;

/**
 *
 * @author Gussoh
 */
public class NetworkSettings {

    /** Network name */
    private String name;
    private String host;
    private int port;
    private String username;
    private String nickname;
    private String realname;
    private String password;
    private String charset;
    private boolean secure;

    /**
     * Create a network setting
     * @param name Name of the network
     * @param host The host of the IRC server
     * @param port The port of the IRC server
     * @param username Username of user
     * @param nickname Nickname
     * @param realname "Real name" or "Ircname"
     * @param password Password for connection, set null if no password
     * @param charset charset of connection, eg. UTF-8 or iso-8859-1
     * @param secure set to true to use SSL connection
     */
    public NetworkSettings(String name, String host, int port, String username, String nickname, String realname, String password, String charset, boolean secure) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.nickname = nickname;
        this.realname = realname;
        this.password = password;
        this.charset = charset;
        this.secure = secure;
    }

    /**
     *
     * @return
     */
    public String getHost() {
        return host;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Get the require password for this connection.
     * If there is no password needed returns null
     * @return password if needed, otherwise null
     */
    public String getPassword() {
        return password;
    }

    /**
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @return
     */
    public String getRealname() {
        return realname;
    }

    /**
     *
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @return
     */
    public String getCharset() {
        return charset;
    }

    /**
     *
     * @return
     */
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String toString() {
        return "Network Settings: \"" + getName() + "\": " + getHost() + ":" + getPort() + ", " + getUsername() + ", " + getNickname();
    }
}
