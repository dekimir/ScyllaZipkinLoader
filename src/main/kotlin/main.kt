import com.datastax.driver.core.Cluster
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

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
    var spans = mapOf<UUID, Span>().toMutableMap()
    val cluster = Cluster.builder().addContactPoints("localhost").build()
    val session = cluster.connect("system_traces")
    val traceSessions = session.execute("SELECT * FROM system_traces.sessions")
    val eventQueryStmt = session.prepare("SELECT * FROM system_traces.events where session_id=?")
    traceSessions.forEach { s ->
        val events = session.execute(eventQueryStmt.bind(s.getUUID("session_id")))
        events.forEach { r ->
            val id = r.getUUID("event_id")
            if (id !in spans) {
                spans[id] = Span(
                    id.toString(), "", "", "",
                    0, 0, "",
                    Endpoint("", 0), Endpoint("", 0), emptyList<Annotation>().toMutableList())
            }
        }
    }
    println(Json.encodeToString(spans.values.toList()))
}
