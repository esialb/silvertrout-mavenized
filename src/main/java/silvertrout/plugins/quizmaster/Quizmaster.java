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
package silvertrout.plugins.quizmaster;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;
import java.util.Calendar;
import java.io.File;
import java.net.URISyntaxException;

import java.net.URL;
import java.util.Collections;

import silvertrout.Channel;
import silvertrout.User;
import silvertrout.Modes;

import silvertrout.commons.game.ScoreManager;
import silvertrout.commons.game.Trophy;
import silvertrout.commons.game.TrophyManager;

/**
 *
 **
 */
public class Quizmaster extends silvertrout.Plugin {

    private enum State { RUNNING, RUNNING_QUESTION, NOT_RUNNING };
	
    // Settings:
    private final int                  voiceInterval        = 60;
    private final int                  hintTime             = 7;
    private final int                  waitTime             = 3;
    private final int                  rankInterval         = 500;
    
    // Variables:
    
    private final LinkedList<Question> questions = new LinkedList<Question>();
    private final Random               rand      = new Random();
    private ScoreManager               scoreManager;
    private TrophyManager              trophyManager;
    private String                     channelName;
        
    private Question                   question;
    //private String                     currentAnswerString;
    private int                        currentHint = 0;

    private String[]                   grad = 
            {
            "\"Tought\"", "Cell", "Egg", "Embryo", "Fetus", "Neonate", 
            "Toddler", "Child", 
            
            "Preschooler", "Lower Primary School Student", 
            "Upper Primary School Student", "Lower Secondary School Student",
            "Upper Secondary School Student", "Bachelor Student", 
            "Master Student", 
            
            "Volunteer", "Intern", "Receptionist", "Personal Secretary", 
            "Personal Assistant", "Clerk", "Executive Secretary", 
            "Executive Assistant", "Foreman", "Supervisor", "Manager",
            "Superintendent",
            
            "Associate Vice President", "Senior Vice President", 
            "Executive Vice President", "Chief Officer", 
            "Chief Executive Officer", "Chairman of the Board",
            
            "Apprentice", "Apprentice-Companion", "Brother",
            "Commander", "Master", "Grand Master"
            };
            

    
    private int                        startTime;
    private int                        currentTime;
    private int                        endTime;
    
    private int                        statTime             = 0;
    
    private int                        unanswerdQuestions   = 0;
    
    private int                        answerStreak         = 0;
    private String                     answerStreakNickname = new String();
    
    private long                       startMiliTime;
    
    private State                      state                = State.NOT_RUNNING;

