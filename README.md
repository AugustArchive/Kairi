# Kairi
> 🐅 **Experimental Kotlin library for [Revolt](https://revolt.chat)**

## Usage
```kotlin
fun main(args: Array<String>) = runBlocking {
    val bot = Kairi {
        token("...")
        apiUrl(Constants.DEFAULT_API_URL)
    }
    
    bot.on<MessageCreateEvent> {
        if (this.content == "!ping") this.channel.send("pong!")
    }
    
    bot.launch()
}
```

## License
**Kairi** is released under the [MIT](/LICENSE) License.
