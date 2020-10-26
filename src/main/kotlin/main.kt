import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Row
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.math.max

@Serializable
data class Endpoint(
    val serviceName: String,
    val ipv4: String,
    val port: Int
)

@Serializable
data class Annotation(
    val timestamp: Long,
    val value: String
)

@Serializable
data class Span(
    val id: String,
    val traceId: String,
    val parentId: String,
    val name: String,
    var timestamp: Long,
    var duration: Long,
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
            val shard_id = r.getString("thread")
            val id = getLongAsLowHex(r, "scylla_span_id")
            val parentId = getLongAsLowHex(r, "scylla_parent_id")
            if (id !in spans) {
                val inet = r.getInet("source")
                spans[id] = Span(
                    id, sessionID.toString().replace("-","").take(32), parentId, shard_id,
                    Long.MAX_VALUE, 0, "SERVER",
                    Endpoint(inet.hostAddress, inet.hostAddress, 7000),
                    Endpoint(inet.hostAddress, inet.hostAddress, 7000),
                    emptyList<Annotation>().toMutableList())
            }
            val start = r.getUUID("event_id").timestamp() / 10
            spans[id]!!.timestamp = min(spans[id]!!.timestamp, start)
            // The span duration is dictated by the last event in it.  But we don't know when an event ends -- that's
            // not recorded in Scylla.  Absent that information, let's assume each event lasts 1us and look for the
            // latest one.
            val possibleNewDuration = start + 1 - spans[id]!!.timestamp
            spans[id]!!.duration = max(spans[id]!!.duration, possibleNewDuration)
            spans[id]!!.annotations.add(Annotation(start, r.getString("activity")))
        }
    }
    println(Json.encodeToString(spans.values.toList()))
}

private fun getLongAsLowHex(row: Row, column: String) = row.getLong(column).toString().takeLast(16)
