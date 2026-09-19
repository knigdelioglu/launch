package io.github.knigdelioglu.seyir.data

import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorsportRepositoryTest {
    @Test
    fun formula1Session_isConvertedToTurkeyTime_andFilteredToToday() = runBlocking {
        val repository = MotorsportRepository { url ->
            when {
                "/data/f1/" in url -> """
                    {
                      "races": [
                        {
                          "round": "15",
                          "raceName": "Azerbaijan Grand Prix",
                          "Circuit": {"circuitName": "Baku City Circuit"},
                          "date": "2026-09-26",
                          "time": "11:00:00Z",
                          "Qualifying": {
                            "date": "2026-09-25",
                            "time": "12:00:00Z"
                          }
                        }
                      ]
                    }
                """.trimIndent()
                "/data/motogp/" in url -> """{"events":[]}"""
                else -> error("Unexpected URL: $url")
            }
        }

        val result = repository.loadTodaySessions(
            zoneId = TodayMatchRepository.TURKEY_TIME_ZONE,
            date = LocalDate.parse("2026-09-25"),
        )

        assertEquals(1, result.sessions.size)
        val session = result.sessions.single()
        assertEquals(MotorsportKind.FORMULA_1, session.sport)
        assertEquals("Sıralama", session.sessionName)
        assertEquals("15:00", java.time.Instant.ofEpochSecond(session.startEpochSeconds)
            .atZone(TodayMatchRepository.TURKEY_TIME_ZONE)
            .toLocalTime()
            .toString())
    }

    @Test
    fun embeddedMotoGpSession_isUsedWithoutPulseLiveFallback() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val repository = MotorsportRepository { url ->
            requestedUrls += url
            when {
                "/data/f1/" in url -> """{"races":[]}"""
                "/data/motogp/" in url -> """
                    {
                      "events": [
                        {
                          "id": "event-1",
                          "name": "GRAND PRIX OF AUSTRIA",
                          "date_start": "2026-09-18",
                          "date_end": "2026-09-20",
                          "test": false,
                          "circuit": {"name": "Red Bull Ring"},
                          "sessions": [
                            {
                              "type": "SPR",
                              "name": "Sprint Race (MotoGP)",
                              "category": "motogp",
                              "datetime": "2026-09-19T13:00:00Z"
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
                else -> error("Pulse Live should not be called when embedded sessions exist")
            }
        }

        val result = repository.loadTodaySessions(
            zoneId = TodayMatchRepository.TURKEY_TIME_ZONE,
            date = LocalDate.parse("2026-09-19"),
        )

        assertEquals(1, result.sessions.size)
        assertEquals(MotorsportKind.MOTOGP, result.sessions.single().sport)
        assertEquals("Sprint", result.sessions.single().sessionName)
        assertTrue(requestedUrls.none { "pulselive" in it })
    }

    @Test
    fun cachedSessions_roundTrip() = runBlocking {
        val expected = listOf(
            TodayMotorsportSession(
                id = "f1-1-race",
                sport = MotorsportKind.FORMULA_1,
                eventName = "Test Grand Prix",
                circuitName = "Test Circuit",
                sessionName = "Yarış",
                startEpochSeconds = 1_800_000_000L,
            ),
        )
        val raw = MotorsportRepository.encodeSessions(expected)
        val repository = MotorsportRepository { error("Network should not be used") }

        assertEquals(expected, repository.parseCachedSessions(raw))
    }
}
