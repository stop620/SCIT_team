
function handleCredentialResponse(response) {
    // response.credential은 구글이 암호화해서 보내주는 사용자의 ID 토큰(JWT)
    const idToken = response.credential;

    // 이 ID 토큰을 우리 백엔드 서버로 전송합니다
    fetch('/mochilearn/api/auth/google', { // 백엔드에 새로 만들 API 주소
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ idToken: idToken }), // JSON 형태로 토큰을 전송
    })
        .then(res => {
            if (res.ok) {
                // 로그인 후의 페이지로 이동
                window.location.href = '/mochilearn/';
            } else {
                // 로그인 실패 시 에러 처리
                alert('로그인에 실패했습니다.');
            }
        })
        .catch(error => {
            console.error('로그인 요청 중 에러 발생:', error);
        });
}