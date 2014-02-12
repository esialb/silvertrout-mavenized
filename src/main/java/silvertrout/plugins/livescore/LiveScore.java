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
package silvertrout.plugins.livescore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import silvertrout.Channel;
import silvertrout.User;
import silvertrout.commons.Color;

/**
 *
 * @see silvertrout.Plugin
 * @see silvertrout.plugins
 */
public class LiveScore extends silvertrout.Plugin {

    String channelName = "#sportit";
    ArrayList<FootballGame> games;
    ArrayList<Follower> followers;
    boolean firstTime;

    public LiveScore() {
        games = new ArrayList<FootballGame>();
        followers = new ArrayList<Follower>();
        LiveScoreParser p = new LiveScoreParser();
        games = p.getGames();
        firstTime = true;
    }

    private boolean followThisGame(String following, FootballGame f) {
        String[] split = following.split("=");
        if (split.length < 2) {
            return false;
        }
        if (split.length == 2) {
            if (f.country.contains("Under 21") || f.country.contains("U21") || f.league.contains("Under 21") || f.league.contains("U21") || f.hometeam.contains("U21") || f.awayteam.contains("U21")) {
                return false;
            }
        } else {
            if (!(f.country.contains("Under 21") || f.country.contains("U21") || f.league.contains("Under 21") || f.league.contains("U21") || f.hometeam.contains("U21") || f.awayteam.contains("U21"))) {
                return false;
            }
        }
        if (split[0].contains("team")) {
            return (f.hometeam.contains(split[1]) || f.awayteam.contains(split[1]));
        }
        if (split[0].contains("league")) {
            return f.league.contains(split[1]);
        }
        if (split[0].contains("country")) {
            return f.country.contains(split[1]);
        }
        return false;
    }

    public ArrayList<FootballEvent> getNewEvents(FootballGame newgame, FootballGame oldgame) {
        ArrayList<FootballEvent> oldEvents = oldgame.events;
        ArrayList<FootballEvent> newEvents = newgame.events;
        ArrayList<FootballEvent> updatedEvents = new ArrayList<FootballEvent>();
        if (newgame.gametime.contains("FT") && !oldgame.gametime.contains("FT")) {
            if (newgame.events.isEmpty()) {
                updatedEvents.add(new FootballEvent("Full time", "", "", ""));
            } else {
                updatedEvents = newgame.events;
                updatedEvents.add(new FootballEvent("Full time", "", "", ""));
            }
        } else if (newgame.gametime.contains("HT") && !oldgame.gametime.contains("HT")) {
            if (newgame.events.isEmpty()) {
                updatedEvents.add(new FootballEvent("Halftime", "", "", ""));
            } else {
                updatedEvents = newgame.events;
                updatedEvents.add(new FootballEvent("Half time", "", "", ""));
            }
        } else if (newgame.gametime.contains("'") && oldgame.gametime.contains("HT")) {
            if (newgame.events.isEmpty()) {
                updatedEvents.add(new FootballEvent("Second Half", "", "", ""));
            } else {
                updatedEvents = newgame.events;
                updatedEvents.add(new FootballEvent("Second Half", "", "", ""));
            }
        } else if (newgame.gametime.contains("'") && !oldgame.gametime.contains("'")) {
            if (newgame.events.isEmpty()) {
                updatedEvents.add(new FootballEvent("First Half", "", "", ""));
            } else {
                updatedEvents = newgame.events;
                updatedEvents.add(new FootballEvent("First Half", "", "", ""));
            }
        } //for (FootballEvent newEvent : newEvents) {
        //    if (!oldEvents.contains(newEvent)) {
        //        updatedEvents.add(newEvent);
        //    }
        //}
        else {
            for (int i = 0; i < newEvents.size(); i++) {
                if (!oldEvents.contains(newEvents.get(i))) {
                    if ((newEvents.get(i).yellowcard && !newEvents.get(i).matchtime.contains("F")) || (newEvents.get(i).goal && (!newEvents.get(i).matchtime.contains("F") || !newEvents.get(i).matchtime.contains("H")))) {
                        continue;
                    }
                    updatedEvents.add(newEvents.get(i));
                }
            }
        }

        return updatedEvents;

    }

    public int getCurrentHour() {
        Calendar cal = Calendar.getInstance();
        final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        String[] time = sdf.format(cal.getTime()).split(" ")[1].split(":");
        int hours = Integer.parseInt(time[0]);
        int minutes = Integer.parseInt(time[1]);
        int seconds = Integer.parseInt(time[2]);
        return hours;
    }

