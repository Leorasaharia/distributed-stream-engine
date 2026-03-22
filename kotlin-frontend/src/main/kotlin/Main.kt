import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay

@Composable
@Preview
fun App() {
    var jobStatus by remember { mutableStateOf(JobStatus()) }
    var userMetrics by remember { mutableStateOf(emptyMap<String, String>()) }
    val apiClient = remember { ApiClient() }

    // Start a coroutine to poll the API every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            jobStatus = apiClient.getStatus()
            userMetrics = apiClient.getMetrics()
            delay(2000L)
        }
    }

    MaterialTheme(colors = darkColors()) {
        Surface(color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Distributed Stream Engine Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard("Jobs Dispatched", jobStatus.jobs_dispatched, Modifier.weight(1f))
                    StatCard("Jobs Processed", jobStatus.jobs_processed, Modifier.weight(1f))
                    StatCard("Engine Status", jobStatus.status, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Top Active Users (Real-Time)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Sort metrics to show top users explicitly
                val topUsers = userMetrics.entries
                    .sortedByDescending { it.value.toIntOrNull() ?: 0 }
                    .take(15)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topUsers) { entry ->
                        UserMetricCard(entry.key, entry.value)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 14.sp, color = Color.Gray)
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface)
        }
    }
}

@Composable
fun UserMetricCard(userId: String, eventCount: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = Color(0xFF2C2C2C)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "User $userId", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(text = "$eventCount events", fontSize = 16.sp, color = MaterialTheme.colors.secondary)
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Stream Engine Dashboard"
    ) {
        App()
    }
}
