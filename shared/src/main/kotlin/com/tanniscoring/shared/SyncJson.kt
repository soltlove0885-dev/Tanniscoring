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
        event.mode?.let { append(",\"mode\":\"").append(escape(it)).append('"') }
        event.server?.let { append(",\"server\":\"").append(escape(it)).append('"') }
        event.noAd?.let { append(",\"noAd\":").append(it) }
        event.sport?.let { append(",\"sport\":\"").append(escape(it)).append('"') }
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
            mode = map["mode"],
            server = map["server"],
            noAd = map["noAd"]?.let { it == "true" },
            sport = map["sport"],
            sequence = map["sequence"]?.toLongOrNull() ?: 0L,
        )
    }

    fun encodeState(dto: MatchStateDto): String = buildString {
        append('{')
        append("\"sport\":\"").append(escape(dto.sport)).append('"')
        append(",\"playerA\":\"").append(escape(dto.playerA)).append('"')
        append(",\"playerB\":\"").append(escape(dto.playerB)).append('"')
        append(",\"bestOf\":").append(dto.bestOf)
        append(",\"mode\":\"").append(escape(dto.mode)).append('"')
        append(",\"noAd\":").append(dto.noAd)
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
        append(",\"server\":\"").append(escape(dto.server)).append('"')
        append(",\"matchActive\":").append(dto.matchActive)
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
            sport = map["sport"] ?: SportType.TENNIS.name,
            playerA = map["playerA"] ?: "",
            playerB = map["playerB"] ?: "",
            bestOf = map["bestOf"]?.toIntOrNull() ?: 3,
            mode = map["mode"] ?: MatchMode.SINGLES.name,
            noAd = map["noAd"] == "true",
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
            server = map["server"] ?: Side.A.name,
            matchActive = map["matchActive"]?.let { it == "true" } ?: true,
        )
    }

    fun encodeHistoryList(entries: List<MatchHistoryEntry>): String = buildString {
        append('[')
        entries.forEachIndexed { i, e ->
            if (i > 0) append(',')
            append(encodeHistoryEntry(e))
        }
        append(']')
    }

    fun decodeHistoryList(json: String): List<MatchHistoryEntry> {
        if (json.isBlank() || json == "[]") return emptyList()
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val items = mutableListOf<MatchHistoryEntry>()
        var depth = 0
        var start = -1
        for (i in trimmed.indices) {
            val c = trimmed[i]
            when (c) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        items.add(decodeHistoryEntry(trimmed.substring(start, i + 1)))
                        start = -1
                    }
                }
            }
        }
        return items
    }

    fun encodeHistoryEntry(e: MatchHistoryEntry): String = buildString {
        append('{')
        append("\"id\":\"").append(escape(e.id)).append('"')
        append(",\"finishedAtEpochMs\":").append(e.finishedAtEpochMs)
        append(",\"playerA\":\"").append(escape(e.playerA)).append('"')
        append(",\"playerB\":\"").append(escape(e.playerB)).append('"')
        append(",\"bestOf\":").append(e.bestOf)
        append(",\"mode\":\"").append(escape(e.mode)).append('"')
        append(",\"setsA\":").append(e.setsA)
        append(",\"setsB\":").append(e.setsB)
        append(",\"winner\":")
        if (e.winner == null) append("null") else append('"').append(escape(e.winner)).append('"')
        append(",\"setHistory\":[")
        e.setHistory.forEachIndexed { i, s ->
            if (i > 0) append(',')
            append("{\"gamesA\":").append(s.gamesA).append(",\"gamesB\":").append(s.gamesB).append('}')
        }
        append(']')
        append('}')
    }

    fun decodeHistoryEntry(json: String): MatchHistoryEntry {
        val map = parseObject(json)
        return MatchHistoryEntry(
            id = map["id"] ?: "",
            finishedAtEpochMs = map["finishedAtEpochMs"]?.toLongOrNull() ?: 0L,
            playerA = map["playerA"] ?: "",
            playerB = map["playerB"] ?: "",
            bestOf = map["bestOf"]?.toIntOrNull() ?: 3,
            mode = map["mode"] ?: MatchMode.SINGLES.name,
            setsA = map["setsA"]?.toIntOrNull() ?: 0,
            setsB = map["setsB"]?.toIntOrNull() ?: 0,
            winner = map["winner"]?.takeIf { it != "null" },
            setHistory = parseSetHistory(json),
        )
    }


    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    /**
     * Very small flat-object parser: extracts "key":value pairs at the **top level only**
     * (skips nested objects/arrays so tournament matches do not pollute scalars).
     */
    private fun parseObject(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pattern = Regex("\"([^\"]+)\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|true|false|null|-?\\d+)")
        var depth = 0
        var i = 0
        var inString = false
        var escape = false
        while (i < json.length) {
            val c = json[i]
            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
                i++
                continue
            }
            when (c) {
                '"' -> {
                    // Look for a top-level key starting here
                    if (depth == 1) {
                        val m = pattern.find(json, i)
                        if (m != null && m.range.first == i) {
                            val key = m.groupValues[1]
                            if (key != "setHistory" && key != "players" && key != "matches") {
                                var value = m.groupValues[2]
                                if (value.startsWith("\"") && value.endsWith("\"")) {
                                    value = value.substring(1, value.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                }
                                result[key] = value
                            }
                            // Advance past the matched key:value, but do not skip nested structures —
                            // arrays/objects after ':' are handled by depth tracking below if we
                            // only consumed scalar matches. For array/object values the regex
                            // does not match, so fall through.
                            if (m.groupValues[2].let { it.startsWith("\"") || it == "true" || it == "false" || it == "null" || it.toLongOrNull() != null }) {
                                i = m.range.last + 1
                                continue
                            }
                        }
                    }
                    inString = true
                }
                '{' -> depth++
                '}' -> depth--
                '[' -> depth++
                ']' -> depth--
            }
            i++
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

    // --- Tournament persistence ---

    fun encodeTournament(t: Tournament): String = buildString {
        append('{')
        append("\"id\":\"").append(escape(t.id)).append('"')
        append(",\"playerCount\":").append(t.playerCount)
        append(",\"defaultBestOf\":").append(t.defaultBestOf)
        append(",\"defaultNoAd\":").append(t.defaultNoAd)
        append(",\"activeMatchId\":")
        if (t.activeMatchId == null) append("null") else append('"').append(escape(t.activeMatchId)).append('"')
        append(",\"champion\":")
        if (t.champion == null) append("null") else append('"').append(escape(t.champion)).append('"')
        append(",\"createdAtEpochMs\":").append(t.createdAtEpochMs)
        append(",\"players\":[")
        t.players.forEachIndexed { i, p ->
            if (i > 0) append(',')
            append('"').append(escape(p)).append('"')
        }
        append(']')
        append(",\"matches\":[")
        t.matches.forEachIndexed { i, m ->
            if (i > 0) append(',')
            append(encodeBracketMatch(m))
        }
        append(']')
        append('}')
    }

    fun decodeTournament(json: String): Tournament {
        val map = parseObject(json)
        val players = parseStringArray(json, "players")
        val matches = parseBracketMatches(json)
        return Tournament(
            id = map["id"] ?: "",
            playerCount = map["playerCount"]?.toIntOrNull() ?: players.size,
            defaultBestOf = map["defaultBestOf"]?.toIntOrNull() ?: 3,
            defaultNoAd = map["defaultNoAd"] == "true",
            players = players,
            matches = matches,
            activeMatchId = map["activeMatchId"]?.takeIf { it != "null" },
            champion = map["champion"]?.takeIf { it != "null" },
            createdAtEpochMs = map["createdAtEpochMs"]?.toLongOrNull() ?: 0L,
        )
    }

    private fun encodeBracketMatch(m: BracketMatch): String = buildString {
        append('{')
        append("\"id\":\"").append(escape(m.id)).append('"')
        append(",\"round\":\"").append(escape(m.round.name)).append('"')
        append(",\"slotIndex\":").append(m.slotIndex)
        append(",\"playerA\":")
        if (m.playerA == null) append("null") else append('"').append(escape(m.playerA)).append('"')
        append(",\"playerB\":")
        if (m.playerB == null) append("null") else append('"').append(escape(m.playerB)).append('"')
        append(",\"bestOf\":").append(m.bestOf)
        append(",\"status\":\"").append(escape(m.status.name)).append('"')
        append(",\"winnerSide\":")
        if (m.winnerSide == null) append("null") else append('"').append(escape(m.winnerSide.name)).append('"')
        append(",\"setsA\":").append(m.setsA)
        append(",\"setsB\":").append(m.setsB)
        append(",\"nextMatchId\":")
        if (m.nextMatchId == null) append("null") else append('"').append(escape(m.nextMatchId)).append('"')
        append(",\"nextSlotIsA\":").append(m.nextSlotIsA)
        append('}')
    }

    private fun parseBracketMatches(json: String): List<BracketMatch> {
        val start = json.indexOf("\"matches\"")
        if (start < 0) return emptyList()
        val arrStart = json.indexOf('[', start)
        if (arrStart < 0) return emptyList()
        var depth = 0
        var arrEnd = -1
        for (i in arrStart until json.length) {
            when (json[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        arrEnd = i
                        break
                    }
                }
            }
        }
        if (arrEnd < 0) return emptyList()
        val arr = json.substring(arrStart + 1, arrEnd)
        if (arr.isBlank()) return emptyList()
        val items = mutableListOf<BracketMatch>()
        var objDepth = 0
        var objStart = -1
        for (i in arr.indices) {
            when (arr[i]) {
                '{' -> {
                    if (objDepth == 0) objStart = i
                    objDepth++
                }
                '}' -> {
                    objDepth--
                    if (objDepth == 0 && objStart >= 0) {
                        items.add(decodeBracketMatch(arr.substring(objStart, i + 1)))
                        objStart = -1
                    }
                }
            }
        }
        return items
    }

    private fun decodeBracketMatch(json: String): BracketMatch {
        val map = parseObject(json)
        return BracketMatch(
            id = map["id"] ?: "",
            round = runCatching { TournamentRound.valueOf(map["round"] ?: "FINAL") }
                .getOrDefault(TournamentRound.FINAL),
            slotIndex = map["slotIndex"]?.toIntOrNull() ?: 0,
            playerA = map["playerA"]?.takeIf { it != "null" },
            playerB = map["playerB"]?.takeIf { it != "null" },
            bestOf = map["bestOf"]?.toIntOrNull() ?: 3,
            status = runCatching { BracketMatchStatus.valueOf(map["status"] ?: "PENDING") }
                .getOrDefault(BracketMatchStatus.PENDING),
            winnerSide = map["winnerSide"]?.takeIf { it != "null" }
                ?.let { runCatching { Side.valueOf(it) }.getOrNull() },
            setsA = map["setsA"]?.toIntOrNull() ?: 0,
            setsB = map["setsB"]?.toIntOrNull() ?: 0,
            nextMatchId = map["nextMatchId"]?.takeIf { it != "null" },
            nextSlotIsA = map["nextSlotIsA"] != "false",
        )
    }

    private fun parseStringArray(json: String, key: String): List<String> {
        val start = json.indexOf("\"$key\"")
        if (start < 0) return emptyList()
        val arrStart = json.indexOf('[', start)
        val arrEnd = json.indexOf(']', arrStart)
        if (arrStart < 0 || arrEnd < 0) return emptyList()
        val arr = json.substring(arrStart + 1, arrEnd)
        if (arr.isBlank()) return emptyList()
        val pattern = Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
        return pattern.findAll(arr).map {
            it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
        }.toList()
    }
}
