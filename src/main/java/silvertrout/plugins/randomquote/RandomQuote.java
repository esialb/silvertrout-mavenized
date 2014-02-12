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
package silvertrout.plugins.randomquote;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.Random;

import silvertrout.Channel;
import silvertrout.User;

/**
 *
 * @author (reggna)
 */
public class RandomQuote extends silvertrout.Plugin {

    @Override
    public void onPrivmsg(User user, Channel channel, String message) {
        if (channel != null) {
            String dir = "jbt/plugins/Logger/" + getNetwork().getNetworkSettings().getName();
            String file = channel.getName().substring(1) + ".log";
            if (message.equals("!random")) {
                try {
                    BufferedReader input = new BufferedReader(new FileReader(new File(dir + "/" + file)));
                    int i;
                    for (i = 0; input.readLine() != null; i++) {
                    }
                    input = new BufferedReader(new FileReader(file));
                    Random r = new Random();
                    i = r.nextInt(i);
                    for (int j = 0; j < i; j++) {
                        input.readLine();
                    }
                    channel.sendPrivmsg(input.readLine());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
