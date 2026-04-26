// ★ 戻る（bfcache 復元）時にボタンを再び押せるようにする
window.addEventListener('pageshow', function (e) {
  if (e.persisted) {
    document.querySelectorAll("button[type='submit']").forEach((btn) => {
      btn.disabled = false;
    });
  }
});

// ★ submit 時の共通処理
document.addEventListener('submit', function (e) {
  const form = e.target;
  const btn = form.querySelector("button[type='submit']");
  if (!btn) return;

  // 全ボタン disable（共通）
  btn.disabled = true;
});
