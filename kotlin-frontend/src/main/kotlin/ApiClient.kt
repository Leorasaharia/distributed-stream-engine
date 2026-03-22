import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class JobStatus(
    val jobs_dispatched: String = "0",
    val jobs_processed: String = "0",
    val status: String = "UNKNOWN"
)

class ApiClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "http://localhost:8080/api/jobs"

    suspend fun getStatus(): JobStatus {
        return try {
            client.get("$baseUrl/status").body()
        } catch (e: Exception) {
            e.printStackTrace()
            JobStatus()
        }
    }

    suspend fun getMetrics(): Map<String, String> {
        return try {
            client.get("$baseUrl/metrics").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
    
    fun close() {
        client.close()
    }
}
