package com.thewatcher.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

/**
 * Everything the Watcher says on screen and in its notes. {player} = Minecraft name, {user} = the name
 * on this computer, {time} = the real time right now, {day} = today. All filled in locally; nothing is sent anywhere.
 */
@Environment(EnvType.CLIENT)
public final class ScareLines {
    private ScareLines() {}

    public static final String[] GLITCH = {
            "I SEE YOU", "BEHIND YOU", "DON'T TURN AROUND", "{player}", "IT'S IN THE WALLS", "WAKE UP",
            "YOU CAN'T LEAVE", "WHY DID YOU LOOK", "FOUND YOU", "I'M CLOSER NOW", "NOT ALONE", "LET ME IN",
            "{user}", "LOOK UP", "IT KNOWS YOUR NAME", "STOP DIGGING", "THE LIGHTS WON'T HELP",
            "HELLO {user}", "I WAS HERE FIRST", "COUNT THE TREES", "DON'T SLEEP",
            "YOUR DOOR IS OPEN", "WHO IS STANDING BEHIND YOUR CHAIR", "I CAN HEAR YOU BREATHING", "IT'S {time}",
            "THEY ARE ALL INSIDE NOW", "YOU LOOKED", "THERE IS NO SAVE", "SMILE", "STAY STILL", "I'M WEARING YOUR FACE",
    };

    public static final String[] SUBLIMINAL = {
            "HE IS BEHIND YOU", "OPEN YOUR EYES", "LOOK", "NO FACE", "IT WEARS YOUR SKIN", "RUN", "{user}",
            "STAY", "NOW", "CLOSER",
    };

    public static final String[] TYPED = {
            "i've been watching you since you spawned, {player}.",
            "you left the door open.",
            "do you hear it breathing? that isn't you.",
            "it's {time}. you should be asleep, {user}.",
            "the thing you saw in the trees was not a tree.",
            "every time you blink, i get a little closer.",
            "you're not the only player in this world.",
            "i like watching you build. i'll like watching it burn.",
            "don't check the corner of your screen.",
            "it learned your name. {player}. {player}. {player}.",
            "close the game and it will still be here, {user}.",
            "there is something under your bed. in the game, i mean. probably.",
            "you've stopped breathing so quietly. i can hear you, {user}.",
            "i moved your things while you weren't looking. you didn't notice.",
            "turn your volume up. i want you to hear me properly.",
            "the light behind you just went out. no, the real one.",
            "it's {day}. nobody knows where you are right now except me.",
            "when you close your eyes tonight, count to ten. i'll be done by then.",
            "that wasn't lag.",
            "i was in your last world too, {player}. you just didn't look up.",
    };

    /** {file name, text} */
    public static final String[][] NOTES = {
            {"READ_ME.txt", """
                    {user}.

                    I know you can read this.
                    You've been playing for a while now. I've been watching the whole time.
                    Did you think it stays in the game?

                    It's {time}. Your room is very quiet.

                    Don't turn around.
                    """},
            {"i_see_you.txt", """
                    i see you {player}
                    i see you {player}
                    i see you {player}
                    i see you {player}
                    i see you {user}
                    """},
            {"dont_close_the_game.txt", """
                    If you close the game, I can't see you anymore.
                    And if I can't see you, I have to come and look.

                    Keep playing, {user}.
                    """},
            {"it_followed_you.txt", """
                    {day}, {time}

                    It followed you out of the world.
                    It's sitting where your desktop icons are. Behind them.
                    Move them and you'll see it.

                    (don't.)
                    """},
            {"HELLO.txt", """
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    hello {user}
                    why won't you say hello back
                    """},
            {"{player}.txt", """
                    Name: {player}
                    Real name: {user}
                    Last seen: {day}, {time}
                    Status: watched
                    Distance: closer
                    """},
            {"untitled.txt", """
                    It doesn't have a face.
                    It wants yours.
                    """},
    };

    public static String pick(String[] lines, Random r) {
        return fill(lines[r.nextInt(lines.length)]);
    }

    public static String fill(String s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String player = mc.getSession() != null ? mc.getSession().getUsername() : "player";
        String user = System.getProperty("user.name", "you");
        LocalDateTime now = LocalDateTime.now();
        return s.replace("{player}", player)
                .replace("{user}", user)
                .replace("{time}", now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)))
                .replace("{day}", now.format(DateTimeFormatter.ofPattern("EEEE", Locale.US)));
    }
}