    /**
     *
     */
    public Quizmaster() {

        String scoresPath   = "Scores/Scores";
        String trophiesPath = "Trophies";

        try {
            scoreManager  = new ScoreManager(new File(this.getClass().getResource(scoresPath).toURI()));
            trophyManager = new TrophyManager(new File(this.getClass().getResource(trophiesPath).toURI()));
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    /** Award trophy to user.
     *
     * @param t
     * @param nick
     */
    public void awardTrophy(Trophy t, String nick) {
        if(!trophyManager.haveTrophy(t, nick)) {
            trophyManager.addTrophy(t, nick);
            getNetwork().getConnection().sendPrivmsg(channelName, nick 
                + ": Du har fått en trofé - " + t.getName());
        }
    }
    
    /**
     *
     * @param score
     */
    public String getRang(int score) {
        int rang = score / rankInterval;
        if(rang < grad.length) {
            return grad[rang];
        } else {
            int umlevel = rang - grad.length + 2;
            return "Über master (level " + umlevel + ")";
        }
    }
    
    /**
     *
     * @param nick
     * @param score
     */
    public void awardScore(String nick, int score) {
        // Calculate answer time, in seconds:
        long miliSec = Calendar.getInstance().getTimeInMillis() - startMiliTime;
        double time  = ((double)miliSec / 1000.0);
        
        // Calculate winning streak
        if(answerStreakNickname.equals(nick)) {
            answerStreak++;
        } else {
            answerStreakNickname = nick;
            answerStreak         = 1;
        }
        
        // Update scores:
        int oldScore = scoreManager.getTotalScore(nick);
        int oldPos   = scoreManager.getPosition(nick);
        scoreManager.addScore(nick, question.category, score);
        int newScore = scoreManager.getTotalScore(nick);
        int newPos   = scoreManager.getPosition(nick);

        // New rank
        if(oldScore / rankInterval < newScore / rankInterval)
        {
            getNetwork().getConnection().sendPrivmsg(channelName,
                    "Utmärkt jobbat! Din nya rank är: " 
                    + getRang(newScore));
        }

        // Print message
        String msg = "Rätt svar var \"" + question.hintLine + "\". ";
        if(answerStreak >= 3)    msg += "(" + answerStreak + " i rad) ";
        if(oldPos == -1)         msg += "(In på listan på placering " + newPos + ") ";
        else if(oldPos < newPos) msg += "(Upp " + (newPos-oldPos) + " placering(ar)) ";
        msg += nick + " (" + time +" sek) fick " + score + "p och har nu " + newScore +"p.";
        getNetwork().getConnection().sendPrivmsg(channelName, msg);

        // Check for trophies won
        int year  = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int day   = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        
        int hour  = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min   = Calendar.getInstance().get(Calendar.MINUTE);
        // First blood trophy
        if(oldScore == 0 && newScore >= 1)
            awardTrophy(trophyManager.getTrophy("First Blood"), nick);
        // Speedster trophy
        if(time < 3.0 && question.hintLine.length() > 5)
            awardTrophy(trophyManager.getTrophy("Speedster"), nick);
        // Chain Reaction
        if(answerStreak >= 5)
            awardTrophy(trophyManager.getTrophy("Chain Reaction"), nick);
        // Chain Overload
        if(answerStreak >= 10)
            awardTrophy(trophyManager.getTrophy("Chain Overload"), nick);
        // Chain Overdose
        if(answerStreak >= 30)
            awardTrophy(trophyManager.getTrophy("Chain Overdose"), nick);
        // Elite!
        if(oldScore < 1337 && newScore >= 1337)
            awardTrophy(trophyManager.getTrophy("Elite!"), nick);
        // Top Ten
        if(newScore > 100 && newPos <= 10)
            awardTrophy(trophyManager.getTrophy("Top Ten"), nick);
        // Top Three
        if(newScore > 300 && newPos <= 3)
            awardTrophy(trophyManager.getTrophy("Top Three"), nick);
        // Top Dog
        if(newScore > 1000 && newPos == 1)
            awardTrophy(trophyManager.getTrophy("Top Dog"), nick);
        // Säg ett datum, vilket som helst!
        if(month == 5 && day == 29)
            awardTrophy(trophyManager.getTrophy("Säg ett datum, vilket som helst!"), nick);
        // Endurance Master
        if(question.hintLine.length() >= 30)
            awardTrophy(trophyManager.getTrophy("Endurance Master"), nick);
    }

    /**
     *
     * @param categories
     */
    public void newRound(java.util.Collection<String> categories) {
        try{
            File qdir = new File(this.getClass().getResource("Questions").toURI());

            for(File d: qdir.listFiles()) {
                //System.out.println("Begin checking directory: " +d.getName());
                if(categories == null || categories.contains(d.getName())) {
                    if(d.isDirectory()) {
                        //System.out.println("Checking directory: " +d.getName());
                        for(File f: d.listFiles()) {
                            if(f.getName().endsWith(".xml")) {
                                System.out.println("Loading questions from " + f.getName());
                                Collection<Question> qss = QuestionReader.load(f);
                                questions.addAll(qss);
                								// TODO: FIX CRASH ON GRAMMAR ERROR: INSTEAD
                                // REPORT ERROR!!
                                //System.out.println("Added file: " + f.getName());
                            }
                        }
                    }
                }
            }
            // Suffle the questions
            Collections.shuffle(questions);
            
            getNetwork().getConnection().sendPrivmsg(channelName, "En ny omgång"
                    + " startas. Totalt finns " + questions.size() + " frågor.");
            
            unanswerdQuestions = 0;
            state              = State.RUNNING;
            newQuestion();
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    /**
     *
     */
    public void endRound() {
        getNetwork().getConnection().sendPrivmsg(channelName, "Omgången är"
                + " slut. Skriv !start för att starta en ny omgång.");
        state = State.NOT_RUNNING;
    }
    
    /**
     *
     */
    public void newQuestion() {
        // Pop of question from queue
        try {
            question = questions.removeFirst();
        } catch(java.util.NoSuchElementException e) {
            endRound();
            return;
        }
        // Construct hint line from answers (if there is none)
        if(question.hintLine == null) {
            int requiredAnswers = 0;
            question.hintLine = "";
            for(Question.Answer answer: question.answers) {
                if(answer.required)requiredAnswers++;
            }
            for(Question.Answer answer: question.answers) {
                if(answer.required) {
                    question.hintLine += answer.answer + " ";
                } else if(requiredAnswers >= 0) {
                    requiredAnswers--;
                    question.hintLine += answer.answer + " ";
                }
            }
            question.hintLine = question.hintLine.trim();
        }

        // Construct hints from hint line (if there are hints missing)
        // TODO, improve
        int generate = question.hintCount - question.hints.size();
        //System.out.println("Generating " + generate + " questions (-1, all dots)");
        if(generate > 0) {
            int chars = 0;
            String base = new String();

            for(int i = 0; i < question.hintLine.length(); i++) {
                if(Character.isLetterOrDigit(question.hintLine.charAt(i))) {
                    chars++;
                    base += '.';
                } else {
                    base += question.hintLine.charAt(i);
                }
            }
            //System.out.println("hintline contains " + chars + " chars");
            // Add base (only dots)
            Question.Hint h = question.new Hint();
            h.hint = base;
            question.hints.add(h);
            generate--;

            final double percentage = 0.70;

            double reveal     = ((double)chars * percentage) / (double)generate;
            double revealLeft = 0;
            //System.out.println("revealing " + reveal + ", r+rl: " + (reveal + revealLeft));
            for(int g = 0; g < generate; g++) {
                for(int r = 1, b = 0; r < reveal + revealLeft && b < 100;) {
                    int index = rand.nextInt(question.hintLine.length());
                    if(base.charAt(index) == '.') {
                        base = base.substring(0, index) + question.hintLine.charAt(index) + base.substring(index + 1);
                        r++;
                    }
                    b++;
                }
                
                if(reveal + revealLeft > 1) {
                    Question.Hint hi = question.new Hint();
                    hi.hint     = base;
                    question.hints.add(hi);
                    //System.out.print("(Added) ");
                }
                //System.out.print(base + " - old left " + revealLeft + ", reveal " + reveal);
                revealLeft = reveal + revealLeft - Math.floor(reveal + revealLeft);
                //System.out.println(" - new left " + revealLeft);
            }
            //System.out.println("y");
        }
        //System.out.println("x");
        currentHint   = 0;
        startTime     = currentTime;
        startMiliTime = Calendar.getInstance().getTimeInMillis();
        getNetwork().getConnection().sendPrivmsg(channelName, "" + "[" 
                + question.category + "] " + question.questionLine);
        state   = State.RUNNING_QUESTION;
    }
    
    
    /**
     *
     * @param answered - true iff the question was correctly answered
     */
    public void endQuestion(boolean answered) {
        if(!answered) {
            getNetwork().getConnection().sendPrivmsg(channelName, "Rätt svar"
                    + " var \"" + question.hintLine + "\". Ingen"
                    + " lyckades svara rätt.");
            unanswerdQuestions++;
            answerStreak = 0;
        } else {
            unanswerdQuestions = 0;
        }
        endTime = currentTime;
        state   = State.RUNNING;
    }
    
    
    public void printTopTen(String sender) {
        ScoreManager.Score[] topList      = scoreManager.getTop(10);
        String               toptenString = new String();

        if(topList.length == 0) {
            toptenString  = "Ingen är på listan än. Quizza hårdare!";
        }
        
        for(int i = 0; i < topList.length; i++) {
            toptenString += "#" + (i+1) + " " + topList[i].nick + " " 
                + topList[i].getTotalScore() + "p";
            if(i != topList.length - 1)toptenString += "  -  ";
        }
        
        getNetwork().getConnection().sendPrivmsg(channelName, 
                "Top 10: " + toptenString);
    }
    
    /**
     *
     * @param sender
     */
    public void printStats(String sender, String lookup) {
        
        int          scorePos   = scoreManager.getPosition(lookup);
        int          score      = scoreManager.getTotalScore(lookup);

        int          trophyTot  = trophyManager.getTrophies().size();
        List<Trophy> trophyList = trophyManager.getTrophies(lookup);
        int          trophy     = trophyList.size();
        
        String       msg        = sender + ", ";
        
        boolean      nickSame   = sender.equalsIgnoreCase(lookup);
        String       nickSing   = nickSame ? "Du":  lookup;
        String       nickPlur   = nickSame ? "Din": lookup.endsWith("s") ? lookup: lookup + "s";
        
        // Score part
        if(scorePos == -1) {
            msg += nickSing + " har inga poäng, och  är inte med på topplistan.  ";
        } else {
            msg += nickSing + " ligger på plats " + scorePos + ", med " + score + "p.  ";
        }
        
        // Trophy part
        if(trophy == 0) {
            msg += nickSing + " har inga troféer. ";
        } else {
            msg += nickSing + " har " + trophy + " av " + trophyTot + " troféer: ";
            
            for(int i = 0; i < trophy; i++) {
                msg += trophyList.get(i).getName();
                if(i == trophy - 2) {
                    msg += " och ";
                } else if(i < trophy - 2) {
                    msg += ", ";
                }
            }
        }
        
        // Rank part
        int nextrank = rankInterval - score % rankInterval;
        msg += ". " + nickPlur + " rank är: " + getRang(score) + " (" 
                + nextrank + "p kvar till nästa rank).";
        
        getNetwork().getConnection().sendPrivmsg(channelName, msg);
    }
    
    void checkAnswer(User user, Channel channel, String message) {
        // Answer to question
        String uanswer      = message.toLowerCase().trim();
        int    uanswerCount = 0;
        int    score        = 0;
        for(Question.Answer answer: question.answers) {

            String cans = answer.answer.toLowerCase();
            int pos = uanswer.indexOf(cans);
            char before = pos <= 0 ? ' ': uanswer.charAt(pos - 1);
            char after  = pos + cans.length() >= uanswer.length() ? ' ': uanswer.charAt(pos + cans.length());
            
            System.out.println(cans + "/" + uanswer + " = " + pos + ", " + before + ", " + after);
            
            if(pos >= 0 && !Character.isLetterOrDigit(before) && 
                        !Character.isLetterOrDigit(after)) { 
                score += answer.score;
                uanswerCount++;
                
            } else {
                if(answer.required) {
                    //getNetwork().getConnection().sendPrivmsg(channelName, 
                    //      "missing req answer: " + answer.answer);
                    return;
                }
            }
        }
        if(uanswerCount >= question.required) {
            for(int i = 0; i < currentHint; i++)
               score -= question.hints.get(i).scoredec;
            awardScore(user.getNickname(), Math.max(1, score));
            endQuestion(true);

        } else {
            //getNetwork().getConnection().sendPrivmsg(channelName, 
            //      "req answer " + question.required + " > " + uanswerCount);
        }

    }
    
    @Override
    public void onPrivmsg(User user, Channel channel, String message) {


        if(channel != null && channel.getName().equalsIgnoreCase(channelName)) {

            if(state == State.RUNNING_QUESTION) {
                checkAnswer(user, channel, message);
            } else if(state == State.NOT_RUNNING) {
                // Start new round
                // TODO: what about categories?
                if(message.startsWith("!start")) {
                    String[] cat = message.substring(6).split("\\s");
                    newRound(null);
                }
            }
            
            if(message.startsWith("!stats")) {
                String[] parts = message.substring(1).split("\\s");
                if(currentTime - statTime > 20) {
                    if(parts.length == 2) {
                        printStats(user.getNickname(), parts[1]);
                    } else {
                        printStats(user.getNickname(), user.getNickname());
                    }
                    statTime = currentTime;
                }
            }
            if(message.equals("!toplist")) {
                if(currentTime - statTime > 20) {
                    printTopTen(user.getNickname());
                    statTime = currentTime;
                }
            }
            else if(message.equals("!help")) {
                if(currentTime - statTime > 20)
                    getNetwork().getConnection().sendPrivmsg(channelName, 
                              user.getNickname()
                            + ", Skriv !start för att starta frågesproten och "
                            + "!toplist för att se tio i topp-listan. För att "
                            + "se dina egna poäng och titta på dina troféer, "
                            + "skriv !stats. Du kan rapportera felaktiga "
                            + "frågor genom att skriva !report num [reason]."
                            + "Om du vill visa denna hjälp igen, skriv !help.");
            }
            else if(message.startsWith("!suggest")) {
                // TODO!
            }
            else if(message.startsWith("!report")) {
                getNetwork().getConnection().sendPrivmsg(channelName,
                    user.getNickname() + ", Not implemented yet");
                // TODO!
            }
            else if(message.equals("!trophies")) {
                        
                int tot    = trophyManager.getTrophies().size();
                String msg = user.getNickname() + ", Följande troféer finns: ";
                for(Trophy t: trophyManager.getTrophies()) {
                    msg += t.getName() + ", ";
                }
                msg += tot + " stycken - samla alla!";
                getNetwork().getConnection().sendPrivmsg(channelName, msg);
            }
        }
    }
    
    /**
     *
     */
    public void giveHint() {

        if(currentHint == question.hints.size()) {
            //getNetwork().getConnection().sendPrivmsg(channelName, "err, to few hints");
            endQuestion(false);
        } else {
            Question.Hint currentHintObj = question.hints.get(currentHint);
            getNetwork().getConnection().sendPrivmsg(channelName, currentHintObj.hint);
            currentHint++;
        }

    }
    
    @Override
    public void onTick(int ticks) {
        currentTime = ticks;
        //System.out.println(currentTime + ": " + state);
        if(state == State.RUNNING_QUESTION) {        
            // If we have a question that no one have answered in a while
            if(currentTime > startTime + hintTime * question.hintCount) {
                endQuestion(false);
                if(unanswerdQuestions >= 5) { 
                    endRound();
                } else {
                    newQuestion();
                }
            // Or if it is time to give a hint
            } else if(currentTime - startTime == hintTime * question.hintCount) {
                endQuestion(false);
            } else if((currentTime - startTime) % hintTime == 0) {
                giveHint();
            }         
        
        } else if(state == State.RUNNING) {
            // Time for a new question
            if(currentTime - endTime == waitTime) {
                newQuestion();
            }            
        }
        
        // Do every minute
        if(ticks % voiceInterval == 0) {
            // Only voice if we are in the channel and are an operator
            if(getNetwork().isInChannel(channelName)) {
            
                Channel channel  = getNetwork().getChannel(channelName);
                User    myUser   = getNetwork().getMyUser();
                boolean operator = channel.getUsers().get(myUser).haveMode('o');
                
                if(operator) {

                    LinkedList<String> voice   = new LinkedList<String>();
                    LinkedList<String> devoice = new LinkedList<String>();                    
                    Map<User, Modes>   users   = channel.getUsers();
                    System.out.println("\n\n\nVoicing people!:");
                    for(User u: users.keySet()) {
                        
                        System.out.println(u);
                        System.out.println(users.get(u));
                          
                        if(users.get(u).haveMode('v')){
                            // the user do have voice
                            if(!scoreManager.isTop(10, u.getNickname()))
                                devoice.add(u.getNickname());
                        } else {
                            // the user do not have voice
                            if(scoreManager.isTop(10, u.getNickname()))
                                voice.add(u.getNickname());
                        }
                    }
                    
                    // TODO: combine or something. Move to the silvertrout 
                    // mode thingy?
                    while(!voice.isEmpty()) {
                        String mode  = "+";
                        String usrs = "";
                        for(int i = 0; !voice.isEmpty() && i < 4; i++) {
                            mode  += "v";
                            usrs += voice.pop() + " ";
                        }
                        getNetwork().getConnection().sendRaw("MODE " + 
                                channelName + " " + mode + " " + usrs);
                        System.out.println("MODE " + 
                                channelName + " " + mode + " " + usrs);
                    }
                    
                    while(!devoice.isEmpty()) {
                        String mode  = "-";
                        String usrs = "";
                        for(int i = 0; !devoice.isEmpty() && i < 4; i++) {
                            mode  += "v";
                            usrs += devoice.pop() + " ";
                        }
                        getNetwork().getConnection().sendRaw("MODE " + 
                                channelName + " " + mode + " " + usrs);
                        System.out.println("MODE " + 
                                channelName + " " + mode + " " + usrs);
                    }

                }
            }
        }
    }
    
    private String printNick(String nick){

        int s = scoreManager.getTotalScore(nick) / rankInterval;
        if(s > 0) {
            if(s > grad.length)
                return grad[grad.length-1] + " " + nick;
            else if(s > 0)
                return grad[s-1] + " " + nick;
        }
        return nick;
    }
    
    @Override
    public void onConnected() {
        // Join quiz channel:
        if(!getNetwork().isInChannel(channelName)) {
            getNetwork().getConnection().join(channelName);
        }
    }
    
    @Override
    public void onDisconnected() {
        state = State.NOT_RUNNING;
    }    
    
    @Override
    public void onLoad(Map<String,String> settings){
        channelName = settings.get("channel");
        if(channelName == null || !channelName.startsWith("#")) channelName = "#superquiz";
    }
}
