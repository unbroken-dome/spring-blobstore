package org.unbrokendome.spring.blobstore.gcs.util

import org.reactivestreams.Publisher
import reactor.core.publisher.*
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Function
import java.util.function.Predicate


class RandomExponentialBackoff(
    private val errorFilter: Predicate<Throwable>,
    private val numRetries: Long = 10,
    private val firstBackoff: Duration = Duration.ofMillis(500),
    private val backoffFactor: Double = 1.5,
    private val maxBackoff: Duration = Duration.ofMillis(Long.MAX_VALUE),
    private val jitterFactor: Double = 0.5,
    private val timer: Scheduler = Schedulers.parallel()
) : Function<Flux<Throwable>, Publisher<Long>> {

    override fun apply(throwables: Flux<Throwable>): Publisher<Long> =
        throwables.index()
            .flatMap { indexAndError ->
                val iteration = indexAndError.t1
                val error = indexAndError.t2

                if (!errorFilter.test(error)) {
                    return@flatMap error.toMono<Long>()
                }

                if (iteration >= numRetries) {
                    return@flatMap IllegalStateException(
                        "Retries exhausted: $iteration/$numRetries",
                        error
                    ).toMono<Long>()
                }

                val nextBackoff = try {
                    firstBackoff.multipliedBy(Math.pow(backoffFactor, iteration.toDouble()).toLong())
                        .coerceAtMost(maxBackoff)
                } catch (ex: ArithmeticException) {
                    maxBackoff
                }

                if (nextBackoff.isZero) {
                    iteration.toMono()
                } else {
                    Mono.delay(applyJitter(nextBackoff), timer)
                }
            }


    private fun applyJitter(nextBackoff: Duration): Duration =
        nextBackoff.plusMillis(jitter(nextBackoff))


    private fun jitter(nextBackoff: Duration): Long {
        val jitterOffset = jitterOffset(nextBackoff)

        val lowBound = Math.max(firstBackoff.minus(nextBackoff).toMillis(), -jitterOffset)
        val highBound = Math.min(maxBackoff.minus(nextBackoff).toMillis(), jitterOffset)

        val random = ThreadLocalRandom.current()
        return if (highBound == lowBound) {
            if (highBound == 0L) 0L else random.nextLong(highBound)
        } else {
            random.nextLong(lowBound, highBound)
        }
    }


    private fun jitterOffset(nextBackoff: Duration): Long =
        try {
            nextBackoff.multipliedBy((jitterFactor * 100.0).toLong())
                .dividedBy(100)
                .toMillis()
        } catch (ex: ArithmeticException) {
            Math.round(Long.MAX_VALUE * jitterFactor)
        }
}
