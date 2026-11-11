package com.pwexpress.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones Lucide (os mesmos do site), gerados a partir do pacote lucide-react.
 * Licença ISC · https://lucide.dev · Para adicionar: inclua o nome no gerador e rode de novo.
 */
object PwIcones {
    private fun icone(nome: String, vararg caminhos: String): ImageVector {
        val b = ImageVector.Builder(name = nome, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        caminhos.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val MapPin: ImageVector by lazy { icone("map-pin", "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0", "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val MapPinned: ImageVector by lazy { icone("map-pinned", "M18 8c0 3.613-3.869 7.429-5.393 8.795a1 1 0 01-1.214 0C9.87 15.429 6 11.613 6 8a6 6 0 0112 0", "M4.474 15h-.197a1 1 0 00-.969.753l-1.097 4.35a1.5 1.5 0 001.444 1.898L20.344 22a1.5 1.5 0 001.446-1.897l-1.098-4.35a1 1 0 00-.969-.753h-.197", "M10 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Navigation: ImageVector by lazy { icone("navigation", "M3 11L22 2L13 21L11 13L3 11Z") }
    val Phone: ImageVector by lazy { icone("phone", "M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384") }
    val Check: ImageVector by lazy { icone("check", "M20 6 9 17l-5-5") }
    val CircleCheck: ImageVector by lazy { icone("circle-check", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "m16 9-5.5 5.5L8 12") }
    val X: ImageVector by lazy { icone("x", "M18 6 6 18", "m6 6 12 12") }
    val FlaskConical: ImageVector by lazy { icone("flask-conical", "M14 2v6a2 2 0 0 0 .245.96l5.51 10.08A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96l5.51-10.08A2 2 0 0 0 10 8V2", "M6.453 15h11.094", "M8.5 2h7") }
    val UserRound: ImageVector by lazy { icone("user-round", "M7 8a5 5 0 1 0 10 0a5 5 0 1 0 -10 0", "M20 21a8 8 0 0 0-16 0") }
    val Camera: ImageVector by lazy { icone("camera", "M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z", "M9 13a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val Image: ImageVector by lazy { icone("image", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z", "M7 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21") }
    val LogOut: ImageVector by lazy { icone("log-out", "m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4") }
    val Sun: ImageVector by lazy { icone("sun", "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41") }
    val Moon: ImageVector by lazy { icone("moon", "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401") }
    val Monitor: ImageVector by lazy { icone("monitor", "M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2Z", "M8 21L16 21", "M12 17L12 21") }
    // desenhados para a Delibra, no mesmo traço do Lucide
    val Capacete: ImageVector by lazy { icone("capacete", "M3 16a9 9 0 0 1 17.6-2.6", "M3 16v1.5A2.5 2.5 0 0 0 5.5 20h12a2.5 2.5 0 0 0 2.5-2.5V17", "M21 11.5h-6.5a2 2 0 0 0-2 2v1.5a2 2 0 0 0 2 2H21", "M6.5 15.5h3") }
    val Moto: ImageVector by lazy { icone("moto", "M2 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M16 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M8 17h6l3.2-5.2", "M5 17l3.5-6H14l2 3", "M14.5 7H17l2 10", "M9 8.5h3.5") }
    val ShoppingBag: ImageVector by lazy { icone("shopping-bag", "M16 10a4 4 0 0 1-8 0", "M3.103 6.034h17.794", "M3.4 5.467a2 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z") }
    val Pill: ImageVector by lazy { icone("pill", "m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7") }
    val Key: ImageVector by lazy { icone("key", "m15.5 7.5 2.3 2.3a1 1 0 0 0 1.4 0l2.1-2.1a1 1 0 0 0 0-1.4L19 4", "m21 2-9.6 9.6", "M2 15.5a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0 -11 0") }
    val Bike: ImageVector by lazy { icone("bike", "M15 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0", "M2 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0", "M14 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0", "M12 17.5V14l-3-3 4-3 2 3h2") }
    val Wallet: ImageVector by lazy { icone("wallet", "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1", "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4") }
    val Timer: ImageVector by lazy { icone("timer", "M10 2L14 2", "M12 14L15 11", "M4 14a8 8 0 1 0 16 0a8 8 0 1 0 -16 0") }
    val Crosshair: ImageVector by lazy { icone("crosshair", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M22 12L18 12", "M6 12L2 12", "M12 6L12 2", "M12 22L12 18") }
    val BatteryMedium: ImageVector by lazy { icone("battery-medium", "M10 14v-4", "M22 14v-4", "M6 14v-4", "M4 6h12a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2Z") }
    val BatteryLow: ImageVector by lazy { icone("battery-low", "M22 14v-4", "M6 14v-4", "M4 6h12a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2Z") }
    val Power: ImageVector by lazy { icone("power", "M12 2v10", "M18.4 6.6a9 9 0 1 1-12.77.04") }
    val Radio: ImageVector by lazy { icone("radio", "M16.247 7.761a6 6 0 0 1 0 8.478", "M19.075 4.933a10 10 0 0 1 0 14.134", "M4.925 19.067a10 10 0 0 1 0-14.134", "M7.753 16.239a6 6 0 0 1 0-8.478", "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Package: ImageVector by lazy { icone("package", "M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z", "M12 22V12", "M3.29 7L12 12L20.71 7", "m7.5 4.27 9 5.15") }
    val PackageCheck: ImageVector by lazy { icone("package-check", "M12 22V12", "m16 17 2 2 4-4", "M21 11.127V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.729l7 4a2 2 0 0 0 2 .001l1.32-.753", "M3.29 7 12 12l8.71-5", "m7.5 4.27 8.997 5.148") }
    val Route: ImageVector by lazy { icone("route", "M3 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15", "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val ChevronsRight: ImageVector by lazy { icone("chevrons-right", "m6 17 5-5-5-5", "m13 17 5-5-5-5") }
    val Settings2: ImageVector by lazy { icone("settings-2", "M14 17H5", "M19 7h-9", "M14 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M4 7a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val Bug: ImageVector by lazy { icone("bug", "M12 20v-9", "M14 7a4 4 0 0 1 4 4v3a6 6 0 0 1-12 0v-3a4 4 0 0 1 4-4z", "M14.12 3.88 16 2", "M21 21a4 4 0 0 0-3.81-4", "M21 5a4 4 0 0 1-3.55 3.97", "M22 13h-4", "M3 21a4 4 0 0 1 3.81-4", "M3 5a4 4 0 0 0 3.55 3.97", "M6 13H2", "m8 2 1.88 1.88", "M9 7.13V6a3 3 0 1 1 6 0v1.13") }
    val ShieldCheck: ImageVector by lazy { icone("shield-check", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "m9 12 2 2 4-4") }
    val PenLine: ImageVector by lazy { icone("pen-line", "M13 21h8", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z") }
    val KeyRound: ImageVector by lazy { icone("key-round", "M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z", "M16 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0") }
    val ArrowLeftRight: ImageVector by lazy { icone("arrow-left-right", "M8 3 4 7l4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4") }
    val PanelTop: ImageVector by lazy { icone("panel-top", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z", "M3 9h18") }
    val Bell: ImageVector by lazy { icone("bell", "M10.268 21a2 2 0 0 0 3.464 0", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326") }
    val CreditCard: ImageVector by lazy { icone("credit-card", "M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z", "M2 10h20") }
    val Unplug: ImageVector by lazy { icone("unplug", "m19 5 3-3", "m2 22 3-3", "M6.3 20.3a2.4 2.4 0 0 0 3.4 0L12 18l-6-6-2.3 2.3a2.4 2.4 0 0 0 0 3.4Z", "M7.5 13.5 10 11", "M10.5 16.5 13 14", "m12 6 6 6 2.3-2.3a2.4 2.4 0 0 0 0-3.4l-2.6-2.6a2.4 2.4 0 0 0-3.4 0Z") }
    val PlugZap: ImageVector by lazy { icone("plug-zap", "M6.3 20.3a2.4 2.4 0 0 0 3.4 0L12 18l-6-6-2.3 2.3a2.4 2.4 0 0 0 0 3.4Z", "m2 22 3-3", "M7.5 13.5 10 11", "M10.5 16.5 13 14", "m18 3-4 4h6l-4 4") }
    val Link2: ImageVector by lazy { icone("link-2", "M9 17H7A5 5 0 0 1 7 7h2", "M15 7h2a5 5 0 1 1 0 10h-2", "M8 12h8") }
    val Gavel: ImageVector by lazy { icone("gavel", "m14 13-8.381 8.38a1 1 0 0 1-3.001-3L11 9.999", "M15.973 4.027 20 8", "M12.5 6.5 17.5 11.5", "m8 8 6-6", "m16 16 6-6", "M9 9l6 6") }
    val UserX: ImageVector by lazy { icone("user-x", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z", "m17 8 5 5", "m22 8-5 5") }
    val Clock: ImageVector by lazy { icone("clock", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 6v6l4 2") }
    val ArrowUpFromLine: ImageVector by lazy { icone("arrow-up-from-line", "m18 9-6-6-6 6", "M12 3v14", "M5 21h14") }
    val ArrowDownToLine: ImageVector by lazy { icone("arrow-down-to-line", "M12 17V3", "m6 11 6 6 6-6", "M19 21H5") }
    val LocateFixed: ImageVector by lazy { icone("locate-fixed", "M2 12L5 12", "M19 12L22 12", "M12 2L12 5", "M12 19L12 22", "M5 12a7 7 0 1 0 14 0a7 7 0 1 0 -14 0", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val BellRing: ImageVector by lazy { icone("bell-ring", "M10.268 21a2 2 0 0 0 3.464 0", "M22 8c0-2.3-.8-4.3-2-6", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326", "M4 2C2.8 3.7 2 5.7 2 8") }
    val Volume2: ImageVector by lazy { icone("volume-2", "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "M16 9a5 5 0 0 1 0 6", "M19.364 18.364a9 9 0 0 0 0-12.728") }
    val RefreshCw: ImageVector by lazy { icone("refresh-cw", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5") }
    val ChevronLeft: ImageVector by lazy { icone("chevron-left", "m15 18-6-6 6-6") }
    val Alerta: ImageVector by lazy { icone("triangle-exclamation-point", "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01") }
    val Sparkles: ImageVector by lazy { icone("sparkles", "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z", "M20 2v4", "M22 4h-4", "M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Pencil: ImageVector by lazy { icone("pencil", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4") }
    val Send: ImageVector by lazy { icone("send", "M14.536 21.686a.5.5 0 0 0 .937-.024l6.5-19a.496.496 0 0 0-.635-.635l-19 6.5a.5.5 0 0 0-.024.937l7.93 3.18a2 2 0 0 1 1.112 1.11z", "m21.854 2.147-10.94 10.939") }
    val EyeOff: ImageVector by lazy { icone("eye-off", "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49", "M14.084 14.158a3 3 0 0 1-4.242-4.242", "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143", "m2 2 20 20") }
    val Maximize2: ImageVector by lazy { icone("maximize-2", "M15 3h6v6", "m21 3-7 7", "m3 21 7-7", "M9 21H3v-6") }
    val Mapa: ImageVector by lazy { icone("map", "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z", "M15 5.764v15", "M9 3.236v15") }
    val Building2: ImageVector by lazy { icone("building-2", "M10 12h4", "M10 8h4", "M14 21v-3a2 2 0 0 0-4 0v3", "M6 10H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-2", "M6 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16") }
    val Users: ImageVector by lazy { icone("users", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M16 3.128a4 4 0 0 1 0 7.744", "M22 21v-2a4 4 0 0 0-3-3.87", "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0") }
    val Plus: ImageVector by lazy { icone("plus", "M5 12h14", "M12 5v14") }
    val Minus: ImageVector by lazy { icone("minus", "M5 12h14") }
    val Trash2: ImageVector by lazy { icone("trash-2", "M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2") }
    val Lock: ImageVector by lazy { icone("lock", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-7a2 2 0 0 1 2 -2Z", "M7 11V7a5 5 0 0 1 10 0v4") }
    val LockOpen: ImageVector by lazy { icone("lock-open", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-7a2 2 0 0 1 2 -2Z", "M7 11V7a5 5 0 0 1 9.9-1") }
    val UserPlus: ImageVector by lazy { icone("user-plus", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0", "M19 8L19 14", "M22 11L16 11") }
    val Search: ImageVector by lazy { icone("search", "m21 21-4.34-4.34", "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0") }
    val Store: ImageVector by lazy { icone("store", "M15 21v-5a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v5", "M17.774 10.31a1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.451 0 1.12 1.12 0 0 0-1.548 0 2.5 2.5 0 0 1-3.452 0 1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.77-3.248l2.889-4.184A2 2 0 0 1 7 2h10a2 2 0 0 1 1.653.873l2.895 4.192a2.5 2.5 0 0 1-3.774 3.244", "M4 10.95V19a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8.05") }
    val Landmark: ImageVector by lazy { icone("landmark", "M10 18v-7", "M11.119 2.205a2 2 0 0 1 1.762 0l7.84 3.846A.5.5 0 0 1 20.5 7h-17a.5.5 0 0 1-.22-.949z", "M14 18v-7", "M18 18v-7", "M3 22h18", "M6 18v-7") }
    val Repeat: ImageVector by lazy { icone("repeat", "m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3") }
    val CalendarClock: ImageVector by lazy { icone("calendar-clock", "M16 14v2.2l1.6 1", "M16 2v3", "M21 7.338V5a2 2 0 00-2-2H5a2 2 0 00-2 2v14a2 2 0 002 2h2.338", "M3 9h5.859", "M8 2v3", "M10 16a6 6 0 1 0 12 0a6 6 0 1 0 -12 0") }
    val CalendarCheck: ImageVector by lazy { icone("calendar-check", "M8 2v3", "M16 2v3", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z", "M3 9h18", "m9 15 2 2 4-4") }
    val CalendarX: ImageVector by lazy { icone("calendar-x", "M8 2v3", "M16 2v3", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2Z", "M3 9h18", "m14 13-4 4", "m10 13 4 4") }
    val ChevronRight: ImageVector by lazy { icone("chevron-right", "m9 18 6-6-6-6") }
    val ChevronDown: ImageVector by lazy { icone("chevron-down", "m6 9 6 6 6-6") }
    val History: ImageVector by lazy { icone("history", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2") }
    val Lista: ImageVector by lazy { icone("list", "M3 5h.01", "M3 12h.01", "M3 19h.01", "M8 5h13", "M8 12h13", "M8 19h13") }
    val Zap: ImageVector by lazy { icone("zap", "M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z") }
    val Download: ImageVector by lazy { icone("download", "M12 15V3", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5") }
    val CircleX: ImageVector by lazy { icone("circle-x", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "m15 9-6 6", "m9 9 6 6") }
    val Ban: ImageVector by lazy { icone("ban", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M4.929 4.929 19.07 19.071") }
    val Hand: ImageVector by lazy { icone("hand", "M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2", "M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v2", "M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8", "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15") }
    val TrendingUp: ImageVector by lazy { icone("trending-up", "M16 7h6v6", "m22 7-8.5 8.5-5-5L2 17") }
    val Star: ImageVector by lazy { icone("star", "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z") }
    val Copy: ImageVector by lazy { icone("copy", "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2Z", "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2") }
    val Eye: ImageVector by lazy { icone("eye", "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0", "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val UserRoundPen: ImageVector by lazy { icone("user-round-pen", "M2 21a8 8 0 0 1 10.821-7.487", "M21.378 16.626a1 1 0 0 0-3.004-3.004l-4.01 4.012a2 2 0 0 0-.506.854l-.837 2.87a.5.5 0 0 0 .62.62l2.87-.837a2 2 0 0 0 .854-.506z", "M5 8a5 5 0 1 0 10 0a5 5 0 1 0 -10 0") }
    val Receipt: ImageVector by lazy { icone("receipt", "M12 17V7", "M16 8h-6a2 2 0 0 0 0 4h4a2 2 0 0 1 0 4H8", "M4 3a1 1 0 0 1 1-1 1.3 1.3 0 0 1 .7.2l.933.6a1.3 1.3 0 0 0 1.4 0l.934-.6a1.3 1.3 0 0 1 1.4 0l.933.6a1.3 1.3 0 0 0 1.4 0l.933-.6a1.3 1.3 0 0 1 1.4 0l.934.6a1.3 1.3 0 0 0 1.4 0l.933-.6A1.3 1.3 0 0 1 19 2a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1 1.3 1.3 0 0 1-.7-.2l-.933-.6a1.3 1.3 0 0 0-1.4 0l-.934.6a1.3 1.3 0 0 1-1.4 0l-.933-.6a1.3 1.3 0 0 0-1.4 0l-.933.6a1.3 1.3 0 0 1-1.4 0l-.934-.6a1.3 1.3 0 0 0-1.4 0l-.933.6a1.3 1.3 0 0 1-.7.2 1 1 0 0 1-1-1z") }
    val Ruler: ImageVector by lazy { icone("ruler", "M21.3 15.3a2.4 2.4 0 0 1 0 3.4l-2.6 2.6a2.4 2.4 0 0 1-3.4 0L2.7 8.7a2.41 2.41 0 0 1 0-3.4l2.6-2.6a2.41 2.41 0 0 1 3.4 0Z", "m14.5 12.5 2-2", "m11.5 9.5 2-2", "m8.5 6.5 2-2", "m17.5 15.5 2-2") }
    val Radar: ImageVector by lazy { icone("radar", "M19.07 4.93A10 10 0 0 0 6.99 3.34", "M4 6h.01", "M2.29 9.62A10 10 0 1 0 21.31 8.35", "M16.24 7.76A6 6 0 1 0 8.23 16.67", "M12 18h.01", "M17.99 11.66A6 6 0 0 1 15.77 16.67", "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0", "m13.41 10.59 5.66-5.66") }
    val Info: ImageVector by lazy { icone("info", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 16v-4", "M12 8h.01") }
    val House: ImageVector by lazy { icone("house", "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val ArrowLeft: ImageVector by lazy { icone("arrow-left", "m12 19-7-7 7-7", "M19 12H5") }
    val Mail: ImageVector by lazy { icone("mail", "m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7", "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2Z") }
    val Menu: ImageVector by lazy { icone("menu", "M4 5h16", "M4 12h16", "M4 19h16") }
    val CircleArrowUp: ImageVector by lazy { icone("circle-arrow-up", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "m16 12-4-4-4 4", "M12 16V8") }
    val Shield: ImageVector by lazy { icone("shield", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z") }
    val LayoutDashboard: ImageVector by lazy { icone("layout-dashboard", "M4 3h5a1 1 0 0 1 1 1v7a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-7a1 1 0 0 1 1 -1Z", "M15 3h5a1 1 0 0 1 1 1v3a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-3a1 1 0 0 1 1 -1Z", "M15 12h5a1 1 0 0 1 1 1v7a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-7a1 1 0 0 1 1 -1Z", "M4 16h5a1 1 0 0 1 1 1v3a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-3a1 1 0 0 1 1 -1Z") }
    val SlidersHorizontal: ImageVector by lazy { icone("sliders-horizontal", "M10 5H3", "M12 19H3", "M14 3v4", "M16 17v4", "M21 12h-9", "M21 19h-5", "M21 5h-7", "M8 10v4", "M8 12H3") }
    val FileText: ImageVector by lazy { icone("file-text", "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z", "M14 2v5a1 1 0 0 0 1 1h5", "M10 9H8", "M16 13H8", "M16 17H8") }
    val ArrowUp: ImageVector by lazy { icone("arrow-up", "m5 12 7-7 7 7", "M12 19V5") }
    val ArrowDown: ImageVector by lazy { icone("arrow-down", "M12 5v14", "m19 12-7 7-7-7") }
    val ChevronUp: ImageVector by lazy { icone("chevron-up", "m18 15-6-6-6 6") }
    val ListChecks: ImageVector by lazy { icone("list-checks", "M13 5h8", "M13 12h8", "M13 19h8", "m3 17 2 2 4-4", "m3 7 2 2 4-4") }
    val SunMoon: ImageVector by lazy { icone("sun-moon", "M12 2v2", "M14.837 16.385a6 6 0 1 1-7.223-7.222c.624-.147.97.66.715 1.248a4 4 0 0 0 5.26 5.259c.589-.255 1.396.09 1.248.715", "M16 12a4 4 0 0 0-4-4", "m19 5-1.256 1.256", "M20 12h2") }
    val SignalZero: ImageVector by lazy { icone("signal-zero", "M2 20h.01") }
    val User: ImageVector by lazy { icone("user", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0") }
    val Layers: ImageVector by lazy { icone("layers", "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z", "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12", "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17") }
    val Flame: ImageVector by lazy { icone("flame", "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z") }
    val Megaphone: ImageVector by lazy { icone("megaphone", "m3 11 18-5v12L3 14v-3z", "M11.6 16.8a3 3 0 1 1-5.8-1.6") }
    val Package2: ImageVector by lazy { icone("package-check", "m16 16 2 2 4-4", "M21 10V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l2-1.14", "m7.5 4.27 9 5.15", "M3.29 7 12 12l8.71-5", "M12 22V12") }
    val ReceiptText: ImageVector by lazy { icone("receipt-text", "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z", "M14 8H8", "M16 12H8", "M13 16H8") }
    val ClipboardList: ImageVector by lazy { icone("clipboard-list", "M8 2h8v4H8z", "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2", "M12 11h4", "M12 16h4", "M8 11h.01", "M8 16h.01") }
    val Crown: ImageVector by lazy { icone("crown", "M11.562 3.266a.5.5 0 0 1 .876 0L15.39 8.87a1 1 0 0 0 1.516.294L21.183 5.5a.5.5 0 0 1 .798.519l-2.834 10.246a1 1 0 0 1-.956.734H5.81a1 1 0 0 1-.957-.734L2.02 6.02a.5.5 0 0 1 .798-.519l4.276 3.664a1 1 0 0 0 1.516-.294z", "M5 21h14") }
    val Shuffle: ImageVector by lazy { icone("shuffle", "m18 14 4 4-4 4", "m18 2 4 4-4 4", "M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-7.6a4 4 0 0 1 3.3-1.7H22", "M2 6h1.972a4 4 0 0 1 3.6 2.2", "M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45") }
    val Percent: ImageVector by lazy { icone("percent", "M19 5 5 19", "M4 6.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0", "M15 17.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0") }
    val CodeXml: ImageVector by lazy { icone("code-xml", "m18 16 4-4-4-4", "m6 8-4 4 4 4", "m14.5 4-5 16") }
    val ToggleRight: ImageVector by lazy { icone("toggle-right", "M12 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M9 5h6a7 7 0 0 1 7 7v0a7 7 0 0 1 -7 7h-6a7 7 0 0 1 -7 -7v0a7 7 0 0 1 7 -7Z") }
    val Smartphone: ImageVector by lazy { icone("smartphone", "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2Z", "M12 18h.01") }
    val Palette: ImageVector by lazy { icone("palette", "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z", "M13 6.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0", "M17 10.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0", "M6 12.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0", "M8 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0") }
    val Heart: ImageVector by lazy { icone("heart", "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5") }
    // conquistas
    val Trophy: ImageVector by lazy { icone("trophy", "M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2", "M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2", "M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3", "M4 22h16", "M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z", "M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3") }
    val Medal: ImageVector by lazy { icone("medal", "M7.21 15 2.66 7.14a2 2 0 0 1 .13-2.2L4.4 2.8A2 2 0 0 1 6 2h12a2 2 0 0 1 1.6.8l1.6 2.14a2 2 0 0 1 .14 2.2L16.79 15", "M11 12 5.12 2.2", "m13 12 5.88-9.8", "M8 7h8", "M7 17a5 5 0 1 0 10 0a5 5 0 1 0 -10 0", "M12 18v-2h-.5") }
    val Award: ImageVector by lazy { icone("award", "m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526", "M6 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0") }
    val Rocket: ImageVector by lazy { icone("rocket", "M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5", "M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09", "M9 12a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.4 22.4 0 0 1-4 2z", "M9 12H4s.55-3.03 2-4c1.62-1.08 5 .05 5 .05") }
    val Mountain: ImageVector by lazy { icone("mountain", "m8 3 4 8 5-5 5 15H2L8 3z") }
    val Gauge: ImageVector by lazy { icone("gauge", "m12 14 4-4", "M3.34 19a10 10 0 1 1 17.32 0") }
    val MoonStar: ImageVector by lazy { icone("moon-star", "M18 5h4", "M20 3v4", "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401") }
    val CalendarDays: ImageVector by lazy { icone("calendar-days", "M8 2v3", "M16 2v3", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z", "M3 9h18", "M8 13h.01", "M12 13h.01", "M16 13h.01", "M8 17h.01", "M12 17h.01", "M16 17h.01") }
    val Gem: ImageVector by lazy { icone("gem", "M10.5 3 8 9l4 13 4-13-2.5-6", "M17 3a2 2 0 0 1 1.6.8l3 4a2 2 0 0 1 .013 2.382l-7.99 10.986a2 2 0 0 1-3.247 0l-7.99-10.986A2 2 0 0 1 2.4 7.8l2.998-3.997A2 2 0 0 1 7 3z", "M2 9h20") }
    val Target: ImageVector by lazy { icone("target", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0", "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Rabbit: ImageVector by lazy { icone("rabbit", "M13 16a3 3 0 0 1 2.24 5", "M18 12h.01", "M18 21h-8a4 4 0 0 1-4-4 7 7 0 0 1 7-7h.2L9.6 6.4a1 1 0 1 1 2.8-2.8L15.8 7h.2c3.3 0 6 2.7 6 6v1a2 2 0 0 1-2 2h-1a3 3 0 0 0-3 3", "M20 8.54V4a2 2 0 1 0-4 0v3", "M7.612 12.524a3 3 0 1 0-1.6 4.3") }
    val BadgeCheck: ImageVector by lazy { icone("badge-check", "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z", "m16 9-5.5 5.5L8 12") }
    val Sunrise: ImageVector by lazy { icone("sunrise", "M12 2v8", "m4.93 10.93 1.41 1.41", "M2 18h2", "M20 18h2", "m19.07 10.93-1.41 1.41", "M22 22H2", "m8 6 4-4 4 4", "M16 18a4 4 0 0 0-8 0") }
    val Hourglass: ImageVector by lazy { icone("hourglass", "M5 22h14", "M5 2h14", "M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22", "M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2") }
    val Coins: ImageVector by lazy { icone("coins", "M13.744 17.736a6 6 0 1 1-7.48-7.48", "M15 6h1v4", "m6.134 14.768.866-.5 2 3.464", "M10 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0") }
    val Handshake: ImageVector by lazy { icone("handshake", "m11 17 2 2a1 1 0 1 0 3-3", "m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4", "m21 3 1 11h-2", "M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3", "M3 4h8") }
    val Footprints: ImageVector by lazy { icone("footprints", "M4 16v-2.38C4 11.5 2.97 10.5 3 8c.03-2.72 1.49-6 4.5-6C9.37 2 10 3.8 10 5.5c0 3.11-2 5.66-2 8.68V16a2 2 0 1 1-4 0Z", "M20 20v-2.38c0-2.12 1.03-3.12 1-5.62-.03-2.72-1.49-6-4.5-6C14.63 6 14 7.8 14 9.5c0 3.11 2 5.66 2 8.68V20a2 2 0 1 0 4 0Z", "M16 17h4", "M4 13h4") }
    val Siren: ImageVector by lazy { icone("siren", "M7 18v-6a5 5 0 1 1 10 0v6", "M5 21a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-1a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2z", "M21 12h1", "M18.5 4.5 18 5", "M2 12h1", "M12 2v1", "m4.929 4.929.707.707", "M12 12v6") }
    val Flag: ImageVector by lazy { icone("flag", "M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 7.333 2q2 0 3.067-.8A1 1 0 0 1 20 4v10a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.528") }
    val Milestone: ImageVector by lazy { icone("milestone", "M12 13v8", "M12 3v3", "M18.172 6a2 2 0 0 1 1.414.586l2.06 2.06a1.207 1.207 0 0 1 0 1.708l-2.06 2.06a2 2 0 0 1-1.414.586H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z") }
    val Cake: ImageVector by lazy { icone("cake", "M20 21v-8a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8", "M4 16s.5-1 2-1 2.5 2 4 2 2.5-2 4-2 2.5 2 4 2 2-1 2-1", "M2 21h20", "M7 8v3", "M12 8v3", "M17 8v3", "M7 4h.01", "M12 4h.01", "M17 4h.01") }
    val ShieldAlert: ImageVector by lazy { icone("shield-exclamation-point", "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z", "M12 8v4", "M12 16h.01") }
    val Infinito: ImageVector by lazy { icone("infinity", "M6 16c5 0 7-8 12-8a4 4 0 0 1 0 8c-5 0-7-8-12-8a4 4 0 1 0 0 8") }
    val UserCog: ImageVector by lazy { icone("user-cog", "M10 15H6a4 4 0 0 0-4 4v2", "m14.305 16.53.923-.382", "m15.228 13.852-.923-.383", "m16.852 12.228-.383-.923", "m16.852 17.772-.383.924", "m19.148 12.228.383-.923", "m19.53 18.696-.382-.924", "m20.772 13.852.924-.383", "m20.772 16.148.924.383", "M15 15a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0") }
    val Car: ImageVector by lazy { icone("car", "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2", "M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0", "M9 17h6", "M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Bicycle: ImageVector by lazy { icone("bicicleta", "M2 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0", "M15 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0", "M14 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0", "M12 17.5V14l-3-3 4-3 2 3h2") }
    val Truck: ImageVector by lazy { icone("truck", "M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2", "M15 18H9", "M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14", "M15 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0", "M5 18a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val Type: ImageVector by lazy { icone("type", "M12 4v16", "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2", "M9 20h6") }
    val SquareDashed: ImageVector by lazy { icone("square-dashed", "M5 3a2 2 0 0 0-2 2", "M19 3a2 2 0 0 1 2 2", "M21 19a2 2 0 0 1-2 2", "M5 21a2 2 0 0 1-2-2", "M9 3h1", "M9 21h1", "M14 3h1", "M14 21h1", "M3 9v1", "M21 9v1", "M3 14v1", "M21 14v1") }
    val Globe: ImageVector by lazy { icone("globe", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20") }
    val PartyPopper: ImageVector by lazy { icone("party-popper", "M5.8 11.3 2 22l10.7-3.79", "M4 3h.01", "M22 8h.01", "M15 2h.01", "M22 20h.01", "m22 2-2.24.75a2.9 2.9 0 0 0-1.96 3.12c.1.86-.57 1.63-1.45 1.63h-.38c-.86 0-1.6.6-1.76 1.44L14 10", "m22 13-.82-.33c-.86-.34-1.82.2-1.98 1.11c-.11.7-.72 1.22-1.43 1.22H17", "m11 2 .33.82c.34.86-.2 1.82-1.11 1.98C9.52 4.9 9 5.52 9 6.23V7", "M11 13c1.93 1.93 2.83 4.17 2 5-.83.83-3.07-.07-5-2-1.93-1.93-2.83-4.17-2-5 .83-.83 3.07.07 5 2Z") }
    val Signature: ImageVector by lazy { icone("signature", "m21 17-2.156-1.868A.5.5 0 0 0 18 15.5v.5a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1c0-2.545-3.991-3.97-8.5-4a1 1 0 0 0 0 5c4.153 0 4.745-11.295 5.708-13.5a2.5 2.5 0 1 1 3.31 3.284", "M3 21h18") }
    val Swords: ImageVector by lazy { icone("swords", "m13 19 6-6", "M14.5 17.5 3.586 6.586A2 2 0 013 5.172V3h2.172a2 2 0 011.414.586L17.5 14.5", "m14.828 6.172 2.586-2.586A2 2 0 0118.828 3H21v2.172a2 2 0 01-.586 1.414l-2.586 2.586", "m16 16 4 4", "m19 21 2-2", "m5 14 4 4", "m5 21-2-2", "M7.5 16.5 4 20") }
    val Snail: ImageVector by lazy { icone("snail", "M2 13a6 6 0 1 0 12 0 4 4 0 1 0-8 0 2 2 0 0 0 4 0", "M2 13a8 8 0 1 0 16 0a8 8 0 1 0 -16 0", "M2 21h12c4.4 0 8-3.6 8-8V7a2 2 0 1 0-4 0v6", "M18 3 19.1 5.2", "M22 3 20.9 5.2") }
    val Turtle: ImageVector by lazy { icone("turtle", "m12 10 2 4v3a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-3a8 8 0 1 0-16 0v3a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-3l2-4h4Z", "M4.82 7.9 8 10", "M15.18 7.9 12 10", "M16.93 10H20a2 2 0 0 1 0 4H2") }
    val Bird: ImageVector by lazy { icone("bird", "M16 7h.01", "M3.4 18H12a8 8 0 0 0 8-8V7a4 4 0 0 0-7.28-2.3L2 20", "m20 7 2 .5-2 .5", "M10 18v3", "M14 17.75V21", "M7 18a6 6 0 0 0 3.84-10.61") }
    val MessagesSquare: ImageVector by lazy { icone("messages-square", "M16 10a2 2 0 0 1-2 2H6.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 2 14.286V4a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z", "M20 9a2 2 0 0 1 2 2v10.286a.71.71 0 0 1-1.212.502l-2.202-2.202A2 2 0 0 0 17.172 19H10a2 2 0 0 1-2-2v-1") }
    val MessageCircle: ImageVector by lazy { icone("message-circle", "M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168l3.413-.998a2 2 0 0 1 1.099.092 10 10 0 1 0-4.777-4.719") }
    val Share2: ImageVector by lazy { icone("share-2", "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M15 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", "M8.59 13.51L15.42 17.49", "M15.41 6.51L8.59 10.49") }
    val CornerUpLeft: ImageVector by lazy { icone("corner-up-left", "M20 20v-7a4 4 0 0 0-4-4H4", "M9 14 4 9l5-5") }
    val CornerUpRight: ImageVector by lazy { icone("corner-up-right", "m15 14 5-5-5-5", "M4 20v-7a4 4 0 0 1 4-4h12") }
    val ArrowUpLeft: ImageVector by lazy { icone("arrow-up-left", "M7 17V7h10", "M17 17 7 7") }
    val ArrowUpRight: ImageVector by lazy { icone("arrow-up-right", "M7 7h10v10", "M7 17 17 7") }
    val RotateCcw: ImageVector by lazy { icone("rotate-ccw", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5") }
    val Undo2: ImageVector by lazy { icone("undo-2", "M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11") }
    val ImagePlus: ImageVector by lazy { icone("image-plus", "M16 5h6", "M19 2v6", "M21 11.5V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7.5", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21", "M7 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0") }
    val BellOff: ImageVector by lazy { icone("bell-off", "M10.268 21a2 2 0 0 0 3.464 0", "M17 17H4a1 1 0 0 1-.74-1.673C4.59 13.956 6 12.499 6 8a6 6 0 0 1 .258-1.742", "m2 2 20 20", "M8.668 3.01A6 6 0 0 1 18 8c0 2.687.77 4.653 1.707 6.05") }
    val VolumeX: ImageVector by lazy { icone("volume-x", "M11 4.702a.7.7 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.7.7 0 0 0 11 19.298z", "m16.5 14.5 5-5", "m16.5 9.5 5 5") }
    val Split: ImageVector by lazy { icone("split", "M16 3h5v5", "M8 3H3v5", "M12 22v-8.3a4 4 0 0 0-1.172-2.872L3 3", "m15 9 6-6") }
    val Merge: ImageVector by lazy { icone("merge", "m8 6 4-4 4 4", "M12 2v10.3a4 4 0 0 1-1.172 2.872L4 22", "m20 22-5-5") }
    val CircleDot: ImageVector by lazy { icone("circle-dot", "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0") }
    val Paperclip: ImageVector by lazy { icone("paperclip", "m16 6-8.414 8.586a2 2 0 0 0 2.829 2.829l8.414-8.586a4 4 0 1 0-5.657-5.657l-8.379 8.551a6 6 0 1 0 8.485 8.485l8.379-8.551") }
    val Database: ImageVector by lazy { icone("database", "M3 5a9 3 0 1 0 18 0a9 3 0 1 0 -18 0", "M3 5V19A9 3 0 0 0 21 19V5", "M3 12A9 3 0 0 0 21 12") }
    val ArrowRightLeft: ImageVector by lazy { icone("arrow-right-left", "m16 3 4 4-4 4", "M20 7H4", "m8 21-4-4 4-4", "M4 17h16") }
    val PiggyBank: ImageVector by lazy { icone("piggy-bank", "M11 17h3v2a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-3a3.16 3.16 0 0 0 2-2h1a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1h-1a5 5 0 0 0-2-4V3a4 4 0 0 0-3.2 1.6l-.3.4H11a6 6 0 0 0-6 6v1a5 5 0 0 0 2 4v3a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1z", "M16 10h.01", "M2 8v1a2 2 0 0 0 2 2h1") }
    val HandCoins: ImageVector by lazy { icone("hand-coins", "M11 15h2a2 2 0 1 0 0-4h-3c-.6 0-1.1.2-1.4.6L3 17", "m7 21 1.6-1.4c.3-.4.8-.6 1.4-.6h4c1.1 0 2.1-.4 2.8-1.2l4.6-4.4a2 2 0 0 0-2.75-2.91l-4.2 3.9", "m2 16 6 6", "M13.1 9a2.9 2.9 0 1 0 5.8 0a2.9 2.9 0 1 0 -5.8 0", "M3 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0") }
    val Boxes: ImageVector by lazy { icone("boxes", "M2.97 12.92A2 2 0 0 0 2 14.63v3.24a2 2 0 0 0 .97 1.71l3 1.8a2 2 0 0 0 2.06 0L12 19v-5.5l-5-3-4.03 2.42Z", "m7 16.5-4.74-2.85", "m7 16.5 5-3", "M7 16.5v5.17", "M12 13.5V19l3.97 2.38a2 2 0 0 0 2.06 0l3-1.8a2 2 0 0 0 .97-1.71v-3.24a2 2 0 0 0-.97-1.71L17 10.5l-5 3Z", "m17 16.5-5-3", "m17 16.5 4.74-2.85", "M17 16.5v5.17", "M7.97 4.42A2 2 0 0 0 7 6.13v4.37l5 3 5-3V6.13a2 2 0 0 0-.97-1.71l-3-1.8a2 2 0 0 0-2.06 0l-3 1.8Z", "M12 8 7.26 5.15", "m12 8 4.74-2.85", "M12 13.5V8") }
}

/** Ícone guardado pelo nome (Lucide) → ícone do app (os que existem aqui; o resto vira pacote). */
fun iconePorNome(nome: String?): ImageVector = when (nome) {
    "file-text", "file-box", "newspaper", "mail", "folder" -> PwIcones.FileText
    "smartphone", "tablet", "laptop", "tv", "cpu", "headphones", "watch", "camera" -> PwIcones.Smartphone
    "flask-conical", "droplet", "fuel" -> PwIcones.FlaskConical
    "flame" -> PwIcones.Flame
    "store", "shopping-bag", "shopping-cart" -> PwIcones.Store
    "building-2", "building", "warehouse", "factory", "hotel", "school", "hospital" -> PwIcones.Building2
    "landmark", "banknote", "circle-dollar-sign" -> PwIcones.Landmark
    "receipt" -> PwIcones.Receipt
    "heart", "pill", "syringe", "stethoscope", "briefcase-medical" -> PwIcones.Heart
    "house" -> PwIcones.House
    "map-pin" -> PwIcones.MapPin
    "clock" -> PwIcones.Clock
    "zap" -> PwIcones.Zap
    "star", "sparkles", "crown" -> PwIcones.Star
    "bike", "truck", "car", "bus" -> PwIcones.Bike
    "palette" -> PwIcones.Palette
    "shield", "shield-check" -> PwIcones.ShieldCheck
    "user-round", "users" -> PwIcones.UserRound
    "package-2", "gift", "box" -> PwIcones.Package2
    else -> PwIcones.Package
}