    public void onTick(int ticks) {
        if (ticks % 120 == 0 && (getCurrentHour() > 12 || getCurrentHour() < 3)) {
            LiveScoreParser p = new LiveScoreParser();
            ArrayList<FootballGame> newGames = p.getGames();
            ArrayList<FootballGame> updatedGames = new ArrayList<FootballGame>();
            ArrayList<FootballGame> addedGames = new ArrayList<FootballGame>();
            boolean addedGame = true;
            for (FootballGame newGame : newGames) {
                addedGame = true;
                for (FootballGame oldGame : games) {
                    if (oldGame.hometeam.equals(newGame.hometeam) && oldGame.league.equals(newGame.league)) {
                        addedGame = false;
                        ArrayList<FootballEvent> events = getNewEvents(newGame, oldGame);
                        if (!events.isEmpty()) {
                            updatedGames.add(new FootballGame(newGame.country, newGame.league, newGame.hometeam, newGame.awayteam, newGame.gametime, events, newGame.result));
                        } else if (!oldGame.result.equals(newGame.result)) {
                            updatedGames.add(new FootballGame(newGame.country, newGame.league, newGame.hometeam, newGame.awayteam, newGame.gametime, events, newGame.result));
                        }
                    }
                }
                if (addedGame) {
                    addedGames.add(newGame);
                }
            }
            for (FootballGame f : addedGames) {
                for (Follower follower : followers) {
                    HashSet<String> watchlist = follower.getWatchList();
                    for (String following : watchlist) {
                        if (followThisGame(following, f)) {
                            String message = follower.getName() + ": ";//print out changes to follower

                            message += f.gametime + " " + f.hometeam + " - " + f.awayteam;
                            follower.getChannel().sendPrivmsg(message);
                        }
                    }
                }
            }

            games = newGames;

            for (FootballGame updatedGame : updatedGames) {
                for (Follower follower : followers) {
                    HashSet<String> watchlist = follower.getWatchList();
                    for (String following : watchlist) {
                        if (followThisGame(following, updatedGame)) {
                            if (updatedGame.followers == null) {
                                updatedGame.followers = new ArrayList<Follower>();
                            }
                            updatedGame.followers.add(follower);
                        }
                    }
                }
            }
                for (FootballGame updatedGame : updatedGames) {
                    if (updatedGame.followers != null) {
                        String message = "";
                        HashSet<Channel>  channels = new HashSet<Channel>();
                        Channel channel = null;
                        for (Follower follower: updatedGame.followers){
                            message += follower.getName() + " ";
                            //channels.add(follower.getChannel());
                            channel = follower.getChannel();
                        }

                        message += updatedGame.gametime + " " + updatedGame.hometeam + " - " + updatedGame.awayteam + " " + updatedGame.result;
                        channel.sendPrivmsg(message);
                        ArrayList<FootballEvent> events = updatedGame.events;
                        for (FootballEvent event : events) {
                            message = event.matchtime + " ";
                            if (event.yellowcard) {
                                message += Color.yellow(" YELLOW CARD " + event.playername);
                            } else if (event.redcard) {
                                message += Color.red(" RED CARD " + event.playername);
                            } else if (event.goal) {
                                message += Color.green(" GOAL " + event.score + " " + event.playername);
                            }
                            channel.sendPrivmsg(message);
                        }
                    }
                }
            }

    }

