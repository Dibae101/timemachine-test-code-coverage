import kotlinx.atomicfu.atomic
import org.andstatus.game2048.model.GameClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtomicTest {

    @Test
    fun atomicTest() {
        val nextIdHolder = atomic(0)
        fun nextId(): Int = nextIdHolder.incrementAndGet()
        assertEquals(1, nextId())

        val boolVal = atomic(false)
        assertTrue(boolVal.compareAndSet(false, true))
        assertEquals(true, boolVal.value)

        val clock = GameClock()
        assertEquals(0, clock.playedSeconds)
        clock.start()
        assertTrue(clock.started)
    }
}