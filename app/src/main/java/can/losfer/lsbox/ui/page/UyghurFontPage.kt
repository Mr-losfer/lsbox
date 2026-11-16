package can.losfer.lsbox.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import can.losfer.lsbox.R
import can.losfer.lsbox.ui.components.AppTopBarScaffold
import can.losfer.lsbox.util.UyghurFont
import can.losfer.lsbox.util.UyghurFontManager

@Composable
fun UyghurFontPage(
    onBack: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
) {
    val context = LocalContext.current
    var current by remember { mutableStateOf(UyghurFontManager.current()) }

    AppTopBarScaffold(
        topBarStartInset = contentPadding.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
        titleContent = {
            Text(
                text = stringResource(R.string.font_page_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) { topPad, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(topPad))
            Text(
                text = stringResource(R.string.font_page_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(UyghurFontManager.available()) { font ->
                    val typeface = remember(font) { UyghurFontManager.previewTypeface(context, font) }
                    val selected = font == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .clickable {
                                UyghurFontManager.set(font)
                                current = font
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = {
                            UyghurFontManager.set(font)
                            current = font
                        })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = font.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            androidx.compose.material3.Text(
                                text = "ئۇيغۇرچە خەت نۇسخىسى — ھەرىپ نۇسخىسى",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily(typeface),
                                    fontSize = 18.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
