package bot.bridges

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Consumer
import bot.messaging.Message

class MessageBridge : Bridge<Message> {
    private val dataBridge = PublishRelay.create<Message>()

    override val consumer: Consumer<Message>
        get() = dataBridge

    override val eventStream: Observable<Message>
        get() = dataBridge
}
