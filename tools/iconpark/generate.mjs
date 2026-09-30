// Regenerates the vendored IconPark multicolor artwork used by the Mony Color icon pack.
//
// Source of truth (do not hand-edit the generated Kotlin files):
//   npm pack @iconify-json/icon-park@1.2.4
//   tar -xzf iconify-json-icon-park-1.2.4.tgz
//   node tools/iconpark/generate.mjs --source package/icons.json
//
// The generator converts the SVG subset used by the selected icons (g, path, circle, rect)
// into Compose ImageVector builders. Path data is copied verbatim, so no coordinate is ever
// invented or re-scaled; the artwork keeps its original 48x48 viewport and is only scaled by
// Compose when it is drawn at a requested size.
//
// Colours: IconPark derives its four themes (outline, filled, two-tone, multi-color) from a
// single source drawing by remapping exactly four colour slots, so the geometry is generated
// once and every theme is reproduced by swapping the palette at runtime. The slots are the ones
// used by IconPark's own runtime (packages/svg/src/runtime):
//
//   #000   -> outerStroke   IconPark colors[0], outStrokeColor
//   #2F88FF -> outerFill    IconPark colors[1], outFillColor
//   #fff   -> innerStroke   IconPark colors[2], innerStrokeColor
//   #43CCF8 -> innerFill    IconPark colors[3], innerFillColor
//
// A slot may be painted as a fill or as a stroke depending on the source path (for example
// `upload` fills with innerStroke), which is why the generated code resolves each slot by name
// instead of assuming a fixed role. The two fill-capable slots are nullable because the outline
// theme paints no fill at all.

import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const OUTPUT_DIR = resolve(HERE, '../../app/src/main/java/com/angel/mony/ui/iconography/vendor/iconpark')
const PACKAGE = 'com.angel.mony.ui.iconography.vendor.iconpark'
const SOURCE_PACKAGE = '@iconify-json/icon-park@1.2.4'

/** Semantic subset vendored into the app. Keep alphabetical; one entry per IconPark glyph. */
const ICONS = [
  'add-one',
  'adjustment',
  'alarm-clock',
  'bank',
  'bank-card',
  'bank-transfer',
  'bell-ring',
  'calendar',
  'car',
  'chart-pie',
  'config',
  'copy',
  'dark-mode',
  'doc-detail',
  'download',
  'edit',
  'filter',
  'health',
  'history',
  'home',
  'left',
  'lock',
  'minus',
  'more-two',
  'movie',
  'noodles',
  'notes',
  'order',
  'peoples',
  'phone',
  'pin',
  'red-cross',
  'refresh-one',
  'right',
  'save',
  'school',
  'search',
  'share-one',
  'shopping-cart',
  'strongbox',
  'tag',
  'time',
  'tool',
  'transaction',
  'undo',
  'unlock',
  'upload',
  'view-list',
  'wallet',
  'wifi',
]

const INHERITED = [
  'fill',
  'stroke',
  'stroke-width',
  'stroke-linecap',
  'stroke-linejoin',
  'stroke-miterlimit',
  'fill-opacity',
  'stroke-opacity',
]

/** Maps each IconPark colour slot to the `IconParkPalette` property that drives it. */
const PALETTE_SLOTS = new Map([
  ['#000', 'outerStroke'],
  ['#000000', 'outerStroke'],
  ['#2f88ff', 'outerFill'],
  ['#43ccf8', 'innerFill'],
  ['#ffffff', 'innerStroke'],
  ['#fff', 'innerStroke'],
])

const TAG = /<(\/)?([a-zA-Z][\w:-]*)((?:\s+[\w:.-]+\s*=\s*"[^"]*")*)\s*(\/)?>/g
const ATTR = /([\w:.-]+)\s*=\s*"([^"]*)"/g

function parseAttributes(raw) {
  const attrs = {}
  for (const match of raw.matchAll(ATTR)) attrs[match[1]] = match[2]
  return attrs
}

function parseTree(body) {
  const root = { tag: '#root', attrs: {}, children: [] }
  const stack = [root]
  for (const match of body.matchAll(TAG)) {
    const [, closing, tag, raw, selfClosing] = match
    const parent = stack[stack.length - 1]
    if (closing) {
      if (stack.length > 1) stack.pop()
      continue
    }
    const node = { tag, attrs: parseAttributes(raw), children: [] }
    parent.children.push(node)
    if (!selfClosing) stack.push(node)
  }
  return root.children
}

