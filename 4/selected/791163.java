package org.chessworks.uscl.services.file;

import java.io.File;
import java.util.Collection;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.chessworks.uscl.model.Game;
import org.chessworks.uscl.model.Player;
import org.chessworks.uscl.model.Team;

public class TestFileTournamentService extends TestCase {

    private final File PLAYERS_FILE = new File("src/test/resources/org/chessworks/uscl/services/file/Players.txt");

    private final File SCHEDULE_FILE = new File("src/test/resources/org/chessworks/uscl/services/file/Games.txt");

    private final File TEAMS_FILE = new File("src/test/resources/org/chessworks/uscl/services/file/Teams.txt");

    private FileTournamentService service;

    private FileTournamentService service2;

    private FileTournamentService service3;

    private FileTournamentService service4;

    private File playersFile;

    private File scheduleFile;

    private File teamsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        playersFile = File.createTempFile("Players.", ".txt");
        scheduleFile = File.createTempFile("Games.", ".txt");
        teamsFile = File.createTempFile("Teams.", ".txt");
        FileUtils.copyFile(PLAYERS_FILE, playersFile);
        FileUtils.copyFile(SCHEDULE_FILE, scheduleFile);
        FileUtils.copyFile(TEAMS_FILE, teamsFile);
        playersFile.deleteOnExit();
        scheduleFile.deleteOnExit();
        teamsFile.deleteOnExit();
        service = new FileTournamentService();
        service.setPlayersFile(playersFile);
        service.setScheduleFile(scheduleFile);
        service.setTeamsFile(teamsFile);
        service2 = new FileTournamentService();
        service2.setPlayersFile(playersFile);
        service2.setScheduleFile(scheduleFile);
        service2.setTeamsFile(teamsFile);
        service3 = new FileTournamentService();
        service3.setPlayersFile(playersFile);
        service3.setScheduleFile(scheduleFile);
        service3.setTeamsFile(teamsFile);
        service4 = new FileTournamentService();
        service4.setPlayersFile(playersFile);
        service4.setScheduleFile(scheduleFile);
        service4.setTeamsFile(teamsFile);
    }

    public void testClearSchedule() throws Exception {
        service.load();
        Player duckstorm = service.findPlayer("DuckStorm");
        Game board = service.findPlayerGame(duckstorm);
        assertNotNull(board);
        assertEquals(90, board.boardNumber);
        service.clearSchedule();
        board = service.findPlayerGame(duckstorm);
        assertNull(board);
        duckstorm = service.findPlayer("DuckStorm");
        assertNotNull(duckstorm);
        Team icc = service.findTeam("ICC");
        assertNotNull(icc);
    }

    public void testFindPlayerGame() throws Exception {
        service.load();
        Player duckstorm = service.findPlayer("DuckStorm");
        Game board = service.findPlayerGame(duckstorm);
        assertNotNull(board);
        assertEquals(90, board.boardNumber);
    }

    public void testFindAllGames() throws Exception {
        service.load();
        Collection<Game> games = service.findAllGames();
        assertFalse(games.isEmpty());
        service.clearSchedule();
        games = service.findAllGames();
        assertTrue(games.isEmpty());
    }

    public void testScheduleGame() throws Exception {
        service.load();
        service.clearSchedule();
        Player duckstorm = service.findPlayer("DuckStorm");
        assertNotNull(duckstorm);
        Player mrbob = service.findPlayer("MrBob");
        assertNotNull(duckstorm);
        Game board = service.findPlayerGame(duckstorm);
        assertNull(board);
        service.scheduleGame(80, duckstorm, mrbob);
        board = service.findPlayerGame(duckstorm);
        assertNotNull(board);
        assertEquals(80, board.boardNumber);
        board = service.findPlayerGame(mrbob);
        assertNotNull(board);
        assertEquals(80, board.boardNumber);
        service.save();
        service2.load();
        board = service2.findPlayerGame(duckstorm);
        assertNotNull(board);
        assertEquals(80, board.boardNumber);
        board = service2.findPlayerGame(mrbob);
        assertNotNull(board);
        assertEquals(80, board.boardNumber);
    }

    public void testSchedule() throws Exception {
    }

    public void testCreatePlayer() throws Exception {
        service.load();
        Team icc = service.findTeam("ICC");
        assertNotNull(icc);
        assertEquals("ICC", icc.getTeamCode());
        assertEquals("Internet Chess Club", icc.getLocation());
        Player test = service.findPlayer("TestPlayer-ICC");
        assertNull(test);
        Player test1 = service.createPlayer("TestPlayer-ICC");
        assertNotNull(test1);
        Player test2 = service.findPlayer("TestPlayer-ICC");
        assertNotNull(test2);
        assertSame(test1, test2);
        assertEquals("TestPlayer-ICC", test1.getHandle());
        assertEquals(icc, test1.getTeam());
        test1.setRealName("Test1");
        service.save();
        test1.setRealName("Test2");
        service2.load();
        Player test3 = service2.findPlayer("TestPlayer-ICC");
        assertNotNull(test3);
        assertEquals("Test1", test3.getRealName());
    }

    public void testPlayerInTwoGames() throws Exception {
    }

    public void testCreateTeam() throws Exception {
    }

    public void testRemovePlayer() throws Exception {
    }

    public void testRemoveTeam() throws Exception {
        service.load();
        Team icc1 = service.findTeam("ICC");
        assertNotNull(icc1);
        Player player1a = service.findPlayer("DuckStorm");
        Player player1b = service.findPlayer("MrBob");
        assertNotNull(player1a);
        assertNotNull(player1b);
        service.removeTeam(icc1);
        service.save();
        service2.load();
        Team icc2 = service2.findTeam("ICC");
        assertNull(icc2);
        Player player2a = service2.findPlayer("DuckStorm");
        Player player2b = service2.findPlayer("MrBob");
        assertNull(player2a);
        assertNull(player2b);
        icc2 = service2.createTeam("ICC");
        assertNotNull(icc2);
        player2a = service2.createPlayer("DuckStorm", icc2);
        player2b = service2.createPlayer("MrBob", icc2);
        assertNotNull(player2a);
        assertNotNull(player2b);
        service2.save();
        service3.load();
        assertNotNull(icc1);
        Player player3a = service3.findPlayer("DuckStorm");
        Player player3b = service3.findPlayer("MrBob");
        assertNotNull(player3a);
        assertNotNull(player3b);
        service.save();
    }

    public void testFindAllPlayers() throws Exception {
        service.load();
        Collection<Player> players = service.findAllPlayers();
        for (Player p : players) {
            if (p.getHandle().equals("DuckStorm")) {
                return;
            }
        }
        fail();
    }

    public void testFindAllTeams() throws Exception {
        service.load();
        Collection<Team> teams = service.findAllTeams();
        for (Team t : teams) {
            if (t.getLocation().contains("Carolina")) {
                return;
            }
        }
        fail();
    }

    public void testFindPlayer() throws Exception {
        service.load();
        Player player = service.findPlayer("aramirez-DaL");
        assertEquals("ARamirez-DAL", player.getHandle());
        assertEquals("Alejandro Ramirez", player.getRealName());
    }

    public void testFindTeam() throws Exception {
        service.load();
        Team team = service.findTeam("ArZ");
        assertEquals("ARZ", team.getTeamCode());
        assertEquals("Arizona", team.getLocation());
        assertEquals("Arizona Scorpions", team.getRealName());
    }

    public void testUnreserveBoard() throws Exception {
    }

    public void testLoad() throws Exception {
        service.load();
    }

    public void testSave() throws Exception {
        service.load();
        service.save();
    }
}
