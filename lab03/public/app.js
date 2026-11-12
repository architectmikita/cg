import { stepwise, dda, line, circle } from './algorithms.js';

const $ = id => document.getElementById(id);
const form = $('input'), method = $('method'), canvas = $('grid');
const ctx = canvas.getContext('2d');
const methods = { stepwise, dda, line, circle };
const values = ids => ids.map(id => $(id).valueAsNumber);
let points = [], shown = 0;

function draw() {
  if (!points.length) return;
  const w = canvas.clientWidth, h = canvas.clientHeight;
  const xExtent = $('extent-x').valueAsNumber, yExtent = $('extent-y').valueAsNumber;
  if (!w || !h || !$('extent-x').validity.valid || !$('extent-y').validity.valid) return;
  const dpr = devicePixelRatio;
  canvas.width = Math.round(w * dpr);
  canvas.height = Math.round(h * dpr);
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  const size = Math.min(w / (2 * xExtent + 1), h / (2 * yExtent + 1));
  const left = w / 2, bottom = h / 2;
  const px = x => left + x * size, py = y => bottom - y * size;
  const x0 = -xExtent, x1 = xExtent, y0 = -yExtent, y1 = yExtent;
  $('zoom-value').value = size.toPrecision(3) + ' пикс./клетку';
  ctx.save();
  ctx.beginPath();
  ctx.rect(px(x0 - .5), py(y1 + .5), (2 * xExtent + 1) * size, (2 * yExtent + 1) * size);
  ctx.clip();
  function segment(x, y, ex, ey) {
    ctx.moveTo(x, y);
    ctx.lineTo(ex, ey);
  }
  const gridStep = Math.max(1, Math.ceil(4 / size));
  ctx.strokeStyle = '#dce3ee';
  ctx.beginPath();
  for (let x = Math.floor(x0 / gridStep) * gridStep; x <= x1; x += gridStep)
    segment(px(x - .5), 0, px(x - .5), h);
  for (let y = Math.floor(y0 / gridStep) * gridStep; y <= y1; y += gridStep)
    segment(0, py(y - .5), w, py(y - .5));
  ctx.stroke();

  for (let i = 0; i < shown; i++) {
    const [x, y] = points[i];
    ctx.fillStyle = i === shown - 1 ? '#e97824' : '#346bcc';
    ctx.fillRect(px(x - .5), py(y + .5), size, size);
  }
  ctx.strokeStyle = '#495a72';
  ctx.beginPath();
  segment(px(0), py(y1 + .5), px(0), py(y0 - .5));
  segment(px(x0 - .5), py(0), px(x1 + .5), py(0));
  ctx.stroke();
  ctx.fillStyle = '#182133';
  ctx.font = '12px system-ui';
  const xAxis = Math.min(h - 6, Math.max(14, py(0) + 15));
  const yAxis = Math.min(w - 35, Math.max(4, px(0) + 5));
  const tick = Math.max(1, Math.ceil(38 / size));
  for (let x = Math.ceil(x0 / tick) * tick; x <= x1; x += tick)
    ctx.fillText(String(x), px(x) + 2, xAxis);
  for (let y = Math.ceil(y0 / tick) * tick; y <= y1; y += tick)
    if (y !== 0) ctx.fillText(String(y), yAxis, py(y) - 2);
  ctx.restore();
  ctx.fillText('X', Math.min(w - 14, px(x1 + .5) - 13), Math.max(13, Math.min(h - 5, py(0) - 5)));
  ctx.fillText('Y', yAxis, Math.max(13, py(y1 + .5) + 13));

  const last = shown ? ' · клетка (' + points[shown - 1].join(', ') + ')' : '';
  $('progress').textContent = 'Показано ' + shown + ' из ' + points.length + last;
  $('first').disabled = $('prev').disabled = shown === 0;
  $('next').disabled = $('all').disabled = shown === points.length;
}

function build() {
  if (!form.reportValidity()) return;
  const args = method.value === 'circle' ? values(['x', 'y', 'radius']) : values(['x', 'y', 'x2', 'y2']);
  points = methods[method.value](...args);
  shown = points.length;
  draw();
}

form.addEventListener('submit', event => {
  event.preventDefault();
  build();
});
method.addEventListener('change', () => {
  const round = method.value === 'circle';
  $('end-x').hidden = $('end-y').hidden = $('x2').disabled = $('y2').disabled = round;
  $('radius-field').hidden = $('radius').disabled = !round;
  $('x-label').textContent = round ? 'x центра' : 'x₁';
  $('y-label').textContent = round ? 'y центра' : 'y₁';
  build();
});
for (const id of ['extent-x', 'extent-y']) $(id).addEventListener('input', draw);
$('first').onclick = () => { shown = 0; draw(); };
$('prev').onclick = () => { shown--; draw(); };
$('next').onclick = () => { shown++; draw(); };
$('all').onclick = () => { shown = points.length; draw(); };

$('measure').onclick = () => {
  const fields = ['x', 'y', 'x2', 'y2', 'radius'];
  if (!fields.every(id => {
    const field = $(id), value = field.valueAsNumber;
    return Number.isInteger(value) && value >= Number(field.min) && value <= Number(field.max);
  })) {
    $('times').replaceChildren();
    const row = $('times').insertRow();
    const cell = row.insertCell();
    cell.colSpan = 3;
    cell.textContent = 'Для замера заполните координаты отрезка и радиус. Поля доступны при выборе соответствующего алгоритма.';
    return;
  }
  const lineArgs = values(['x', 'y', 'x2', 'y2']);
  const circleArgs = values(['x', 'y', 'radius']);
  $('times').replaceChildren();
  for (const option of method.options) {
    const run = methods[option.value];
    const args = option.value === 'circle' ? circleArgs : lineArgs;
    const count = run(...args).length;
    const repeats = Math.min(100000, Math.max(1000, Math.ceil(2500000 / count)));
    for (let i = 0; i < 500; i++) run(...args);
    const start = performance.now();
    for (let i = 0; i < repeats; i++) run(...args);
    const mean = (performance.now() - start) * 1000 / repeats;
    const row = $('times').insertRow();
    for (const value of [option.text, count, mean.toFixed(3)]) row.insertCell().textContent = value;
  }
};

new ResizeObserver(draw).observe(canvas);
build();