    @Override
    public void onPrivmsg(User user, Channel channel,
            String message) {

        if (channel != null) {

            if (message.startsWith("!watchlist")) {
                if (firstTime) {
                    WatchlistKeeper wlk = new WatchlistKeeper();
                    followers = wlk.getWatchlist(channel);
                    firstTime = false;
                }
                String[] splitmess = message.split(" ");
                if (splitmess.length < 2) {
                    channel.sendPrivmsg("Gief parameters!");
                    return;
                }
                if (splitmess[1].equals("add")) {
                    if (splitmess.length < 3) {
                        channel.sendPrivmsg(user.getNickname() + ": Nothing to add?");
                        return;
                    }
                    HashSet<String> watchlist = new HashSet<String>();
                    String addToWatchlist = "";
                    int jump = 0;
                    boolean u21 = false;
                    if (splitmess[2].equals("u21")) {
                        jump++;
                        u21 = true;
                    }
                    for (int i = 2 + jump; i < splitmess.length; i++) {
                        if (i > 2) {
                            addToWatchlist += " ";
                        }
                        addToWatchlist += splitmess[i];
                    }
                    if (u21) {
                        addToWatchlist += "=u21";
                    }
                    Follower fo = null;
                    watchlist.add(addToWatchlist);
                    for (Follower follower : followers) {
                        if (user.getNickname().equals(follower.name)) {
                            fo = follower;
                            break;
                        }
                    }
                    if (fo != null) {
                        fo.watchlist.add(addToWatchlist);
                    } else {
                        followers.add(new Follower(user.getNickname(), channel, watchlist));
                    }
                    for (FootballGame f : games) {
                        for (String following : watchlist) {
                            if (followThisGame(following, f)) {
                                channel.sendPrivmsg(user.getNickname() + ": " + f.gametime + " " + f.hometeam + " - " + f.awayteam + " " + f.result);
                            }
                        }
                    }
                } else if (splitmess[1].equals("remove")) {
                    if (splitmess.length < 3) {
                        channel.sendPrivmsg(user.getNickname() + ": Nothing to remove?");
                        return;
                    }
                    HashSet<String> watchlist = new HashSet<String>();
                    String addToWatchlist = "";
                    for (int i = 2; i < splitmess.length; i++) {
                        if (i > 2) {
                            addToWatchlist += " ";
                        }
                        addToWatchlist += splitmess[i];
                    }
                    Follower fo = null;
                    watchlist.add(addToWatchlist);
                    for (Follower follower : followers) {
                        if (user.getNickname().equals(follower.name)) {
                            fo = follower;
                            break;
                        }
                    }
                    if (fo != null) {
                        if (fo.removeFromWatchlist(watchlist)) {
                            channel.sendPrivmsg(user.getNickname() + ": removed " + addToWatchlist + " from watchlist");
                        } else {
                            channel.sendPrivmsg(user.getNickname() + ": " + addToWatchlist + " did not exist in watchlist");
                        }
                        if (fo.watchlist.isEmpty()) {
                            followers.remove(fo);
                        }
                    } else {
                        channel.sendPrivmsg(user.getNickname() + ": You have no watchlist");
                    }
                } else if (splitmess[1].equals("show")) {
                    Follower fo = null;
                    for (Follower follower : followers) {
                        if (user.getNickname().equals(follower.name)) {
                            fo = follower;
                            break;
                        }
                    }
                    if (fo != null) {
                        channel.sendPrivmsg(user.getNickname() + "s Watchlist:");
                        for (String following : fo.watchlist) {
                            channel.sendPrivmsg(following);
                        }
                    } else {
                        channel.sendPrivmsg(user.getNickname() + ": You have no watchlist");
                    }
                }


            } else if (message.startsWith("!scores")) {
                String[] split = message.split(" ");
                if (split.length < 2) {
                    channel.sendPrivmsg("what team/league/country?");
                    return;
                }
                String addToWatchlist = "";
                int jump = 0;
                boolean u21 = false;
                if (split[1].equals("u21")) {
                    jump++;
                    u21 = true;
                }
                for (int i = 1 + jump; i < split.length; i++) {
                    addToWatchlist += split[i];
                }
                if (u21) {
                    addToWatchlist += "=u21";
                }
                String following = addToWatchlist;

                for (FootballGame updatedGame : games) {
                    if (followThisGame(following, updatedGame)) {
                        String mess = user.getNickname() + ": ";//print out changes to follower

                        mess += updatedGame.gametime + " " + updatedGame.hometeam + " - " + updatedGame.awayteam + " " + updatedGame.result;
                        channel.sendPrivmsg(mess);
                        ArrayList<FootballEvent> events = updatedGame.events;
                        for (FootballEvent event : events) {
                            mess = event.matchtime;
                            if (event.yellowcard) {
                                mess += Color.yellow(" YELLOW CARD " + event.playername);
                            } else if (event.redcard) {
                                mess += Color.red(" RED CARD" + event.playername);
                            } else if (event.goal) {
                                mess += " " + Color.green(event.score) + Color.green(" GOAL " + event.playername);
                            }
                            channel.sendPrivmsg(mess);
                        }
                    }
                }
            } else if (message.startsWith("!save")) {
                WatchlistKeeper wlk = new WatchlistKeeper();
                wlk.saveWatchlist(followers);
            }
        }
    }

    public void onConnected() {
        // Join channel:
        if (!getNetwork().isInChannel(channelName)) {
            getNetwork().getConnection().join(channelName);
        }
    }
}
