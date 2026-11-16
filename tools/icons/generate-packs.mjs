// Generate the offline Compose subsets used by the icon packs.
// See docs/ICONOGRAPHY.md for source versions, licenses, and invocation details.

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const BASE = resolve(HERE, '../../app/src/main/java/com/angel/mony/ui/iconography/vendor')
const PACKAGE = 'com.angel.mony.ui.iconography.vendor'

const MINGCUTE = [
  'wallet-4', 'wallet', 'calendar', 'calendar-2', 'history', 'settings-3', 'palette',
  'search', 'add', 'edit', 'delete', 'more-1', 'check', 'close', 'warning', 'information',
  'tag', 'check-circle', 'hand-coins', 'fork-knife', 'bank-card', 'game-2', 'group',
  'wifi', 'refresh-3', 'car-2', 'home-3', 'lock', 'unlock', 'package-2', 'save', 'filter-2',
  'download-2', 'upload-2', 'share-3', 'copy', 'pin', 'arrow-left', 'arrow-right', 'time',
  'notification', 'notification-off', 'scan', 'mic', 'arrow-up', 'arrow-down', 'alarm-2', 'chart-bar',
  'pig-money', 'shopping-bag-3',
]

const FLAT = [
  'alarm-clock', 'bookmark', 'calendar', 'clock', 'download', 'edit-image', 'home', 'lock', 'money-transfer', 'phone',
  'pie-chart', 'plus', 'reading-ebook', 'safe', 'search', 'services', 'settings', 'shop',
  'share', 'unlock', 'upload',
]

const FLUENT_FLAT = [
  'alarm-clock', 'artist-palette', 'automobile', 'bank', 'bar-chart', 'bell', 'bell-with-slash',
  'calendar', 'chart-increasing', 'chart-decreasing', 'coin', 'control-knobs', 'credit-card',
  'dollar-banknote', 'floppy-disk', 'fork-and-knife', 'game-die', 'gear',
  'globe-with-meridians', 'hospital', 'house', 'locked', 'magnifying-glass-tilted-left',
  'money-bag', 'pencil', 'repeat-button', 'school', 'shopping-cart', 'telephone',
  'toolbox', 'unlocked', 'warning', 'wastebasket',
]

const TAG = /<(\/)?([a-zA-Z][\w:-]*)((?:\s+[\w:.-]+\s*=\s*"[^"]*")*)\s*(\/)?\s*>/g
const ATTR = /([\w:.-]+)\s*=\s*"([^"]*)"/g

function attrs(raw) {
  return Object.fromEntries([...raw.matchAll(ATTR)].map(([, key, value]) => [key, value]))
}

function tree(svg) {
  const root = { tag: '#root', attrs: {}, children: [] }
  const stack = [root]
  for (const [, close, tag, raw, self] of svg.matchAll(TAG)) {
    if (tag === 'svg' || tag === '?xml') continue
    if (close) { if (stack.length > 1) stack.pop(); continue }
    const node = { tag, attrs: attrs(raw), children: [] }
    stack.at(-1).children.push(node)
    if (!self) stack.push(node)
  }
  return root.children
}

function num(value, fallback = 0) {
  if (value === undefined) return fallback
  const parsed = Number.parseFloat(value)
  if (!Number.isFinite(parsed)) throw new Error(`Coordenada SVG inválida: ${value}`)
  return parsed
}

function roundRect(x, y, width, height, rx = 0, ry = rx) {
  if (!rx && !ry) return `M${x},${y}h${width}v${height}h-${width}Z`
  const rX = Math.min(rx, width / 2), rY = Math.min(ry, height / 2)
  return `M${x + rX},${y}h${width - 2 * rX}a${rX},${rY} 0 0 1 ${rX},${rY}v${height - 2 * rY}a${rX},${rY} 0 0 1 -${rX},${rY}h-${width - 2 * rX}a${rX},${rY} 0 0 1 -${rX},-${rY}v-${height - 2 * rY}a${rX},${rY} 0 0 1 ${rX},-${rY}Z`
}

function circle(cx, cy, r) {
  return `M${cx - r},${cy}a${r},${r} 0 1 0 ${2 * r},0a${r},${r} 0 1 0 -${2 * r},0Z`
}

