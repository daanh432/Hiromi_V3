package nl.daanh.hiromi.helpers.queues;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DeliverCallback;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import nl.daanh.hiromi.helpers.queues.exceptions.HiromiQueueIOException;
import nl.daanh.hiromi.models.proto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MessageQueue extends BaseQueue {
    protected static final Logger LOGGER = LoggerFactory.getLogger(MessageQueue.class);
    protected final String EXCHANGE_NAME = "discord.messages";
    protected final String QUEUE_NAME = "";
    protected final String REPLY_QUEUE_NAME = "messages-reply";

    public MessageQueue() {
        super();
        connect();
    }

    private byte[] guildMessageToByte(GuildMessageReceivedEvent event) {
        if (event.getMember() == null) throw new RuntimeException("Member is null somehow");

        final List<Message.Mention> mentionedMembers = event.getMessage().getMentionedMembers()
                .stream()
                .map(member -> Message.Mention.newBuilder()
                        .setUserId(member.getId())
                        .setUsername(member.getEffectiveName())
                        .build())
                .collect(Collectors.toList());

        final Message message = Message.newBuilder()
                .setMessage(event.getMessage().getContentRaw())
                .setUsername(event.getMember().getEffectiveName())
                .setUserId(event.getMember().getId())
                .setChannelId(event.getChannel().getId())
                .setGuildId(event.getGuild().getId())
                .addAllMentions(mentionedMembers)
                .build();

        return message.toByteArray();
    }

    public void call(String message) {
        if (!channel.isOpen()) connect();

        try {
            channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new HiromiQueueIOException(e.getMessage(), e);
        }
    }

    public void callRPC(GuildMessageReceivedEvent event, DeliverCallback callback) {
        if (!channel.isOpen()) connect();

        try {
            final String corrId = UUID.randomUUID().toString();

            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(REPLY_QUEUE_NAME)
                    .build();

            channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, props, guildMessageToByte(event));

            channel.basicConsume(REPLY_QUEUE_NAME, true, (consumerTag, delivery) -> {
                callback.handle(consumerTag, delivery);
                channel.basicCancel(consumerTag);
            }, consumerTag -> {
                LOGGER.debug(String.format("Consumer %s cancelled", consumerTag));
            });
        } catch (IOException e) {
            throw new HiromiQueueIOException(e.getMessage(), e);
        }
    }
}