package com.acepanel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

enum class AppIcon {
    Websites,
    Database,
    Projects,
    Software,
    Certificate,
    Firewall,
    Files,
    Terminal,
    Ssh,
    Tasks,
    Backup,
    Monitor,
    Logs,
    Settings,
    ConfigFile,
    Rewrite,
    Delete,
    Generic
}

@Composable
fun MenuSvgIcon(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    color: Color = icon.defaultColor()
) {
    val imageVector = remember(icon) { icon.toImageVector() }

    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

fun panelFeatureIcon(key: String): AppIcon = when (key) {
    "websites" -> AppIcon.Websites
    "databases" -> AppIcon.Database
    "projects" -> AppIcon.Projects
    "software" -> AppIcon.Software
    "ssl" -> AppIcon.Certificate
    "firewall" -> AppIcon.Firewall
    "files" -> AppIcon.Files
    "terminal" -> AppIcon.Terminal
    "ssh" -> AppIcon.Ssh
    "tasks" -> AppIcon.Tasks
    "backup" -> AppIcon.Backup
    "monitor" -> AppIcon.Monitor
    "logs" -> AppIcon.Logs
    "settings" -> AppIcon.Settings
    else -> AppIcon.Generic
}

fun websiteFeatureIcon(key: String): AppIcon = when (key) {
    "config" -> AppIcon.ConfigFile
    "rewrite" -> AppIcon.Rewrite
    "ssl" -> AppIcon.Certificate
    "directory" -> AppIcon.Files
    "delete" -> AppIcon.Delete
    else -> AppIcon.Generic
}

private fun AppIcon.defaultColor(): Color = when (this) {
    AppIcon.Websites -> Color(0xFF2563EB)
    AppIcon.Database -> Color(0xFF7C3AED)
    AppIcon.Projects -> Color(0xFF0891B2)
    AppIcon.Software -> Color(0xFF16A34A)
    AppIcon.Certificate -> Color(0xFF0F766E)
    AppIcon.Firewall -> Color(0xFFDC2626)
    AppIcon.Files -> Color(0xFFD97706)
    AppIcon.Terminal -> Color(0xFF18181B)
    AppIcon.Ssh -> Color(0xFF4F46E5)
    AppIcon.Tasks -> Color(0xFF2563EB)
    AppIcon.Backup -> Color(0xFF0284C7)
    AppIcon.Monitor -> Color(0xFF059669)
    AppIcon.Logs -> Color(0xFF52525B)
    AppIcon.Settings -> Color(0xFF475569)
    AppIcon.ConfigFile -> Color(0xFF2563EB)
    AppIcon.Rewrite -> Color(0xFF9333EA)
    AppIcon.Delete -> Color(0xFFEF4444)
    AppIcon.Generic -> Color(0xFF71717A)
}

private fun AppIcon.toImageVector(): ImageVector = when (this) {
    AppIcon.Websites -> outlineIcon("Websites") {
        moveTo(4f, 5f)
        horizontalLineTo(20f)
        verticalLineTo(19f)
        horizontalLineTo(4f)
        close()
        moveTo(4f, 9f)
        horizontalLineTo(20f)
        moveTo(8f, 5f)
        verticalLineTo(9f)
        moveTo(8f, 14f)
        lineTo(6f, 16f)
        lineTo(8f, 18f)
        moveTo(16f, 14f)
        lineTo(18f, 16f)
        lineTo(16f, 18f)
        moveTo(11f, 18f)
        lineTo(13f, 14f)
    }
    AppIcon.Database -> outlineIcon("Database") {
        moveTo(5f, 6f)
        curveTo(5f, 4.9f, 8.1f, 4f, 12f, 4f)
        curveTo(15.9f, 4f, 19f, 4.9f, 19f, 6f)
        curveTo(19f, 7.1f, 15.9f, 8f, 12f, 8f)
        curveTo(8.1f, 8f, 5f, 7.1f, 5f, 6f)
        moveTo(5f, 6f)
        verticalLineTo(18f)
        curveTo(5f, 19.1f, 8.1f, 20f, 12f, 20f)
        curveTo(15.9f, 20f, 19f, 19.1f, 19f, 18f)
        verticalLineTo(6f)
        moveTo(5f, 12f)
        curveTo(5f, 13.1f, 8.1f, 14f, 12f, 14f)
        curveTo(15.9f, 14f, 19f, 13.1f, 19f, 12f)
    }
    AppIcon.Projects -> outlineIcon("Projects") {
        moveTo(12f, 4f)
        lineTo(20f, 8f)
        lineTo(12f, 12f)
        lineTo(4f, 8f)
        close()
        moveTo(4f, 12f)
        lineTo(12f, 16f)
        lineTo(20f, 12f)
        moveTo(4f, 16f)
        lineTo(12f, 20f)
        lineTo(20f, 16f)
    }
    AppIcon.Software -> outlineIcon("Software") {
        moveTo(12f, 3.5f)
        lineTo(20f, 8f)
        verticalLineTo(16f)
        lineTo(12f, 20.5f)
        lineTo(4f, 16f)
        verticalLineTo(8f)
        close()
        moveTo(12f, 12f)
        lineTo(20f, 8f)
        moveTo(12f, 12f)
        verticalLineTo(20.5f)
        moveTo(12f, 12f)
        lineTo(4f, 8f)
        moveTo(8f, 5.7f)
        lineTo(16f, 10.3f)
    }
    AppIcon.Certificate -> outlineIcon("Certificate") {
        moveTo(12f, 3.5f)
        lineTo(19f, 6.5f)
        verticalLineTo(11f)
        curveTo(19f, 15.4f, 16.2f, 18.8f, 12f, 21f)
        curveTo(7.8f, 18.8f, 5f, 15.4f, 5f, 11f)
        verticalLineTo(6.5f)
        close()
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15.5f, 9f)
    }
    AppIcon.Firewall -> outlineIcon("Firewall") {
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        horizontalLineTo(4f)
        close()
        moveTo(4f, 11f)
        horizontalLineTo(20f)
        moveTo(4f, 15f)
        horizontalLineTo(20f)
        moveTo(8f, 7f)
        verticalLineTo(11f)
        moveTo(14f, 7f)
        verticalLineTo(11f)
        moveTo(11f, 11f)
        verticalLineTo(15f)
        moveTo(17f, 11f)
        verticalLineTo(15f)
        moveTo(8f, 15f)
        verticalLineTo(18f)
        moveTo(14f, 15f)
        verticalLineTo(18f)
    }
    AppIcon.Files -> outlineIcon("Files") {
        moveTo(3.5f, 7f)
        horizontalLineTo(9.5f)
        lineTo(11.5f, 9f)
        horizontalLineTo(20.5f)
        verticalLineTo(18.5f)
        horizontalLineTo(3.5f)
        close()
        moveTo(3.5f, 7f)
        verticalLineTo(18.5f)
    }
    AppIcon.Terminal -> outlineIcon("Terminal") {
        moveTo(4f, 5f)
        horizontalLineTo(20f)
        verticalLineTo(19f)
        horizontalLineTo(4f)
        close()
        moveTo(7f, 10f)
        lineTo(10f, 12.5f)
        lineTo(7f, 15f)
        moveTo(12.5f, 15f)
        horizontalLineTo(17f)
    }
    AppIcon.Ssh -> outlineIcon("Ssh") {
        moveTo(14f, 9f)
        curveTo(14f, 6.8f, 15.8f, 5f, 18f, 5f)
        curveTo(20.2f, 5f, 22f, 6.8f, 22f, 9f)
        curveTo(22f, 11.2f, 20.2f, 13f, 18f, 13f)
        curveTo(15.8f, 13f, 14f, 11.2f, 14f, 9f)
        moveTo(14.8f, 12.2f)
        lineTo(5f, 22f)
        moveTo(8f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(16f)
        moveTo(11f, 16f)
        horizontalLineTo(8f)
        verticalLineTo(13f)
    }
    AppIcon.Tasks -> outlineIcon("Tasks") {
        moveTo(9f, 6f)
        horizontalLineTo(20f)
        moveTo(9f, 12f)
        horizontalLineTo(20f)
        moveTo(9f, 18f)
        horizontalLineTo(20f)
        moveTo(4f, 6.5f)
        lineTo(5.2f, 7.7f)
        lineTo(7.3f, 5.3f)
        moveTo(4f, 12.5f)
        lineTo(5.2f, 13.7f)
        lineTo(7.3f, 11.3f)
        moveTo(4f, 18.5f)
        lineTo(5.2f, 19.7f)
        lineTo(7.3f, 17.3f)
    }
    AppIcon.Backup -> outlineIcon("Backup") {
        moveTo(17f, 18f)
        horizontalLineTo(18f)
        curveTo(20.2f, 18f, 22f, 16.2f, 22f, 14f)
        curveTo(22f, 11.9f, 20.4f, 10.2f, 18.4f, 10f)
        curveTo(17.6f, 7.1f, 15f, 5f, 12f, 5f)
        curveTo(8.8f, 5f, 6.2f, 7.2f, 5.5f, 10.2f)
        curveTo(3.5f, 10.6f, 2f, 12.2f, 2f, 14.2f)
        curveTo(2f, 16.3f, 3.7f, 18f, 5.8f, 18f)
        horizontalLineTo(7f)
        moveTo(12f, 19f)
        verticalLineTo(11f)
        moveTo(8.8f, 14.2f)
        lineTo(12f, 11f)
        lineTo(15.2f, 14.2f)
    }
    AppIcon.Monitor -> outlineIcon("Monitor") {
        moveTo(4f, 5f)
        horizontalLineTo(20f)
        verticalLineTo(16f)
        horizontalLineTo(4f)
        close()
        moveTo(8f, 20f)
        horizontalLineTo(16f)
        moveTo(12f, 16f)
        verticalLineTo(20f)
        moveTo(7f, 12f)
        lineTo(10f, 9f)
        lineTo(12.2f, 11.2f)
        lineTo(16.5f, 7f)
        lineTo(18f, 8.5f)
    }
    AppIcon.Logs -> outlineIcon("Logs") {
        moveTo(7f, 3f)
        horizontalLineTo(14f)
        lineTo(19f, 8f)
        verticalLineTo(21f)
        horizontalLineTo(7f)
        close()
        moveTo(14f, 3f)
        verticalLineTo(8f)
        horizontalLineTo(19f)
        moveTo(9.5f, 12f)
        horizontalLineTo(16f)
        moveTo(9.5f, 16f)
        horizontalLineTo(16f)
        moveTo(9.5f, 19f)
        horizontalLineTo(13f)
    }
    AppIcon.Settings -> outlineIcon("Settings") {
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(4f, 12f)
        horizontalLineTo(20f)
        moveTo(4f, 17f)
        horizontalLineTo(20f)
        moveTo(8f, 5f)
        verticalLineTo(9f)
        moveTo(16f, 10f)
        verticalLineTo(14f)
        moveTo(11f, 15f)
        verticalLineTo(19f)
    }
    AppIcon.ConfigFile -> outlineIcon("ConfigFile") {
        moveTo(6f, 3f)
        horizontalLineTo(14f)
        lineTo(18f, 7f)
        verticalLineTo(21f)
        horizontalLineTo(6f)
        close()
        moveTo(14f, 3f)
        verticalLineTo(7f)
        horizontalLineTo(18f)
        moveTo(9f, 12f)
        horizontalLineTo(15f)
        moveTo(9f, 16f)
        horizontalLineTo(13f)
    }
    AppIcon.Rewrite -> outlineIcon("Rewrite") {
        moveTo(4f, 7f)
        horizontalLineTo(14f)
        moveTo(11f, 4f)
        lineTo(14f, 7f)
        lineTo(11f, 10f)
        moveTo(20f, 17f)
        horizontalLineTo(10f)
        moveTo(13f, 14f)
        lineTo(10f, 17f)
        lineTo(13f, 20f)
        moveTo(7f, 7f)
        curveTo(7f, 12f, 17f, 12f, 17f, 17f)
    }
    AppIcon.Delete -> outlineIcon("Delete") {
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(10f, 11f)
        verticalLineTo(17f)
        moveTo(14f, 11f)
        verticalLineTo(17f)
        moveTo(6f, 7f)
        lineTo(7f, 21f)
        horizontalLineTo(17f)
        lineTo(18f, 7f)
        moveTo(9f, 7f)
        verticalLineTo(4f)
        horizontalLineTo(15f)
        verticalLineTo(7f)
    }
    AppIcon.Generic -> outlineIcon("Generic") {
        moveTo(5f, 5f)
        horizontalLineTo(11f)
        verticalLineTo(11f)
        horizontalLineTo(5f)
        close()
        moveTo(13f, 5f)
        horizontalLineTo(19f)
        verticalLineTo(11f)
        horizontalLineTo(13f)
        close()
        moveTo(5f, 13f)
        horizontalLineTo(11f)
        verticalLineTo(19f)
        horizontalLineTo(5f)
        close()
        moveTo(13f, 13f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(13f)
        close()
    }
}

private fun outlineIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            block()
        }
    }.build()