function findMasks(nodes) {
  const masks = new Map()
  for (const node of nodes) {
    if (node.tag !== 'mask') continue
    const opaque = node.children.some(
      (child) =>
        (child.tag === 'path' || child.tag === 'rect') &&
        String(child.attrs.fill ?? '#fff').toLowerCase() === '#fff',
    )
    const full =
      (node.attrs.width === '48' || node.attrs.width === undefined) &&
      (node.attrs.height === '48' || node.attrs.height === undefined)
    if (opaque && full && node.children.length === 1) masks.set(node.attrs.id, { noop: true })
    else masks.set(node.attrs.id, { noop: false })
  }
  return masks
}

function num(value, fallback = 0) {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function color(raw, context) {
  if (raw === undefined || raw === 'none') return null
  const value = raw.trim().toLowerCase()
  const slot = PALETTE_SLOTS.get(value)
  if (slot) return { slot }
  if (value === 'transparent') return null
  throw new Error(`${context}: colour no soportado "${raw}"`)
}

function float(value) {
  return `${num(value).toFixed(4).replace(/\.?0+$/, '')}f`
}

function cap(value) {
  switch (value) {
    case 'round':
      return 'StrokeCap.Round'
    case 'square':
      return 'StrokeCap.Square'
    case 'butt':
    case undefined:
      return 'StrokeCap.Butt'
    default:
      throw new Error(`stroke-linecap no soportado "${value}"`)
  }
}

function lineJoin(value) {
  switch (value) {
    case 'round':
      return 'StrokeJoin.Round'
    case 'bevel':
      return 'StrokeJoin.Bevel'
    case 'miter':
    case undefined:
      return 'StrokeJoin.Miter'
    default:
      throw new Error(`stroke-linejoin no soportado "${value}"`)
  }
}

function roundedRectPath(x, y, width, height, rx, ry) {
  const limit = Math.min(width, height) / 2
  rx = Math.min(rx, limit)
  ry = Math.min(ry, limit)
  const n = (v) => Number(v.toFixed(4))
  if (rx <= 0 || ry <= 0) {
    return `M${n(x)},${n(y)}H${n(x + width)}V${n(y + height)}H${n(x)}Z`
  }
  return [
    `M${n(x + rx)},${n(y)}`,
    `H${n(x + width - rx)}`,
    `A${n(rx)},${n(ry)} 0 0 1 ${n(x + width)},${n(y + ry)}`,
    `V${n(y + height - ry)}`,
    `A${n(rx)},${n(ry)} 0 0 1 ${n(x + width - rx)},${n(y + height)}`,
    `H${n(x + rx)}`,
    `A${n(rx)},${n(ry)} 0 0 1 ${n(x)},${n(y + height - ry)}`,
    `V${n(y + ry)}`,
    `A${n(rx)},${n(ry)} 0 0 1 ${n(x + rx)},${n(y)}`,
    'Z',
  ].join('')
}

function circlePath(cx, cy, r) {
  const n = (v) => Number(v.toFixed(4))
  return [
    `M${n(cx - r)},${n(cy)}`,
    `A${n(r)},${n(r)} 0 1 0 ${n(cx + r)},${n(cy)}`,
    `A${n(r)},${n(r)} 0 1 0 ${n(cx - r)},${n(cy)}`,
    'Z',
  ].join('')
}

function shapePath(node, context) {
  if (node.tag === 'path') {
    if (!node.attrs.d) throw new Error(`${context}: path sin atributo d`)
    return node.attrs.d
  }
  if (node.tag === 'circle') {
    const r = num(node.attrs.r)
    if (r <= 0) throw new Error(`${context}: circle con radio inválido`)
    return circlePath(num(node.attrs.cx), num(node.attrs.cy), r)
  }
  if (node.tag === 'rect') {
    const rx = num(node.attrs.rx, 0)
    const ry = node.attrs.ry !== undefined ? num(node.attrs.ry) : rx
    return roundedRectPath(num(node.attrs.x), num(node.attrs.y), num(node.attrs.width), num(node.attrs.height), rx, ry)
  }
  throw new Error(`${context}: elemento <${node.tag}> no soportado`)
}

function parseTransform(raw, context) {
  if (!raw) return null
  const values = raw.match(/[a-zA-Z]+\s*\([^)]*\)/g)
  if (!values || values.length !== 1) throw new Error(`${context}: transform complejo "${raw}"`)
  const call = values[0]
  const name = call.slice(0, call.indexOf('('))
  const args = call
    .slice(call.indexOf('(') + 1, call.lastIndexOf(')'))
    .split(/[\s,]+/)
    .filter(Boolean)
    .map(Number)
  if (name === 'rotate') {
    return { rotate: args[0], pivotX: args[1] ?? 0, pivotY: args[2] ?? 0, hasPivot: args.length === 3 }
  }
  if (name === 'translate') return { translationX: args[0] ?? 0, translationY: args[1] ?? 0 }
  if (name === 'scale') return { scaleX: args[0] ?? 1, scaleY: args[1] ?? args[0] ?? 1 }
  throw new Error(`${context}: transform no soportado "${raw}"`)
}

