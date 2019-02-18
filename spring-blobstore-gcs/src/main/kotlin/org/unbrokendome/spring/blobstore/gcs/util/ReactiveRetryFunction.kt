package org.unbrokendome.spring.blobstore.gcs.util

import org.reactivestreams.Publisher
import reactor.core.publisher.*
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.function.Function


internal typealias RetryablePredicate = (Throwable) -> Boolean
internal typealias BackOffSelector = (Throwable) -> ReactiveBackOff


internal class ReactiveRetryFunction(
    private val numAttempts: Long = DefaultNumAttempts,
    private val retryable: RetryablePredicate = DefaultRetryablePredicate,
    private val backOffSelector: BackOffSelector = DefaultBackOffSelector,
    private val timer: Scheduler = Schedulers.parallel()
) : Function<Flux<Throwable>, Publisher<Long>> {

    companion object {

        const val DefaultNumAttempts: Long = 10L
        val DefaultRetryablePredicate: RetryablePredicate = { true }
        val DefaultBackOffSelector: BackOffSelector = { ReactiveBackOff.fixed().jitter() }
    }


    override fun apply(errors: Flux<Throwable>): Publisher<Long> =
        errors.index()
            .flatMap { indexAndError ->

                val attempt = indexAndError.t1
                val error = indexAndError.t2

                if (!retryable(error)) {
                    return@flatMap error.toMono<Long>()
                }

                if (attempt >= numAttempts) {
                    return@flatMap IllegalStateException(
                        "Retries exhausted: $attempt/$numAttempts",
                        error
                    ).toMono<Long>()
                }

                val backOff = backOffSelector(error)
                val backOffDuration = backOff.nextBackOffDuration(attempt)

                if (backOffDuration.isZero) {
                    attempt.toMono()
                } else {
                    Mono.delay(backOffDuration, timer)
                }
            }
}
