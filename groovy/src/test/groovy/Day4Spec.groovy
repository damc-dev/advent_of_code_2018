import groovy.transform.Canonical
import spock.lang.Specification

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter

class Day4Spec extends Specification {

    @Canonical
    class Log {
        LocalDate date
        List<Integer> minutesAwake
        LocalDateTime timestamp
        int guard
        String message
    }

    def getTimeAsleep(List<Log> logs) {
        def timeAsleep = 0
        Log lastLog = null
        for (Log log : logs) {
            if (lastLog != null && !log.message.contains("Guard") && log.message.contains("wakes up")) {
                Duration duration = Duration.between(lastLog.timestamp, log.timestamp)
//                println "${lastLog.guard}  ${lastLog.message} to ${log.message} = ${duration.toMinutes()} min"

                timeAsleep += duration.toMinutes()
            }
            lastLog = log
        }
        return timeAsleep
    }

    def getStartAndEndTimes(List<Log> logs) {
        logs = logs.findAll { !it.message.contains( "Guard #")}
        def logsByDay = logs.groupBy { it.timestamp.toLocalDate() }

        logsByDay.collectEntries { day, daysLogs ->
            [(day): [
                    startTime: daysLogs.find {it.message.contains("falls asleep")}.timestamp,
                    endTime: daysLogs.find {it.message.contains("wakes up")}.timestamp
            ]]

        }
    }

    def getMinutesAsleep(List<Log> logs) {
        logs = logs.findAll { !it.message.contains( "Guard #")}

        def logsByDay = logs.groupBy { it.timestamp.toLocalDate() }

       return logsByDay.collect { day, daysLogs ->
           def minutesAsleep = []
           def fellAsleep = null
           def wokeUp = null
           for (Log log : daysLogs) {
               if (log.message.contains("falls asleep")) {
                   fellAsleep = log.timestamp.getMinute()
               } else if (log.message.contains("wakes up")) {
                   wokeUp = log.timestamp.getMinute()
               }
               if (fellAsleep && wokeUp) {
                   minutesAsleep.addAll(fellAsleep..wokeUp)
                   fellAsleep = null
                   wokeUp = null
               }
           }
           return minutesAsleep
        }
    }

    def process(List<String> logLines) {
        List<Log> logs = logLines.collect { parseLine(it) }
        logs = logs.sort { it.timestamp }
        int guard = 0
        for ( Log log : logs) {
            if (log.message.contains("Guard #")) {
                def matcher = (log.message =~ /Guard #(\d+).*/)
                guard = Integer.valueOf(matcher[0][1])
            }
            log.guard = guard
        }

        def guards = logs.groupBy  { log -> log.guard }

        guards.each { key, value ->
            println "$key"
            value.each { println "  $it"}
        }

        def guardsTotalTimeAsleep = guards.collectEntries { Integer guardI, List<Log> guardsLogs ->
            [(guardI): getTimeAsleep(guardsLogs)]
        }
        println guardsTotalTimeAsleep.collect { k, v ->
            [k, v]
        }.sort { it[1] }.reverse()


        def theGuard = guardsTotalTimeAsleep.max { it.value }

        def theGuardLogs = guards[theGuard.key]

        def minutesAsleep = getMinutesAsleep(theGuardLogs)
       // minutesAsleep.each { println it }

        def minOccurences = (0..60).collect { aMin ->

            [aMin, minutesAsleep.findAll { it.contains(aMin)}.size()]

        }.sort { it[1] }.reverse()
        println minOccurences

        def bestMin = (0..60).max { theMin ->
            minutesAsleep.findAll { it.contains(theMin)}.size()
        }

        return [theGuard.key, bestMin]
    }

    Log parseLine(String logLine) {
        def matches = (logLine =~ /\[(.*?)\] (.*)/)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return new Log(
                timestamp: java.time.LocalDateTime.parse(matches[0][1], formatter),
                message: matches[0][2]
        )
    }

    void testParseLine() {
        given:
        Log log = parseLine("[1518-11-01 00:00] Guard #10 begins shift")
        Log expectedLog = new Log(
                timestamp: LocalDateTime.of(1518, Month.NOVEMBER, 1, 0, 0,0),
                message: "Guard #10 begins shift"
        )
        expect:
        expectedLog == log
    }

    void "testTimeAsleep"() {
        List<String> logLines = """[1518-11-01 23:58] Guard #99 begins shift
[1518-11-02 00:40] falls asleep
[1518-11-02 00:50] wakes up
[1518-11-04 00:02] Guard #99 begins shift
[1518-11-04 00:36] falls asleep
[1518-11-04 00:46] wakes up
[1518-11-05 00:03] Guard #99 begins shift
[1518-11-05 00:45] falls asleep
[1518-11-05 00:55] wakes up""".split("\n")
        List<Log> logs = logLines.collect { parseLine(it) }
        expect:
        getTimeAsleep(logs) == 30
    }

    void "getMinutesAsleep"() {
        List<String> logLines = """[1518-11-01 23:58] Guard #99 begins shift
[1518-11-02 00:40] falls asleep
[1518-11-02 00:50] wakes up
[1518-11-04 00:02] Guard #99 begins shift
[1518-11-04 00:36] falls asleep
[1518-11-04 00:46] wakes up
[1518-11-05 00:03] Guard #99 begins shift
[1518-11-05 00:45] falls asleep
[1518-11-05 00:55] wakes up""".split("\n")
        List<Log> logs = logLines.collect { parseLine(it) }
        def minutesAsleep = getMinutesAsleep(logs)
        expect:
        assert (minutesAsleep[0] as List<Integer>) == [40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50]
        minutesAsleep == [
                [40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50],
                36..46,
                45..55
        ]
    }

    void "getMinutesAsleep when multiple naps per shift"() {
        List<String> logLines = """[1518-11-01 23:58] Guard #99 begins shift
[1518-11-02 00:36] falls asleep
[1518-11-02 00:46] wakes up
[1518-11-02 00:50] falls asleep
[1518-11-02 00:58] wakes up""".split("\n")
        List<Log> logs = logLines.collect { parseLine(it) }
        def minutesAsleep = getMinutesAsleep(logs)
        expect:
        assert (minutesAsleep[0] as List<Integer>) == [36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 50, 51, 52, 53, 54, 55, 56, 57, 58]
    }

    void "test1"() {
        expect:
        List<String> input = this.getClass().getClassLoader().getResource("Day4Test1.txt").readLines()
        process(input) == [10, 24]
    }
}
