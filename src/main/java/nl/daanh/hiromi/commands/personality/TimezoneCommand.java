package nl.daanh.hiromi.commands.personality;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import nl.daanh.hiromi.database.IDatabaseManager;
import nl.daanh.hiromi.helpers.MessageFormatting;
import nl.daanh.hiromi.models.commandcontext.ICommandContext;
import nl.daanh.hiromi.models.commandcontext.ISlashCommandContext;
import nl.daanh.hiromi.models.commands.ICommand;
import nl.daanh.hiromi.models.commands.ISlashCommand;
import nl.daanh.hiromi.models.commands.annotations.CommandCategory;
import nl.daanh.hiromi.models.commands.annotations.CommandInvoke;
import nl.daanh.hiromi.models.commands.annotations.SelfPermission;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@CommandInvoke("timezone")
@CommandInvoke("gettimezone")
@CommandInvoke("gettz")
@CommandCategory(CommandCategory.CATEGORY.PERSONALITY)
@SelfPermission(Permission.MESSAGE_WRITE)
public class TimezoneCommand implements ICommand, ISlashCommand {
    private String parseTimezone(ZoneId timezoneInput) {
        return timezoneInput.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }


    @Override
    public void handle(ICommandContext ctx) {
        Member member = ctx.getMember();
        IDatabaseManager databaseManager = ctx.getConfiguration().getDatabaseManager();
        List<String> args = ctx.getArgs();
        List<Member> mentionedMembers = ctx.getMessage().getMentionedMembers();

        if (args.isEmpty()) {
            ZoneId timezone = databaseManager.getTimezone(member.getUser());
            if (timezone == null) {
                ctx.reply(String.format("You don't have your timezone set. Please use %ssettimezone <timezone> to set your timezone",
                        databaseManager.getPrefix(ctx.getGuild())
                )).queue();
                return;
            }

            ctx.reply(String.format("Your timezone is set to ``%s``", parseTimezone(timezone))).queue();
            return;
        }


        if (mentionedMembers.isEmpty()) {
            ctx.reply("Please specify a valid user").queue();
            return;
        }

        Member mentionedMember = mentionedMembers.get(0);

        ZoneId mentionedTimezone = databaseManager.getTimezone(mentionedMember.getUser());
        if (mentionedTimezone == null) {
            ctx.reply(String.format("It looks like your friend hasn't set their timezone set. Please tell them to use %ssettimezone <timezone> to set their timezone",
                    databaseManager.getPrefix(ctx.getGuild())
            )).queue();
            return;
        }

        ZoneId timezone = databaseManager.getTimezone(member.getUser());

        if (timezone == null) {
            ctx.reply(String.format("The timezone of %s is set to %s\nTheir current time is: %s",
                    mentionedMember.getEffectiveName(),
                    parseTimezone(mentionedTimezone),
                    MessageFormatting.currentDateTimeFormatted(mentionedTimezone)
            )).queue();
            return;
        }

        LocalDateTime mentionedTime = ZonedDateTime.now(mentionedTimezone).toLocalDateTime();
        LocalDateTime time = ZonedDateTime.now(timezone).toLocalDateTime();

        boolean isBehind = mentionedTime.isBefore(time);
        boolean isAfter = mentionedTime.isAfter(time);

        ctx.reply(String.format("The timezone of %s is set to %s\nTheir current time is: %s\nYour time is: %s\nThey are %s %s",
                mentionedMember.getEffectiveName(),
                parseTimezone(mentionedTimezone),
                MessageFormatting.currentDateTimeFormatted(mentionedTimezone),
                MessageFormatting.currentDateTimeFormatted(timezone),
                String.format("%s hour(s) and %s minutes",
                        Math.abs(mentionedTime.getHour() - time.getHour()),
                        Math.abs(mentionedTime.getMinute() - time.getMinute())
                ),
                isBehind ? "behind" : isAfter ? "after" : "ahead or behind"
        )).queue();
    }

    @Override
    public void handle(ISlashCommandContext ctx) {
        SlashCommandEvent event = ctx.getEvent();
        Member member = ctx.getMember();
        IDatabaseManager databaseManager = ctx.getConfiguration().getDatabaseManager();
        OptionMapping userMention = event.getOption("member");

        if (userMention == null) {
            ZoneId timezone = databaseManager.getTimezone(member.getUser());
            if (timezone == null) {
                ctx.reply("You don't have your timezone set. Please use /settimezone <timezone> to set your timezone")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            ctx.reply(String.format("Your timezone is set to ``%s``", parseTimezone(timezone))).setEphemeral(true).queue();
            return;
        }


        if (userMention.getAsMember() == null) {
            ctx.reply("Please specify a valid user").setEphemeral(true).queue();
            return;
        }

        Member mentionedMember = userMention.getAsMember();

        ZoneId mentionedTimezone = databaseManager.getTimezone(mentionedMember.getUser());
        if (mentionedTimezone == null) {
            ctx.reply("It looks like your friend hasn't set their timezone set. Please tell them to use /settimezone <timezone> to set their timezone")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        ZoneId timezone = databaseManager.getTimezone(member.getUser());

        if (timezone == null) {
            ctx.reply(String.format("The timezone of %s is set to %s\nTheir current time is: %s",
                    mentionedMember.getEffectiveName(),
                    parseTimezone(mentionedTimezone),
                    MessageFormatting.currentDateTimeFormatted(mentionedTimezone)
            )).setEphemeral(true).queue();
            return;
        }

        LocalDateTime mentionedTime = ZonedDateTime.now(mentionedTimezone).toLocalDateTime();
        LocalDateTime time = ZonedDateTime.now(timezone).toLocalDateTime();

        boolean isBehind = mentionedTime.isBefore(time);
        boolean isAfter = mentionedTime.isAfter(time);

        ctx.reply(String.format("The timezone of %s is set to %s\nTheir current time is: %s\nYour time is: %s\nThey are %s %s",
                mentionedMember.getEffectiveName(),
                parseTimezone(mentionedTimezone),
                MessageFormatting.currentDateTimeFormatted(mentionedTimezone),
                MessageFormatting.currentDateTimeFormatted(timezone),
                String.format("%s hour(s) and %s minutes",
                        Math.abs(mentionedTime.getHour() - time.getHour()),
                        Math.abs(mentionedTime.getMinute() - time.getMinute())
                ),
                isBehind ? "behind" : isAfter ? "after" : "ahead or behind"
        )).setEphemeral(true).queue();
    }

    @Override
    public CommandData getCommandDefinition() {
        return new CommandData("timezone", "Get your own or your friends timezone and time")
                .addOption(OptionType.USER, "member", "The person you want to view the timezone of", false);
    }
}
