import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer

class Client (val client: IDiscordClient) {
	private def sendMessage(channelID: Long, msg: String): Unit = {
		RequestBuffer.request(() =>
			client.getChannelByID(channelID).sendMessage(msg)
		)
	}

	@EventSubscriber
	def messageRecieved(e: MessageReceivedEvent): Unit = {
		if (e.getMessage.getContent.startsWith("!test"))
			sendMessage(e.getChannel.getLongID, "hey it's working!")
	}
}
object Client {
	def apply(apiKey: String): Client = {
		val iClient = new ClientBuilder().withToken(apiKey).build()
		val client = new Client(iClient)
		iClient.getDispatcher.registerListeners(client)
		iClient.login()
		client
	}
}
