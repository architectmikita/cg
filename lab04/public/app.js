import { clipSegment, clipPolygon, convexPolygon } from './geometry.js';

const $ = id => document.getElementById(id);
const mode = $('mode'), canvas = $('canvas'), ctx = canvas.getContext('2d');
const tolerance = 0.0001;
const segmentExample = '5\n-2 1 6 1\n1 1 3 2\n-2 -2 -1 4\n2 -2 2 5\n-1 -1 0 0\n0 0 4 3';
const subjectExample = '-2 1\n2 -1\n6 1\n2 3';
const windowExample = '0 0\n4 0\n4 4\n0 4';
let scene;

function numbers(text) {
  if (!text.trim()) throw new Error('Введите исходные данные.');
  return text.trim().split(/\s+/).map(token => {
    if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/i.test(token)) {
      throw new Error('Неверное число: ' + token);
    }
    const value = Number(token);
    if (!Number.isFinite(value) || Math.abs(value) > 1e6) {
      throw new Error('Допустимый диапазон чисел: от −1 000 000 до 1 000 000.');
    }
    return value;
  });
}

function polygon(id, label) {
  const data = $(id).value.trim().split(/\r?\n/).filter(line => line.trim()).map(numbers);
  if (data.some(row => row.length !== 2) || data.length > 200) {
    throw new Error(label + ' — до 200 вершин, по два числа на строку.');
  }
  return convexPolygon(data, label);
}

function format(value) {
  return Number(value.toFixed(5)).toString();
}

function pointText(point) {
  return '(' + point.map(format).join(', ') + ')';
}

function build() {
  const status = $('status');
  try {
    if (mode.value === 'segments') {
      const [n, ...data] = numbers($('segment-data').value);
      if (!Number.isInteger(n) || n < 1 || n > 2000 || data.length !== 4 * n + 4) {
        throw new Error('Укажите число отрезков (1–2000), по четыре координаты на отрезок и четыре границы окна.');
      }
      const bounds = data.slice(-4);
      const [xmin, ymin, xmax, ymax] = bounds;
      if (xmin >= xmax || ymin >= ymax) throw new Error('Минимальные координаты окна должны быть меньше максимальных.');
      const source = [], result = [], rows = [];
      for (let i = 0; i < n; i++) {
        const a = data.slice(4 * i, 4 * i + 2), b = data.slice(4 * i + 2, 4 * i + 4);
        source.push([a, b]);
        const clipped = clipSegment(a, b, bounds, tolerance);
        if (clipped) result.push(clipped);
        rows.push((i + 1) + '. ' + (clipped ? clipped.map(pointText).join(' → ') : 'вне окна или точечное касание не найдено'));
      }
      scene = { window: [[xmin, ymin], [xmax, ymin], [xmax, ymax], [xmin, ymax]], source, result, closed: false };
      status.textContent = 'Видимых отрезков: ' + result.length + ' из ' + n + '. Точность деления: ' + tolerance + '.';
      $('coordinates').textContent = rows.join('\n');
    } else {
      const subject = polygon('subject', 'Исходная фигура');
      const window = polygon('window', 'Отсекающее окно');
      const result = clipPolygon(subject, window);
      scene = { window, source: [subject], result: [result], closed: true };
      status.textContent = result.length > 2 ? 'Вершин результата: ' + result.length :
        ['Пересечения нет.', 'Пересечение — точка.', 'Пересечение — отрезок.'][result.length];
      $('coordinates').textContent = result.map(pointText).join('\n');
    }
    status.classList.remove('error');
  } catch (error) {
    scene = null;
    status.classList.add('error');
    status.textContent = error.message;
    $('coordinates').textContent = '';
  }
  draw();
}

