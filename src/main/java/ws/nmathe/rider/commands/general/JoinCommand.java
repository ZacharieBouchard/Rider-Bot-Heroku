package ws.nmathe.rider.commands.general;

import ws.nmathe.rider.Main;
import ws.nmathe.rider.commands.Command;
import ws.nmathe.rider.core.group.GroupTable;
import ws.nmathe.rider.utils.__out;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

/**
 */
public class JoinCommand implements Command
{
    private String chanName = Main.getBotSettings().getChannel();

    @Override
    public String help(boolean brief)
    {
        final String invoke = Main.getBotSettings().getCommandPrefix() + "join";
        final String USAGE_BRIEF = "``" + invoke + " <arg>`` - join a group";
        final String USAGE_EXTENDED = "<arg> may be the group leader's name, a different user in the group," +
                " or the group name.";
        final String EXAMPLES = "Ex1. ``" + invoke + " @notem#1654``" +
                "\nEx2. **``" + invoke + " expert trials roulette**";

        if( brief )
            return USAGE_BRIEF;
        else
            return USAGE_BRIEF + "\n\n" + USAGE_EXTENDED + "\n\n" + EXAMPLES;
    }

    @Override
    public boolean verify(String[] args, MessageReceivedEvent event)
    {
        return args.length >= 1;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event)
    {
        String key = "";
        for( int i = 0; i < args.length - 1 ; i++)
        {
            key += args[i] + " ";
        }
        key += args[args.length - 1];

        key = key.replace("<","").replace("@","").replace(">","");

        GroupTable gTable = Main.getGroupManager().getGroupTable( event.getGuild().getId());

        if( gTable.isALeader(key) || gTable.isATitle(key) )
        {
            if (gTable.isAMember(event.getAuthor().getId()))
            {
                gTable.removeMember(event.getAuthor().getId());
                gTable.addMember(key, event.getAuthor().getId());
            } else
            {
                gTable.addMember(key, event.getAuthor().getId());
            }
        }
        else if( gTable.isAMember(key) )
        {
            if (gTable.isAMember(event.getAuthor().getId()))
            {
                gTable.removeMember(event.getAuthor().getId());
                gTable.addMember(key, event.getAuthor().getId());
            } else
            {
                gTable.addMember(key, event.getAuthor().getId());
            }
        }

        if( gTable.isALeader( event.getAuthor().getId() ) )
        {
            gTable.removeGroup( event.getAuthor().getId() );

            Guild guild = event.getGuild();
            Member member = guild.getMember(event.getAuthor());

            List<Role> roles = guild.getRolesByName(chanName, true);
            if( !roles.isEmpty() && guild.getMember(Main.getBotSelfUser()).hasPermission(Permission.MANAGE_ROLES) )
            {
                try
                {
                    guild.getController().removeRolesFromMember(member, roles.get(0)).queue();
                }
                catch( Exception e )
                {
                    __out.printOut(this.getClass(), e.getMessage());
                }
            }
        }
    }
}
