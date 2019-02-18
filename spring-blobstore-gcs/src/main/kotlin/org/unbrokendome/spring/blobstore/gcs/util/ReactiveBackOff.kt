package org.unbrokendome.spring.blobstore.gcs.util

import java.time.Duration
import java.util.concurrent.ThreadLocalRandom


interface ReactiveBackOff {

    fun nextBackOffDuration(attempt: Long): Duration


    fun jitter(jitterFactor: Double = 0.2): ReactiveBackOff =
        JitterReactiveBackOffDecorator(this, jitterFactor)


    companion object {

        fun fixed(interval: Duration = Duration.ofMillis(500L)): ReactiveBackOff =
            FixedReactiveBackOff(interval)

        fun exponential(
            firstBackOff: Duration = Duration.ofMillis(500L),
            backOffFactor: Double = 1.5,
            maxBackOff: Duration = Duration.ofMillis(Long.MAX_VALUE)
        ): ReactiveBackOff =
            ExponentialReactiveBackOff(firstBackOff, backOffFactor, maxBackOff)
    }
}


class FixedReactiveBackOff(
    private val interval: Duration
) : ReactiveBackOff {

    override fun nextBackOffDuration(attempt: Long): Duration =
        interval
}


class ExponentialReactiveBackOff(
    private val firstBackOff: Duration,
    private val backOffFactor: Double,
    private val maxBackOff: Duration
) : ReactiveBackOff {

    override fun nextBackOffDuration(attempt: Long): Duration =
        try {
            firstBackOff.multipliedBy(Math.pow(backOffFactor, attempt.toDouble()).toLong())
                .coerceAtMost(maxBackOff)
        } catch (ex: ArithmeticException) {
            maxBackOff
        }
}


class JitterReactiveBackOffDecorator(
    private val delegate: ReactiveBackOff,
    jitterFactor: Double = 0.2
) : ReactiveBackOff {

    init {
        require(jitterFactor in (0.0)..(1.0)) { "jitterFactor must be between 0.0 and 1.0" }
    }

    private val minJitter = 1.0 - jitterFactor
    private val maxJitter = 1.0 + jitterFactor

    override fun nextBackOffDuration(attempt: Long): Duration {

        val nextBackOff = delegate.nextBackOffDuration(attempt)

        val actualJitter = ThreadLocalRandom.current().nextDouble(minJitter, maxJitter)
            .coerceIn(0.0, 2.0)

        return try {
            nextBackOff.multipliedBy((100 * actualJitter).toLong())
                .dividedBy(100)
        } catch (ex: ArithmeticException) {
            nextBackOff
        }
    }
}
