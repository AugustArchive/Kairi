package kairi.core

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class KairiThreadFactory: ThreadFactory {
    private val increment = AtomicInteger(0)
    private val group: ThreadGroup

    init {
        val security = System.getSecurityManager()
        group = if (security != null)
            security.threadGroup
        else
            Thread.currentThread().threadGroup
    }

    override fun newThread(r: Runnable): Thread {
        val name = "Kairi-Thread[${increment.incrementAndGet()}]"
        val thread = Thread(group, r, name)

        if (thread.isDaemon)
            thread.isDaemon = false

        if (thread.priority != Thread.NORM_PRIORITY)
            thread.priority = Thread.NORM_PRIORITY

        return thread
    }
}
