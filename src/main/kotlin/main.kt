import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Row
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Endpoint(
    val ipv4: String,
    val port: Int
)

@Serializable
data class Annotation(
    val timestamp: Int,
    val value: String
)

@Serializable
data class Span(
    val id: String,
    val traceId: String,
    val parentId: String,
    val name: String,
    val timestamp: Int,
    val duration: Int,
    val kind: String,
    val localEndpoint: Endpoint,
    val remoteEndpoint: Endpoint,
    val annotations: MutableList<Annotation>
)

fun main(args: Array<String>) {
    var spans = mapOf<String, Span>().toMutableMap()
    val cluster = Cluster.builder().addContactPoints("localhost").build()
    val session = cluster.connect("system_traces")
    val traceSessions = session.execute("SELECT * FROM system_traces.sessions")
    val eventQueryStmt = session.prepare("SELECT * FROM system_traces.events where session_id=?")
    traceSessions.forEach { s ->
        val sessionID = s.getUUID("session_id")
        val events = session.execute(eventQueryStmt.bind(sessionID))
        events.forEach { r ->
            val id = getLongAsLowHex(r, "scylla_span_id")
            val parentId = getLongAsLowHex(r, "scylla_parent_id")
            if (id !in spans) {
                spans[id] = Span(
                    id, sessionID.toString().replace("-", "").takeLast(16), parentId, "",
                    0, 0, "",
                    Endpoint("", 0), Endpoint("", 0), emptyList<Annotation>().toMutableList())
            }
        }
    }
    println(Json.encodeToString(spans.values.toList()))
}

private fun getLongAsLowHex(row: Row, column: String) = row.getLong(column).toString().takeLast(16)