function emitPath(node, inherited, context) {
  const style = { ...inherited, ...node.attrs }
  const fill = color(style.fill, context)
  const stroke = color(style.stroke, context)
  if (!fill && !stroke) return null

  const fillAlpha = style['fill-opacity'] !== undefined ? num(style['fill-opacity'], 1) : 1
  const strokeAlpha = style['stroke-opacity'] !== undefined ? num(style['stroke-opacity'], 1) : 1
  const fillType = (style['fill-rule'] ?? 'nonzero') === 'evenodd' ? 'PathFillType.EvenOdd' : 'PathFillType.NonZero'

  const lines = [
    'addPath(',
    `    pathData = PathParser().parsePathString("${node.pathData}").toNodes(),`,
    `    fill = ${fill ? `palette.${fill.slot}.brush()` : 'null'},`,
    `    fillAlpha = ${float(fillAlpha)},`,
    `    stroke = ${stroke ? `SolidColor(palette.${stroke.slot})` : 'null'},`,
    `    strokeAlpha = ${float(strokeAlpha)},`,
    `    strokeLineWidth = ${float(num(style['stroke-width'], 1))},`,
    `    strokeLineCap = ${cap(style['stroke-linecap'])},`,
    `    strokeLineJoin = ${lineJoin(style['stroke-linejoin'])},`,
    `    strokeLineMiter = ${float(num(style['stroke-miterlimit'], 4))},`,
    `    pathFillType = ${fillType},`,
    ')',
  ].join('\n')

  const transform = parseTransform(node.attrs.transform, context)
  if (!transform) return lines
  return [
    'addGroup(',
    ...(transform.rotate !== undefined ? [`    rotate = ${float(transform.rotate)},`] : []),
    ...(transform.hasPivot
      ? [`    pivotX = ${float(transform.pivotX)},`, `    pivotY = ${float(transform.pivotY)},`]
      : []),
    ...(transform.translationX !== undefined ? [`    translationX = ${float(transform.translationX)},`] : []),
    ...(transform.translationY !== undefined ? [`    translationY = ${float(transform.translationY)},`] : []),
    ...(transform.scaleX !== undefined ? [`    scaleX = ${float(transform.scaleX)},`] : []),
    ...(transform.scaleY !== undefined ? [`    scaleY = ${float(transform.scaleY)},`] : []),
    ')',
    lines,
    'clearGroup()',
  ].join('\n')
}

function emitIcon(name, definition) {
  const context = `icon-park/${name}`
  const masks = findMasks(definition.body.filter((n) => n.tag === 'mask'))
  const roots = definition.body.filter((node) => node.tag !== 'mask')
  const inherited = {}
  const blocks = []

  const visit = (nodes, style, transform) => {
    for (const node of nodes) {
      if (node.tag === 'mask' || node.tag === 'use') continue
      if (node.tag === 'g') {
        const childStyle = { ...style }
        for (const key of INHERITED) {
          if (node.attrs[key] !== undefined) childStyle[key] = node.attrs[key]
        }
        visit(node.children, childStyle, transform)
        continue
      }
      if (node.attrs.mask) {
        const id = node.attrs.mask.replace(/^url\(#/, '').replace(/\)$/, '')
        const mask = masks.get(id)
        if (!mask) throw new Error(`${context}: máscara desconocida "${id}"`)
        if (!mask.noop) throw new Error(`${context}: máscara con recorte real, no representable en ImageVector`)
      }
      const nodeTransform = transform ?? parseTransform(node.attrs.transform, context)
      node.pathData = shapePath(node, context)
      const block = emitPath(node, style, context)
      if (block) blocks.push(block)
    }
  }

  visit(roots, { ...inherited }, null)

  return blocks
}

function lowerCamel(name) {
  return name.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase())
}

function pascal(name) {
  const camel = lowerCamel(name)
  return camel.charAt(0).toUpperCase() + camel.slice(1)
}

function kotlinString(value) {
  return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\s+/g, ' ').trim()
}

