/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareResult
import com.mobilebytelabs.kmptoolkit.share.compose.rememberShareCapabilities
import com.mobilebytelabs.kmptoolkit.share.compose.rememberShareManager
import kotlinx.coroutines.launch

/**
 * The injected path, end to end.
 *
 * Nothing here constructs a `ShareManager`: [App] resolves it from Koin (`shareModule`) and
 * provides it, this screen reads it back with [rememberShareManager]. Every button is gated on
 * `supports(payload)` — which is the behaviour the capability matrix exists for. Run this on tvOS
 * or Windows and the binary buttons disable themselves instead of failing after a tap.
 */
@Composable
fun ShareManagerScreen() {
    val share = rememberShareManager()
    val caps = rememberShareCapabilities()
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf<String?>(null) }

    val textPayload = SharePayload.Text("Sent through the injected ShareManager.")
    val urlPayload = SharePayload.Url("https://github.com/mobilebytelabs/kmptoolkit")
    val filePayload = SharePayload.File("file:///tmp/kmptoolkit-report.pdf", "application/pdf")

    fun run(label: String, block: suspend () -> ShareResult) {
        scope.launch {
            lastResult = when (val r = block()) {
                is ShareResult.Completed -> "$label → Completed"
                is ShareResult.Cancelled -> "$label → Cancelled by user"
                is ShareResult.Failed -> "$label → Failed: ${r.cause}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Injected ShareManager", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Resolved from Koin's shareModule, provided via LocalShareManager, read back with " +
                "rememberShareManager(). Buttons disable themselves where this platform cannot " +
                "carry the payload.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Capabilities on ${getPlatform().name}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                CapabilityRow("text", caps.text)
                CapabilityRow("url", caps.url)
                CapabilityRow("image", caps.image)
                CapabilityRow("file", caps.file)
                CapabilityRow("multi", caps.multi)
            }
        }

        Button(
            onClick = { run("shareText") { share.shareText("Sent through the injected ShareManager.") } },
            enabled = share.supports(textPayload),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Share text") }

        Button(
            onClick = { run("shareUrl") { share.shareUrl("https://github.com/mobilebytelabs/kmptoolkit") } },
            enabled = share.supports(urlPayload),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Share URL") }

        Button(
            onClick = {
                // One call for "file + message" — this is the case that used to force callers to
                // assemble a SharePayload.Multi by hand.
                run("shareFile + message") {
                    share.shareFile(
                        fileUri = "file:///tmp/kmptoolkit-report.pdf",
                        mimeType = "application/pdf",
                        message = "Latest report",
                    )
                }
            },
            enabled = share.supports(filePayload),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Share file + message") }

        lastResult?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun CapabilityRow(name: String, supported: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        Text(if (supported) "yes" else "no", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
