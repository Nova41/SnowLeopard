package com.nova41.bukkitdev.slr.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CommandManagerTest {

    @Test
    public void onCommand() {
        CommandSender mockedSender = mock(CommandSender.class);

        doAnswer((Answer<Void>) invocation -> {
            String message = (String) invocation.getArguments()[0];
            assertEquals("fly true", message);

            return null;
        }).when(mockedSender).sendMessage(anyString());

        Command mockedCommand = mock(Command.class);
        when(mockedCommand.getName()).thenReturn("slr");

        String[] args = "region flag set fly true".split(" ");

        CommandManager manager = new CommandManager(null, "slr");

        manager.register("", (sender, params) -> sender.sendMessage("Called command without args"));
        manager.register("region flag set", (sender, params) -> sender.sendMessage(
                String.join(" ", params)));
        manager.onCommand(mockedSender, mockedCommand, null, args);
    }

}
