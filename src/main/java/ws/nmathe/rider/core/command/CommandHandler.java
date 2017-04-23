package ws.nmathe.rider.core.command;

import net.dv8tion.jda.core.entities.ChannelType;
import ws.nmathe.rider.commands.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import ws.nmathe.rider.commands.admin.NowPlayingCommand;
import ws.nmathe.rider.commands.admin.StatsCommand;
import ws.nmathe.rider.commands.general.*;
import ws.nmathe.rider.utils.MessageUtilities;
import ws.nmathe.rider.utils.VerifyUtilities;
import ws.nmathe.rider.utils.__out;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles MessageEvents containing commands, parsing them into containers then processing
 * the command on a thread.
 */
public class CommandHandler
{
    private final CommandParser commandParser = new CommandParser();      // parses command strings into containers
    private final ExecutorService commandExec = Executors.newCachedThreadPool(); // thread pool for running commands
    private final RateLimiter rateLimiter = new RateLimiter();
    private HashMap<String, Command> commands;         // maps Command to invoke string
    private HashMap<String, Command> adminCommands;    // ^^ but for admin commands

    public CommandHandler()
    {
        commands = new HashMap<>();
        adminCommands = new HashMap<>();
    }

    public void init()
    {
        // add bot commands with their lookup name
        commands.put("lf", new LookingForCommand() );
        commands.put("close", new CloseCommand() );
        commands.put( "join", new JoinCommand() );
        commands.put( "renew", new RenewCommand() );
        commands.put( "leave", new LeaveCommand() );
        commands.put( "help", new HelpCommand() );
        commands.put( "kick", new KickCommand() );
        commands.put( "setup", new SetupCommand() );

        // add administrator commands with their lookup name
        adminCommands.put( "stats", new StatsCommand() );
        adminCommands.put( "playing", new NowPlayingCommand() );
    }

    public void handleCommand( MessageReceivedEvent event, Integer type )
    {
        CommandParser.CommandContainer cc = commandParser.parse( event );
        if( type == 0 )
        {
            if(rateLimiter.isOnCooldown(event.getAuthor().getId()) )
            {
                __out.printOut(this.getClass(), "@" + event.getAuthor().getName() +
                        " [" + event.getAuthor().getId() + "] was rate limited on '" +
                        event.getGuild().getName() +"' [" + event.getGuild().getId() + "] using the '" +
                        cc.invoke + "' command!");
                return;
            }
            handleGeneralCommand( cc );
        }
        else if( type == 1 )
        {
            handleAdminCommand( cc );
        }

    }

    private void handleGeneralCommand(CommandParser.CommandContainer cc)
    {
        // if the invoking command appears in commands
        if(commands.containsKey(cc.invoke))
        {
            commandExec.submit( () ->
            {
                boolean valid = commands.get(cc.invoke).verify(cc.args, cc.event);

                // do command action if valid arguments
                if (valid)
                {
                    try
                    {
                        commands.get(cc.invoke).action(cc.args, cc.event);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                try
                {
                    Thread.sleep(1000);
                }
                catch(Exception ignored)
                { }

                if (!cc.event.isFromType(ChannelType.PRIVATE) && VerifyUtilities.verifyManagePerm(cc.event.getGuild(), cc.event.getTextChannel()))
                    MessageUtilities.deleteMsg(cc.event.getMessage(), null);
            });
        }
    }

    private void handleAdminCommand(CommandParser.CommandContainer cc)
    {
        // for admin commands
        if(adminCommands.containsKey(cc.invoke))
        {
            boolean valid = adminCommands.get(cc.invoke).verify(cc.args, cc.event);

            // do command action if valid arguments
            if (valid)
            {
                commandExec.submit( () -> {
                    try
                    {
                        adminCommands.get(cc.invoke).action(cc.args, cc.event);
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public Collection<Command> getCommands()
    {
        return commands.values();
    }

    public Command getCommand( String invoke )
    {
        // check if command exists, if so return it
        if( commands.containsKey(invoke) )
            return commands.get(invoke);

        else    // otherwise return null
            return null;
    }

}
