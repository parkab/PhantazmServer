package org.phantazm.core.guild.party.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;
import org.phantazm.core.guild.party.Party;
import org.phantazm.core.guild.party.PartyCreator;
import org.phantazm.core.player.BasicPlayerViewProvider;
import org.phantazm.core.player.PlayerViewProvider;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
public class PartyJoinCommandIntegrationTest extends AbstractPartyCommandIntegrationTest {

    @SuppressWarnings({"UnstableApiUsage", "JUnitMalformedDeclaration"})
    @Test
    public void testNotInPartyAfterJoiningOtherPlayerNotInParty(Env env) {
        PlayerViewProvider viewProvider = new BasicPlayerViewProvider(identitySource, env.process().connection());
        PartyCreator partyCreator = new PartyCreator.Builder().build();
        Command command = PartyCommand.partyCommand(commandConfig, MiniMessage.miniMessage(), partyHolder, viewProvider, partyCreator, new Random());
        env.process().command().register(command);
        Instance instance = env.createFlatInstance();
        Player firstPlayer = env.createPlayer(instance, Pos.ZERO);
        firstPlayer.setUsernameField("first");
        Player secondPlayer = env.createPlayer(instance, Pos.ZERO);
        secondPlayer.setUsernameField("second");

        env.process().command().execute(secondPlayer, "party join first");

        assertFalse(partyHolder.uuidToGuild().containsKey(firstPlayer.getUuid()));
        assertFalse(partyHolder.uuidToGuild().containsKey(secondPlayer.getUuid()));
    }

    @SuppressWarnings({"UnstableApiUsage", "JUnitMalformedDeclaration"})
    @Test
    public void testNotInPartyWithoutInviteAndJoiningOtherPlayerInParty(Env env) {
        PlayerViewProvider viewProvider = new BasicPlayerViewProvider(identitySource, env.process().connection());
        PartyCreator partyCreator = new PartyCreator.Builder().build();
        Command command = PartyCommand.partyCommand(commandConfig, MiniMessage.miniMessage(), partyHolder, viewProvider, partyCreator, new Random());
        env.process().command().register(command);
        Instance instance = env.createFlatInstance();
        Player firstPlayer = env.createPlayer(instance, Pos.ZERO);
        firstPlayer.setUsernameField("first");
        env.process().command().execute(firstPlayer, "party create");
        Party party = partyHolder.uuidToGuild().get(firstPlayer.getUuid());
        Player secondPlayer = env.createPlayer(instance, Pos.ZERO);
        secondPlayer.setUsernameField("second");

        env.process().command().execute(secondPlayer, "party join first");

        assertFalse(partyHolder.uuidToGuild().containsKey(secondPlayer.getUuid()));
        assertFalse(party.getMemberManager().hasMember(secondPlayer.getUuid()));
    }

    @SuppressWarnings({"UnstableApiUsage", "JUnitMalformedDeclaration"})
    @Test
    public void testInPartyAfterInviteAndJoiningOtherPlayerInParty(Env env) {
        PlayerViewProvider viewProvider = new BasicPlayerViewProvider(identitySource, env.process().connection());
        PartyCreator partyCreator = new PartyCreator.Builder().build();
        Command command = PartyCommand.partyCommand(commandConfig, MiniMessage.miniMessage(), partyHolder, viewProvider, partyCreator, new Random());
        env.process().command().register(command);
        Instance instance = env.createFlatInstance();
        Player firstPlayer = env.createPlayer(instance, Pos.ZERO);
        firstPlayer.setUsernameField("first");
        env.process().command().execute(firstPlayer, "party create");
        Party party = partyHolder.uuidToGuild().get(firstPlayer.getUuid());
        Player secondPlayer = env.createPlayer(instance, Pos.ZERO);
        secondPlayer.setUsernameField("second");
        party.getInvitationManager().invite(party.getMemberManager().getMember(firstPlayer.getUuid()),
                viewProvider.fromPlayer(secondPlayer));

        env.process().command().execute(secondPlayer, "party join first");

        assertEquals(party, partyHolder.uuidToGuild().get(secondPlayer.getUuid()));
        assertTrue(party.getMemberManager().hasMember(secondPlayer.getUuid()));
    }

}
