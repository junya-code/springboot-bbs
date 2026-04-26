const likeButton = document.getElementById('like-button');
const likeIcon = document.getElementById('like-icon');
const likeCount = document.getElementById('like-count');

const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute('content');
const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute('content');

if (likeButton) {
  likeButton.addEventListener('click', function () {
    const postId = likeButton.getAttribute('data-post-id');

    fetch(`/posts/${postId}/like`, {
      method: 'POST',
      headers: {
        // オブジェクトリテラルの左側（キー名）は、デフォルトでは“文字列”として扱われる。
        // だから、変数名をキーとして使いたいときは [] で囲んで「これは変数だよ」と教える必要がある。
        [csrfHeader]: csrfToken,
      },
    })
      // --- レスポンス検証ブロック ---
      // fetch は「HTTP 200 でも中身が HTML のエラーページ」という事故が普通に起きる。
      // そのため、ここでは 2 段階でレスポンスを検証している。
      //
      // 1. response.ok（HTTP ステータス）
      //    - 200 以外（404 / 500 / CSRF エラーなど）は即エラー扱い。
      //    - サーバーが返した HTML をそのまま text() で読み、デバッグログに残す。
      //    - ユーザーには共通のエラーメッセージだけを見せる。
      //
      // 2. Content-Type が application/json かどうか
      //    - 200 OK でも HTML が返るケースがあるため、JSON 以外は弾く。
      //    - JSON パースエラーを根本から防ぐための保険。
      //    - 想定外レスポンスは UI を壊すので、ここで必ず止める。
      //
      // この 2 段階チェックのおかげで、
      // 「API が壊れたときに UI が壊れる」という最悪の事故を防げる。
      .then(async (response) => {
        if (!response.ok) {
          const text = await response.text();
          console.error('HTTP error:', response.status, text);
          showFlash('エラーが発生しました');
          throw new Error('HTTP error ' + response.status);
        }

        const contentType = response.headers.get('Content-Type') || '';
        if (!contentType.includes('application/json')) {
          console.error('Unexpected content type:', contentType);
          showFlash('エラーが発生しました');
          throw new Error('Invalid JSON response');
        }

        return response.json();
      })

      .then((data) => {
        if (data.error) {
          showFlash(data.error);
          return;
        }

        // フラッシュメッセージがあれば表示
        if (data.flashMessage) {
          showFlash(data.flashMessage);
        }

        if (data.isLiked) {
          likeIcon.classList.remove('bi-emoji-expressionless');

          likeIcon.classList.add('bi-emoji-heart-eyes');
        } else {
          likeIcon.classList.remove('bi-emoji-heart-eyes');

          likeIcon.classList.add('bi-emoji-expressionless');
        }
        likeCount.textContent = data.likeCount;
      })
      .catch((error) => {
        console.error('Error:', error); // 開発者向け
        showFlash('エラーが発生しました'); // ユーザー向け
      });
  });
}
// トースト風フラッシュメッセージ
let flashTimer = null;

function showFlash(message) {
  const flash = document.getElementById('flash');
  flash.textContent = message;
  flash.classList.add('show');

  // すでにタイマーが動いていたらキャンセル
  if (flashTimer) {
    clearTimeout(flashTimer);
  }

  // 新しいタイマーをセット
  flashTimer = setTimeout(() => {
    flash.classList.remove('show');
    flashTimer = null; // タイマー終了
  }, 2000);
}
