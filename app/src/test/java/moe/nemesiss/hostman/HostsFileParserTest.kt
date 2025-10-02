package moe.nemesiss.hostman

import io.netty.resolver.HostsFileParser
import org.junit.Test
import java.io.StringReader

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class HostsFileParserTest {
    @Test
    fun parse_empty_string() {
        HostsFileParser.parse(StringReader(""))
    }
}