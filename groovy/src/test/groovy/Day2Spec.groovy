import spock.lang.Specification

class Day2Spec extends Specification {



    def charCounts(String input) {
        def charList = input.toCharArray() as List
        charList.collectEntries { Character chr ->
               String chrString = new String(chr)
                [(chrString): input.count(chrString)]
        }
    }

    def process(List<String> inList) {
        def twoAndThreeCounts = inList.collect { String input ->
            def charCount = charCounts(input)
            def counts = charCount.values()
            int twoCounts = counts.count { it == 2 }
            int threeCounts = counts.count { it == 3 }
            println "$input -> $twoCounts, $threeCounts"

            [twoCounts, threeCounts]
        }

        int twoCountsTotal = twoAndThreeCounts.count { int twoCounts, int threeCounts ->
            twoCounts > 0
        }
        println "two Counts Total: ${twoCountsTotal}"


        int threeCountsTotal = twoAndThreeCounts.count { int twoCounts, int threeCounts ->
            threeCounts > 0
        }
        println "three counts total: ${threeCountsTotal}"

        return [twoCountsTotal, threeCountsTotal]
    }





    void "test1"() {
        expect:
        def input = "abcdef"

        charCounts(input) == [
                "a": 1,
                "b": 1,
                "c": 1,
                "d": 1,
                "e": 1,
                "f": 1
        ]
    }


    void "test2"() {
        expect:
        def input = "bababc"

        charCounts(input) == [
                "a": 2,
                "b": 3,
                "c": 1
        ]
    }

    void "test3"() {
        expect:
        List<String> input = """abcdef
bababc
abbcde
abcccd
aabcdd
abcdee
ababab""".split("\n")
        process(input) == [4, 3]
    }

    void answer() {
        expect:
        List<String> input = this.getClass().getClassLoader().getResource("Day2.txt").readLines()
        def out = process(input)

        out == [246,33]
        out[0] * out[1] == 8118


    }
}