const HEADER = `package ${PACKAGE}

// GENERATED FILE - do not edit by hand.
// Source: ${SOURCE_PACKAGE} (IconPark by ByteDance, Apache-2.0).
// Regenerate with: node tools/iconpark/generate.mjs --source <extracted>/package/icons.json
`

function render(name, definition) {
  const blocks = emitIcon(name, definition)
  const functionName = lowerCamel(name)
  return `${HEADER}
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * IconPark \`${name}\` (48x48) as a Compose [ImageVector].
 *
 * The geometry is shared by every IconPark theme; [palette] selects the theme by supplying the
 * four colour slots IconPark remaps at runtime. A fresh vector is built on every call, so callers
 * should cache it for the composition (for example with \`remember\`).
 */
public fun IconPark.${functionName}(palette: IconParkPalette): ImageVector = ImageVector.Builder(
    name = "IconPark.${name}",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
${blocks.map((block) => block.split('\n').map((line) => `    ${line}`).join('\n')).join('\n')}
}.build()
`
}

const CONTROLLER = `${HEADER}
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * The four colour slots IconPark remaps to render its themes.
 *
 * IconPark (IconPark by ByteDance, Apache-2.0) authors each glyph once and derives every theme by
 * swapping these slots, so the vendored vectors stay theme-parameterised instead of duplicated per
 * style. [outerFill] and [innerFill] are nullable because the outline theme paints no fill.
 *
 * @see MonyColorStyle for the theme presets the UI offers.
 */
public class IconParkPalette(
    public val outerStroke: Color,
    public val outerFill: Color?,
    public val innerStroke: Color,
    public val innerFill: Color?,
)

/** Namespace for the selected IconPark vectors bundled by Mony for the Mony Color pack. */
public object IconPark

/** Resolves a nullable palette slot to a brush, or \`null\` when the theme paints no fill. */
internal fun Color?.brush(): Brush? = this?.let { SolidColor(it) }
`

function main() {
  const args = process.argv.slice(2)
  const sourceIndex = args.indexOf('--source')
  if (sourceIndex === -1 || !args[sourceIndex + 1]) {
    console.error('uso: node tools/iconpark/generate.mjs --source <ruta>/package/icons.json')
    process.exit(2)
  }
  const source = resolve(args[sourceIndex + 1])
  const data = JSON.parse(readFileSync(source, 'utf8'))

  const missing = ICONS.filter((name) => !data.icons[name])
  if (missing.length) {
    console.error(`Iconos ausentes en ${SOURCE_PACKAGE}: ${missing.join(', ')}`)
    process.exit(1)
  }

  rmSync(OUTPUT_DIR, { recursive: true, force: true })
  mkdirSync(OUTPUT_DIR, { recursive: true })

  const functions = []
  for (const name of ICONS) {
    const definition = { ...data.icons[name], left: 0, top: 0 }
    if ((definition.width ?? data.width) !== 48 || (definition.height ?? data.height) !== 48) {
      throw new Error(`icon-park/${name}: viewport inesperado`)
    }
    writeFileSync(join(OUTPUT_DIR, `${pascal(name)}.kt`), render(name, { body: parseTree(definition.body) }), 'utf8')
    functions.push(lowerCamel(name))
  }
  writeFileSync(join(OUTPUT_DIR, 'IconPark.kt'), CONTROLLER, 'utf8')

  console.log(`Generados ${ICONS.length} iconos en ${OUTPUT_DIR}`)
  console.log(functions.join(', '))
}

main()
