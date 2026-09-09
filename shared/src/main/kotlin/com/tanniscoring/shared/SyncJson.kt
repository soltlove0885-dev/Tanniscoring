package com.tanniscoring.shared

/**
 * Minimal JSON codec for Wear ↔ Phone payloads (no third-party deps in :shared).
 * Sufficient for our flat DTOs; not a general-purpose parser.
 */
object SyncJson {

    fun encodeEvent(event: ScoringEventDto): String = buildString {
        append('{')
        append("\"type\":\"").append(escape(event.type)).append('"')
        event.side?.let { append(",\"side\":\"").append(escape(it)).append('"') }
        event.playerA?.let { append(",\"playerA\":\"").append(escape(it)).append('"') }
        event.playerB?.let { append(",\"playerB\":\"").append(escape(it)).append('"') }
        event.bestOf?.let { append(",\"bestOf\":").append(it) }
        append(",\"sequence\":").append(event.sequence)
        append('}')
    }

    fun decodeEvent(json: String): ScoringEventDto {
        val map = parseObject(json)
        return ScoringEventDto(
            type = map["type"] ?: "",
            side = map["side"],
            playerA = map["playerA"],
            playerB = map["playerB"],
            bestOf = map["bestOf"]?.toIntOrNull(),
            sequence = map["sequence"]?.toLongOrNull() ?: 0L,
        )
    }

    fun encodeState(dto: MatchStateDto): String = buildString {
        append('{')
        append("\"playerA\":\"").append(escape(dto.playerA)).append('"')
        append(",\"playerB\":\"").append(escape(dto.playerB)).append('"')
        append(",\"bestOf\":").append(dto.bestOf)
        append(",\"setsA\":").append(dto.setsA)
        append(",\"setsB\":").append(dto.setsB)
        append(",\"gamesA\":").append(dto.gamesA)
        append(",\"gamesB\":").append(dto.gamesB)
        append(",\"pointsA\":").append(dto.pointsA)
        append(",\"pointsB\":").append(dto.pointsB)
        append(",\"isDeuce\":").append(dto.isDeuce)
        append(",\"advantageA\":").append(dto.advantageA)
        append(",\"advantageB\":").append(dto.advantageB)
        append(",\"isTiebreak\":").append(dto.isTiebreak)
        append(",\"isMatchOver\":").append(dto.isMatchOver)
        append(",\"winner\":")
        if (dto.winner == null) append("null") else append('"').append(escape(dto.winner)).append('"')
        append(",\"pointDisplayA\":\"").append(escape(dto.pointDisplayA)).append('"')
        append(",\"pointDisplayB\":\"").append(escape(dto.pointDisplayB)).append('"')
        append(",\"setHistory\":[")
        dto.setHistory.forEachIndexed { i, s ->
            if (i > 0) append(',')
            append("{\"gamesA\":").append(s.gamesA).append(",\"gamesB\":").append(s.gamesB).append('}')
        }
        append(']')
        append('}')
    }

    fun decodeState(json: String): MatchStateDto {
        val map = parseObject(json)
        val history = parseSetHistory(json)
        return MatchStateDto(
            playerA = map["playerA"] ?: "",
            playerB = map["playerB"] ?: "",
            bestOf = map["bestOf"]?.toIntOrNull() ?: 3,
            setsA = map["setsA"]?.toIntOrNull() ?: 0,
            setsB = map["setsB"]?.toIntOrNull() ?: 0,
            gamesA = map["gamesA"]?.toIntOrNull() ?: 0,
            gamesB = map["gamesB"]?.toIntOrNull() ?: 0,
            pointsA = map["pointsA"]?.toIntOrNull() ?: 0,
            pointsB = map["pointsB"]?.toIntOrNull() ?: 0,
            isDeuce = map["isDeuce"] == "true",
            advantageA = map["advantageA"] == "true",
            advantageB = map["advantageB"] == "true",
            isTiebreak = map["isTiebreak"] == "true",
            isMatchOver = map["isMatchOver"] == "true",
            winner = map["winner"]?.takeIf { it != "null" },
            setHistory = history,
            pointDisplayA = map["pointDisplayA"] ?: "0",
            pointDisplayB = map["pointDisplayB"] ?: "0",
        )
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    /**
     * Very small flat-object parser: extracts "key":value pairs at the top level
     * (ignores nested objects/arrays for scalar fields).
     */
    private fun parseObject(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pattern = Regex("\"([^\"]+)\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|true|false|null|-?\\d+)")
        for (m in pattern.findAll(json)) {
            val key = m.groupValues[1]
            if (key == "setHistory") continue
            var value = m.groupValues[2]
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            }
            result[key] = value
        }
        return result
    }

    private fun parseSetHistory(json: String): List<SetScoreDto> {
        val start = json.indexOf("\"setHistory\"")
        if (start < 0) return emptyList()
        val arrStart = json.indexOf('[', start)
        val arrEnd = json.indexOf(']', arrStart)
        if (arrStart < 0 || arrEnd < 0) return emptyList()
        val arr = json.substring(arrStart + 1, arrEnd)
        if (arr.isBlank()) return emptyList()
        val itemPattern = Regex("\\{\\s*\"gamesA\"\\s*:\\s*(\\d+)\\s*,\\s*\"gamesB\"\\s*:\\s*(\\d+)\\s*\\}")
        return itemPattern.findAll(arr).map {
            SetScoreDto(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }.toList()
    }
}
