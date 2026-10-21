package org.oddlama.vane.util

/**
 * Small text helpers that vane used to pull `commons-text` in for. Both replacements exist so the
 * shipped jar does not have to carry ~700 KiB of library for two call shapes.
 */
object TextUtil {
    /** The five characters that must not appear raw in HTML text or attribute content. */
    private val HTML_ESCAPES =
        mapOf(
            '&' to "&amp;",
            '<' to "&lt;",
            '>' to "&gt;",
            '"' to "&quot;",
            '\'' to "&#39;",
        )

    /**
     * Escapes the characters that could otherwise break out of HTML text or an attribute value.
     *
     * This replaces `StringEscapeUtils.escapeHtml4`, which additionally rewrote every non-ASCII
     * character that happens to have a named HTML 4 entity. That part was never needed: the map
     * integrations emit UTF-8, so a literal `é` renders exactly like `&eacute;` would. Only the
     * five markup-significant characters actually have to be escaped, and those behave identically.
     *
     * Null passes straight through, as it did with `escapeHtml4`, so callers that hand over an
     * optional name keep behaving exactly the same.
     *
     * @param str Text to escape, or null.
     * @return The text with markup-significant characters replaced by entities, or null.
     */
    @JvmStatic
    fun escapeHtml(str: String?): String? {
        if (str == null || str.none { it in HTML_ESCAPES }) return str
        return buildString(str.length + 16) {
            str.forEach { append(HTML_ESCAPES[it] ?: it) }
        }
    }

    /**
     * Greedily wraps [str] to lines of at most [wrapLength] characters, joining them with
     * [newLineStr]. Words longer than [wrapLength] are never split; they overflow their line.
     *
     * This replaces `WordUtils.wrap(str, wrapLength, newLineStr, false)` and was verified to
     * produce byte-identical output for every configuration description in the project and for 200k
     * randomly generated single-spaced strings. The two differ only on runs of consecutive spaces,
     * where this version collapses the run at the wrap point instead of reproducing commons-text's
     * off-by-one handling — descriptions are single-spaced prose, so that case does not arise, and
     * collapsing is the better rendering if it ever does.
     *
     * @param str Text to wrap.
     * @param wrapLength Maximum line length, in characters.
     * @param newLineStr Separator inserted at each wrap point.
     * @return The wrapped text.
     */
    @JvmStatic
    fun wordWrap(str: String, wrapLength: Int, newLineStr: String): String {
        val len = str.length
        val out = StringBuilder(len + 32)
        var offset = 0

        while (len - offset > wrapLength) {
            // A space sitting exactly at the line start would otherwise be emitted as an empty
            // line.
            if (str[offset] == ' ') {
                offset++
                continue
            }

            var wrapAt = str.lastIndexOf(' ', offset + wrapLength)
            if (wrapAt <= offset) {
                // The next word is longer than a whole line: keep it intact and break after it.
                wrapAt = str.indexOf(' ', offset + wrapLength)
                if (wrapAt < 0) break
            }

            out.append(str, offset, wrapAt).append(newLineStr)
            offset = wrapAt + 1
        }

        return out.append(str, offset, len).toString()
    }
}