function draw() {
  const w = canvas.clientWidth, h = canvas.clientHeight;
  if (!w || !h) return;
  const dpr = devicePixelRatio;
  canvas.width = Math.round(w * dpr);
  canvas.height = Math.round(h * dpr);
  ctx.setTransform(canvas.width / w, 0, 0, canvas.height / h, 0, 0);
  if (!scene) return;

  let xmin = 0, ymin = 0, xmax = 0, ymax = 0;
  for (const [x, y] of [...scene.window, ...scene.source.flat()]) {
    xmin = Math.min(xmin, x);
    ymin = Math.min(ymin, y);
    xmax = Math.max(xmax, x);
    ymax = Math.max(ymax, y);
  }
  const zoom = $('zoom').valueAsNumber / 100;
  const size = Math.min((w - 54) / Math.max(1e-6, xmax - xmin), (h - 54) / Math.max(1e-6, ymax - ymin)) * zoom;
  const ox = w / 2 - (xmin + xmax) * size / 2;
  const oy = h / 2 + (ymin + ymax) * size / 2;
  const px = x => ox + x * size, py = y => oy - y * size;
  function segment(x, y, ex, ey) {
    ctx.moveTo(x, y);
    ctx.lineTo(ex, ey);
  }
  const unit = 10 ** Math.floor(Math.log10(53 / size));
  const step = [1, 2, 5, 10].map(n => n * unit).find(n => n * size >= 52) ?? 10 * unit;
  ctx.font = '12px system-ui';
  ctx.strokeStyle = '#e1e6ee';
  ctx.fillStyle = '#43516a';
  ctx.beginPath();
  const labelY = Math.min(h - 5, Math.max(13, oy + 14));
  const labelX = Math.min(w - 52, Math.max(4, ox + 5));
  for (let i = Math.ceil(-ox / size / step); i <= Math.floor((w - ox) / size / step); i++) {
    const x = px(i * step);
    segment(x, 0, x, h);
    ctx.fillText(Number((i * step).toPrecision(5)), x + 3, labelY);
  }
  for (let i = Math.ceil((oy - h) / size / step); i <= Math.floor(oy / size / step); i++) {
    const y = py(i * step);
    segment(0, y, w, y);
    if (i) ctx.fillText(Number((i * step).toPrecision(5)), labelX, y - 3);
  }
  ctx.stroke();
  ctx.strokeStyle = '#536078';
  ctx.beginPath();
  segment(ox, 0, ox, h);
  segment(0, oy, w, oy);
  ctx.stroke();
  ctx.fillText('X', w - 15, Math.min(h - 6, Math.max(12, oy - 5)));
  ctx.fillText('Y', labelX, 14);

  function shape(points, color, width, closed, fill) {
    if (!points.length) return;
    ctx.beginPath();
    const [x, y] = points[0];
    if (points.length === 1 || (points.length === 2 && x === points[1][0] && y === points[1][1])) {
      ctx.arc(px(x), py(y), 3, 0, 2 * Math.PI);
      ctx.fillStyle = color;
      ctx.fill();
      return;
    }
    ctx.moveTo(px(x), py(y));
    for (const [x, y] of points.slice(1)) ctx.lineTo(px(x), py(y));
    if (closed && points.length > 2) ctx.closePath();
    if (fill && points.length > 2) {
      ctx.fillStyle = fill;
      ctx.fill();
    }
    ctx.lineWidth = width;
    ctx.strokeStyle = color;
    ctx.stroke();
  }
  shape(scene.window, '#2c5d9a', 2, true);
  for (const item of scene.source) shape(item, '#9a4e14', 1.5, scene.closed);
  for (const item of scene.result) shape(item, '#087249', 3, scene.closed, scene.closed ? '#08724922' : null);
}

function setExample() {
  if (mode.value === 'segments') {
    $('segment-data').value = segmentExample;
    $('file').value = '';
  } else {
    $('subject').value = subjectExample;
    $('window').value = windowExample;
  }
  build();
}

$('segment-data').value = segmentExample;
$('subject').value = subjectExample;
$('window').value = windowExample;
$('form').onsubmit = event => { event.preventDefault(); build(); };
$('example').onclick = setExample;
mode.onchange = () => {
  $('segments').hidden = mode.value !== 'segments';
  $('polygon').hidden = mode.value !== 'polygon';
  build();
};
$('zoom').oninput = event => {
  $('zoom-value').value = event.target.value + '%';
  draw();
};
$('file').onchange = async event => {
  const file = event.target.files[0];
  if (!file) return;
  try {
    if (file.size > 1000000) throw new Error('Размер файла должен быть не более 1 МБ.');
    $('segment-data').value = await file.text();
    if (mode.value === 'segments') build();
  } catch (error) {
    $('status').classList.add('error');
    $('status').textContent = error.message;
  }
  event.target.value = '';
};
new ResizeObserver(draw).observe(canvas);
build();
