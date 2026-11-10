import { pathToFileURL } from "node:url";
import { writeFileSync } from "node:fs";
const pasta = process.argv[2], saida = process.argv[3];
const nomes = ["map-pin","map-pinned","navigation","phone","check","circle-check","x","flask-conical","user-round","camera","image","log-out","sun","moon","monitor","bike","wallet","timer","crosshair","battery-medium","battery-low","power","radio","package","package-check","route","chevrons-right","settings-2","bug","shield-check","pen-line","key-round","clock","arrow-up-from-line","arrow-down-to-line","locate-fixed","bell-ring","volume-2","refresh-cw","chevron-left","triangle-exclamation-point","sparkles","pencil","send","eye-off","maximize-2","map","building-2","users","plus","minus","trash-2","lock","lock-open","user-plus","search","store","landmark","repeat","calendar-clock","calendar-check","calendar-x","chevron-right","chevron-down","history","list","zap","download","circle-x","ban","hand","trending-up","star","copy","eye","user-round-pen","receipt","ruler","radar","info","house","arrow-left","mail","menu","circle-arrow-up","shield","layout-dashboard","sliders-horizontal","file-text","arrow-up","arrow-down","chevron-up","list-checks","sun-moon","signal-zero","code-xml","toggle-right","smartphone","palette","heart"];
const n = (v) => Number(v);
function emPath([tag, a]) {
  switch (tag) {
    case "path": return a.d;
    case "circle": { const cx=n(a.cx), cy=n(a.cy), r=n(a.r); return `M${cx-r} ${cy}a${r} ${r} 0 1 0 ${2*r} 0a${r} ${r} 0 1 0 ${-2*r} 0`; }
    case "ellipse": { const cx=n(a.cx), cy=n(a.cy), rx=n(a.rx), ry=n(a.ry); return `M${cx-rx} ${cy}a${rx} ${ry} 0 1 0 ${2*rx} 0a${rx} ${ry} 0 1 0 ${-2*rx} 0`; }
    case "line": return `M${a.x1} ${a.y1}L${a.x2} ${a.y2}`;
    case "polyline": case "polygon": { const p=a.points.trim().split(/[\s,]+/).map(Number); let d=`M${p[0]} ${p[1]}`; for(let i=2;i<p.length;i+=2) d+=`L${p[i]} ${p[i+1]}`; return tag==="polygon"? d+"Z" : d; }
    case "rect": { const x=n(a.x??0), y=n(a.y??0), w=n(a.width), h=n(a.height); let rx=n(a.rx??a.ry??0), ry=n(a.ry??a.rx??0); rx=Math.min(rx,w/2); ry=Math.min(ry,h/2);
      if(!rx) return `M${x} ${y}h${w}v${h}h${-w}Z`;
      return `M${x+rx} ${y}h${w-2*rx}a${rx} ${ry} 0 0 1 ${rx} ${ry}v${h-2*ry}a${rx} ${ry} 0 0 1 ${-rx} ${ry}h${-(w-2*rx)}a${rx} ${ry} 0 0 1 ${-rx} ${-ry}v${-(h-2*ry)}a${rx} ${ry} 0 0 1 ${rx} ${-ry}Z`; }
    default: throw new Error("tag " + tag);
  }
}
const renomear = { "triangle-exclamation-point": "Alerta", map: "Mapa", list: "Lista" }; // evita nomes que confundem com tipos do Kotlin
const pascal0 = (s) => s.split("-").map((p) => p[0].toUpperCase() + p.slice(1)).join("").replace(/^(\d)/, "I$1");
const pascal = (s) => renomear[s] ?? pascal0(s);
let linhas = [];
for (const nome of nomes) {
  let m = await import(pathToFileURL(`${pasta}/${nome}.mjs`).href);
  // alguns nomes são apelidos: o arquivo só reexporta o ícone de verdade
  if (!m.__iconData) {
    const alvo = /from '\.\/([^']+)\.mjs'/.exec((await import("node:fs")).readFileSync(`${pasta}/${nome}.mjs`, "utf8"))?.[1];
    if (alvo) m = await import(pathToFileURL(`${pasta}/${alvo}.mjs`).href);
  }
  const no = m.__iconData?.node ?? m.default?.iconNode ?? m.__iconNode;
  if (!no) throw new Error("ícone sem dados: " + nome);
  const caminhos = no.map(emPath).map((d) => JSON.stringify(d));
  linhas.push(`    val ${pascal(nome)}: ImageVector by lazy { icone("${nome}", ${caminhos.join(", ")}) }`);
}
writeFileSync(saida, `package com.pwexpress.core.designsystem

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

${linhas.join("\n")}
}
`);
console.log("ícones:", nomes.length);
