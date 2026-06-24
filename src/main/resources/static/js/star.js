const c = document.getElementById('stars');
const ctx = c.getContext('2d'); //ctxはCanvasRenderingContext2D型になる
let w,
  h,
  stars = []; // starsだけ[]（配列）

function resize() {
  w = window.innerWidth;
  h = window.innerHeight;
  c.width = w;
  c.height = h;
  stars = Array.from({ length: 500 }, () => ({
    x: Math.random() * w, // Math.random()は 0 以上 1.0 未満の、基本的には小数点以下16桁〜17桁までが選ばれる
    y: Math.random() * h,
    z: Math.random() * w,
  }));
}

window.addEventListener('resize', resize); // ブラウザから'resize'という画面のサイズ変更の情報が来たら（左）、resizeに登録する（右）。
resize();

function drawStars() {
  ctx.fillStyle = '#000';
  ctx.fillRect(0, 0, w, h);

  for (const s of stars) {
    s.z -= 2; // 星が手前に一定のペースで近づく（zが小さくなるほど、画面上では加速して見えるようになる）
    if (s.z <= 0) s.z = w; // 画面を通り過ぎた星は一番奥に戻す
    const k = 128 / s.z; // 手前に来る（zが小さくなる）ほど大きくなる、遠近感の倍率（これが画面上の移動スピードを上げる）
    const x = w / 2 + (s.x - w / 2) * k; // 画面の中心から、遠近感（k）を掛け算した実際の星の横位置（中心より左ならマイナス、右ならプラス方向に広がる）
    const y = h / 2 + (s.y - h / 2) * k; // 画面の中心から、遠近感（k）を掛け算した実際の星の縦位置（中心より上ならマイナス、下ならプラス方向に広がる）

    ctx.fillStyle = '#007fff';
    ctx.fillRect(x, y, 1.5, 1.5);
  }
  requestAnimationFrame(drawStars);
}
drawStars();