function ellipse(cx, cy, rx, ry) {
  return `M${cx - rx},${cy}a${rx},${ry} 0 1 0 ${2 * rx},0a${rx},${ry} 0 1 0 -${2 * rx},0Z`
}

function colorCode(value, currentColor = false) {
  if (!value || value === 'none') return null
  if (value === 'currentColor') return currentColor ? 'Color.Black' : null
  const hex = value.toUpperCase()
  if (/^#[0-9A-F]{6}$/.test(hex)) return `Color(0xFF${hex.slice(1)})`
  if (/^#[0-9A-F]{3}$/.test(hex)) return `Color(0xFF${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]})`
  throw new Error(`Color SVG no soportado: ${value}`)
}

function collect(nodes, inherited = {}, currentColor = false, out = []) {
  for (const node of nodes) {
    const style = { ...inherited, ...node.attrs }
    if (node.tag === 'g' || node.tag === '#root') {
      collect(node.children, style, currentColor, out)
      continue
    }
    if (!['path', 'circle', 'ellipse', 'rect'].includes(node.tag)) {
      throw new Error(`SVG contiene elemento no soportado <${node.tag}>`)
    }
    let d = node.attrs.d
    if (node.tag === 'circle') d = circle(num(node.attrs.cx), num(node.attrs.cy), num(node.attrs.r))
    if (node.tag === 'ellipse') d = ellipse(num(node.attrs.cx), num(node.attrs.cy), num(node.attrs.rx), num(node.attrs.ry))
    if (node.tag === 'rect') d = roundRect(num(node.attrs.x), num(node.attrs.y), num(node.attrs.width), num(node.attrs.height), num(node.attrs.rx), num(node.attrs.ry, num(node.attrs.rx)))
    if (!d) throw new Error('SVG path sin geometría')
    out.push({
      d,
      fill: colorCode(style.fill ?? (currentColor ? 'currentColor' : '#000000'), currentColor),
      stroke: colorCode(style.stroke, currentColor),
      strokeWidth: num(style['stroke-width'], 1),
      lineCap: style['stroke-linecap'],
      lineJoin: style['stroke-linejoin'],
      fillRule: style['fill-rule'],
    })
  }
  return out
}

function functionName(name) {
  return name.split(/[-_]/).map((x) => x[0].toUpperCase() + x.slice(1)).join('')
}

function kotlinPath(path, mode) {
  const fill = mode === 'colored' ? path.fill : path.fill ? 'Color.Black' : null
  const stroke = mode === 'colored' ? path.stroke : path.stroke ? 'Color.Black' : null
  return `    addPath(\n      pathData = PathParser().parsePathString("${path.d.replaceAll('\\', '\\\\').replaceAll('"', '\\"')}").toNodes(),\n      fill = ${fill ? `SolidColor(${fill})` : 'null'},\n      stroke = ${stroke ? `SolidColor(${stroke})` : 'null'},\n      strokeLineWidth = ${path.strokeWidth}f,\n      strokeLineCap = ${path.lineCap === 'round' ? 'StrokeCap.Round' : path.lineCap === 'square' ? 'StrokeCap.Square' : 'StrokeCap.Butt'},\n      strokeLineJoin = ${path.lineJoin === 'round' ? 'StrokeJoin.Round' : path.lineJoin === 'bevel' ? 'StrokeJoin.Bevel' : 'StrokeJoin.Miter'},\n      pathFillType = ${path.fillRule === 'evenodd' ? 'PathFillType.EvenOdd' : 'PathFillType.NonZero'},\n    )`
}

function renderVector(name, paths, viewport, mode, pack, prefixOverride = null) {
  const prefix = prefixOverride ?? (pack === 'MingCute' ? 'mingCute' : 'flatColor')
  const vectorName = `${prefix}${functionName(name)}`
  return `public fun ${vectorName}(): ImageVector = ImageVector.Builder(\n  name = "${pack}.${functionName(name)}", defaultWidth = 24.dp, defaultHeight = 24.dp,\n  viewportWidth = ${viewport[0]}f, viewportHeight = ${viewport[1]}f,\n).apply {\n${paths.map((p) => kotlinPath(p, mode)).join('\n')}\n}.build()\n`
}

const HEADER = `package ${PACKAGE}.%s\n\n// GENERATED FILE - do not edit by hand.\n`
const COMMON_IMPORTS = `import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.PathFillType\nimport androidx.compose.ui.graphics.SolidColor\nimport androidx.compose.ui.graphics.StrokeCap\nimport androidx.compose.ui.graphics.StrokeJoin\nimport androidx.compose.ui.graphics.vector.ImageVector\nimport androidx.compose.ui.graphics.vector.PathParser\nimport androidx.compose.ui.unit.dp\n`

function readArgs() {
  const args = process.argv.slice(2)
  const value = (key) => { const i = args.indexOf(key); return i < 0 ? null : args[i + 1] }
  if (!value('--mingcute') || !value('--flat') || !value('--fluent-flat')) {
    throw new Error('Faltan fuentes: --mingcute <package> --flat <icons.json> --fluent-flat <icons.json>')
  }
  return {
    mingcute: resolve(value('--mingcute')),
    flat: resolve(value('--flat')),
    fluentFlat: resolve(value('--fluent-flat')),
  }
}

function main() {
  const { mingcute, flat, fluentFlat } = readArgs()
  const flatSet = JSON.parse(readFileSync(flat, 'utf8'))
  const fluentSet = JSON.parse(readFileSync(fluentFlat, 'utf8'))
  const mingDir = join(mingcute, 'core-regular')
  const mingOut = join(BASE, 'mingcute')
  const flatOut = join(BASE, 'flatcolor')
  const fluentOut = join(BASE, 'fluentemojiflat')
  mkdirSync(mingOut, { recursive: true }); mkdirSync(flatOut, { recursive: true })
  mkdirSync(fluentOut, { recursive: true })

  const mingFunctions = []
  for (const name of MINGCUTE) {
    const svg = readFileSync(join(mingDir, `${name}.svg`), 'utf8')
    const viewBox = /viewBox="([^"]+)"/.exec(svg)?.[1]?.split(/[ ,]+/).map(Number)
    if (!viewBox || viewBox.length !== 4) throw new Error(`ViewBox inválido: MingCute ${name}`)
    const paths = collect(tree(svg), {}, true)
    if (!paths.length) throw new Error(`Sin paths: MingCute ${name}`)
    writeFileSync(join(mingOut, `${functionName(name)}.kt`),
      `${HEADER.replace('%s', 'mingcute')}${COMMON_IMPORTS}\n${renderVector(name, paths, viewBox.slice(2), 'tint', 'MingCute')}`)
    mingFunctions.push(`    ${functionName(name)},`)
  }

  const flatFunctions = []
  for (const name of FLAT) {
    const definition = flatSet.icons[name]
    if (!definition) throw new Error(`Icono ausente: Flat Color ${name}`)
    const width = definition.width ?? flatSet.width ?? 48
    const height = definition.height ?? flatSet.height ?? 48
    const paths = collect(tree(definition.body))
    if (!paths.length) throw new Error(`Sin paths: Flat Color ${name}`)
    writeFileSync(join(flatOut, `${functionName(name)}.kt`),
      `${HEADER.replace('%s', 'flatcolor')}${COMMON_IMPORTS}\n${renderVector(name, paths, [width, height], 'colored', 'FlatColor')}`)
    flatFunctions.push(`    ${functionName(name)},`)
  }
  const generateFixedColorSet = (names, set, directory, packName, functionPrefix) => {
    const missing = names.filter((name) => !set.icons[name])
    if (missing.length) throw new Error(`${packName}: faltan iconos ${missing.join(', ')}`)
    for (const name of names) {
      const definition = set.icons[name]
      const width = definition.width ?? set.width ?? 32
      const height = definition.height ?? set.height ?? 32
      const paths = collect(tree(definition.body))
      if (!paths.length) throw new Error(`${packName}: icono vacío ${name}`)
      const body = renderVector(name, paths, [width, height], 'colored', packName, functionPrefix)
      writeFileSync(join(directory, `${functionName(name)}.kt`),
        `${HEADER.replace('%s', packName.toLowerCase().replaceAll(' ', ''))}${COMMON_IMPORTS}\n${body}`)
    }
  }
  generateFixedColorSet(FLUENT_FLAT, fluentSet, fluentOut, 'FluentEmojiFlat', 'fluentFlat')
  console.log(`Generados ${mingFunctions.length} MingCute, ${flatFunctions.length} Flat Color y ${FLUENT_FLAT.length} Fluent Emoji Flat.`)
}

main()

