package bot.utils

import bot.bridges.*
import bot.messaging_services.Discord
import bot.messaging_services.GroupMe
import bot.messaging_services.Message
import bot.messaging_services.Slack
import bot.transformers.*
import bot.utils.jobs.ScoreUpdateJob
import io.reactivex.rxjava3.core.Observable
import shared.Postgres
import java.util.concurrent.TimeUnit

object Arbiter {
    init {
        setupTransactionsBridge()
        setupScoreUpdateBridge()
        setupCloseScoreUpdateBridge()
        setupMatchUpBridge()
        setupStandingsBridge()
        setupJobs()
        setupMessageBridge()
        sendInitialMessage()
    }

    fun start() {
        Observable.interval(0, 15, TimeUnit.SECONDS)
            .subscribe {
                try {
                    val event = DataRetriever.getTransactions()
                    val latestTimeChecked = Postgres.latestTimeChecked
                    TransactionsBridge.dataObserver.accept(Pair(latestTimeChecked, event))
                    Postgres.saveLastTimeChecked()
                } catch (e: Exception) {
                    println(e.localizedMessage)
                }
            }
    }

    private fun sendInitialMessage() {
        if (!Postgres.startupMessageSent) {
            MessageBridge.dataObserver.accept(
                Message.Generic(
                    """
                        |Hey there! I am the Yahoo Fantasy Bot that notifies you about all things happening in your league!
                        |Star me on Github: https://github.com/landonp1203/yahoo-fantasy-bot
                    """.trimIndent()
                )
            )
            Postgres.markStartupMessageReceived()
        } else {
            println("Start up message already sent, not sending...")
        }
    }

    private fun setupTransactionsBridge() {
        val transactions = TransactionsBridge.dataObservable
            .convertToTransactionMessage()

        transactions.subscribe(MessageBridge.dataObserver)
    }

    private fun setupScoreUpdateBridge() {
        val transactions = ScoreUpdateBridge.dataObservable
            .convertToMatchUpObject()
            .convertToScoreUpdateMessage()

        transactions.subscribe(MessageBridge.dataObserver)
    }

    private fun setupCloseScoreUpdateBridge() {
        val transactions = CloseScoreUpdateBridge.dataObservable
            .convertToMatchUpObject()
            .convertToScoreUpdateMessage(true)

        transactions.subscribe(MessageBridge.dataObserver)
    }

    private fun setupMatchUpBridge() {
        val transactions = MatchUpBridge.dataObservable
            .convertToMatchUpObject()
            .convertToMatchUpMessage()

        transactions.subscribe(MessageBridge.dataObserver)
    }

    private fun setupStandingsBridge() {
        val standings = StandingsBridge.dataObservable
            .convertToStandingsObject()
            .convertToStandingsMessage()

        standings.subscribe(MessageBridge.dataObserver)
    }

    private fun setupMessageBridge() {
        val messages = MessageBridge.dataObservable
            .convertToStringMessage()

        messages.subscribe(Discord)
        messages.subscribe(GroupMe)
        messages.subscribe(Slack)
    }

    private fun setupJobs() {
        // Times are in GMT since it is not effected by DST

        JobRunner.createJob(ScoreUpdateJob::class.java, "0 55 3 ? 9-1 FRI *")
        JobRunner.createJob(ScoreUpdateJob::class.java, "0 00 17 ? 9-1 SUN *")
        JobRunner.createJob(ScoreUpdateJob::class.java, "0 00 20 ? 9-1 SUN *")
        JobRunner.createJob(ScoreUpdateJob::class.java, "0 00 0 ? 9-1 MON *")
        JobRunner.createJob(ScoreUpdateJob::class.java, "0 55 3 ? 9-1 MON *")
        JobRunner.createJob(ScoreUpdateJob::class.java, "0 55 3 ? 9-1 TUE *")

        JobRunner.runJobs()
    }
}
