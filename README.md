# Kairi
> 🐅 **Experimental Kotlin library for [Revolt](https://revolt.chat)**

## Usage
```kotlin
fun main(args: Array<String>) = runBlocking {
    val bot = Kairi {
        token = ""
    }
    
    bot.on<MessageCreateEvent> {
        if (this.content == "!ping") this.channel.send("pong!")
    }
    
    bot.launch()
}
```

## Support
if you need support, you can join the Revolt server here: https://app.revolt.chat/invite/bXkwK8af (it's very incomplete lol)

## License
**Kairi** is released under the [MIT](/LICENSE) License.
