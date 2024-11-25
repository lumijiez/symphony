package io.github.lumijiez.raft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Raft {
    private static final Random RANDOM = new Random();

    private static RaftStates STATE = RaftStates.FOLLOWER;
    private static int CURRENT_TERM = 0;
    private static String VOTED_FOR = null;
    private static List<String> LOG = new ArrayList<>();
    private static int COMMIT_INDEX = 0;
    private static int LAST_APPLIED = 0;


    private static final int ELECTION_TIMEOUT = 150 + RANDOM.nextInt(151);

}