
document.addEventListener('DOMContentLoaded', () => {

    const loginForm = document.getElementById('loginForm');
    const joinForm = document.getElementById('joinForm');
    const toggleLink = document.getElementById('toggle-link');
    const toggleText = document.getElementById('toggle-text');
    const formTitle = document.getElementById('form-title');

    console.log(toggleLink);
    console.log(toggleLink);

    // 로그인 / 가입 유효성검사 이벤트리스너
    loginForm.addEventListener('submit', validationLoginForm);
    // joinForm의 기본 제출 동작을 막고 AJAX로 처리하기 위해 validationJoinForm만 연결합니다.
    joinForm.addEventListener('submit', validationJoinForm);

    toggleLink.addEventListener('click', (e) => {
        e.preventDefault();
        if (loginForm.classList.contains('active')) {
            // 가입으로 전환
            loginForm.classList.remove('active');
            joinForm.classList.add('active');
            toggleText.textContent = "이미 계정이 있으신가요?";
            toggleLink.textContent = "로그인";
            formTitle.textContent = "MochiLearn 가입하기";
        } else {
            // 로그인으로 전환
            loginForm.classList.add('active');
            joinForm.classList.remove('active');
            toggleText.textContent = "계정이 없으신가요?";
            toggleLink.textContent = "회원가입";
            formTitle.textContent = "로그인";
        }
    });


});

function validationLoginForm(e) {
    e.preventDefault();

    const loginId = document.getElementById('loginId');
    const loginPw = document.getElementById('loginPw');
    const idErr = document.getElementById('login-id-error');
    const pwErr = document.getElementById('login-password-error');
    let isValid = true;

    idErr.textContent = '';
    pwErr.textContent = '';

    if (loginId.value.trim().length == 1) {
        idErr.textContent = '이메일을 입력해주세요.';
        loginId.focus();
        isValid = false;
    } else if(loginId.value.trim().indexOf('@') == -1 || loginId.value.trim().indexOf('.') == -1) {
        idErr.textContent = '이메일 형식으로 입력해주세요.';
        loginId.focus();
        isValid = false;
    } else if (loginPw.value.trim().length < 1) {
        pwErr.textContent = '비밀번호를 입력해주세요.';
        loginPw.focus();
        isValid = false;
    }

    // 유효성 검사 통과 시
    if (isValid) {
        console.log('로그인');
        e.target.submit();

        /*// 리디렉션할 url 파싱
        const urlParams = new URLSearchParams(window.location.search);
        const redirectUrl = urlParams.get('redirect');

        const formData = {
            id: loginId.value.trim(),
            password: loginPw.value.trim()
        };

        // redirect 파라미터가 있다면 param추가
        let fetchUrl = '/mochilearn/member/login';
        if (redirectUrl) {
            fetchUrl += `?redirect=${redirectUrl}`;
        }

        fetch(fetchUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        })
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                return response.json().then(errorData => {
                    throw new Error(errorData.message || '로그인 중 오류가 발생했습니다.');
                });
            })
            .then(data => {
                // 성공 후 페이지로 이동
                if (data.redirectUrl) {
                    window.location.href = data.redirectUrl;
                } else {
                    window.location.href = '/mochilearn/';
                }
            })
            .catch(error => {
                // 서버에서 보낸 에러 메시지 표시
                console.error('로그인 실패:', error);
                if (error.message.includes("아이디")) {
                    idErr.textContent = error.message;
                } else if (error.message.includes("비밀번호")) {
                    pwErr.textContent = error.message;
                } else {
                    idErr.textContent = error.message;
                }
            });*/
    }
}

function validationJoinForm(e) {
    e.preventDefault(); // 기본 폼 제출 동작을 막습니다.

    let isValid = true;

    const joinId = document.getElementById('joinId');
    const joinPw = document.getElementById('joinPw');
    const joinPw2 = document.getElementById('joinPw2');
    const name = document.getElementById('name');
    const nickname = document.getElementById('nickname');

    const idErr = document.getElementById('join-id-error');
    const pwErr = document.getElementById('join-password-error');
    const pw2Err = document.getElementById('join-password2-error');
    const nameErr = document.getElementById('join-name-error');
    const nicknameErr = document.getElementById('join-nickname-error');

    // 기존 에러 메시지 초기화
    idErr.textContent = '';
    pwErr.textContent = '';
    pw2Err.textContent = '';
    nameErr.textContent = '';
    nicknameErr.textContent = '';

    if(joinId.value.trim().length == 0) {
        idErr.textContent = '사용할 이메일을 입력해주세요.';
        joinId.focus();
        isValid = false;

    } else if(joinId.value.trim().indexOf('@') == -1 || joinId.value.trim().indexOf('.') == -1) {
        idErr.textContent = '이메일 형식으로 입력해주세요.';
        joinId.focus();
        isValid = false;

    } else if (joinPw.value.trim().length == 0) {
        pwErr.textContent = '사용할 비밀번호를 입력해주세요.';
        joinPw.focus();
        isValid = false;

    } else if (joinPw2.value.trim().length == 0) {
        pw2Err.textContent = '비밀번호를 한번 더 입력해주세요.';
        joinPw2.focus();
        isValid = false;

    } else if (joinPw.value.trim() != joinPw2.value.trim()) {
        pw2Err.textContent = '동일한 비밀번호를 입력해주세요.'
        joinPw2.focus();
        isValid = false;

    } else if (name.value.trim().length == 0) {
        nameErr.textContent = '이름을 입력해주세요.';
        name.focus();
        isValid = false;

    } else if (nickname.value.trim().length == 0) {
        nicknameErr.textContent = '닉네임을 입력해주세요.';
        nickname.focus();
        isValid = false;

    }

    // 클라이언트 측 유효성 검사 통과 시
    if(isValid) {
        console.log('회원가입 데이터 전송');

        const formData = {
            userId: joinId.value.trim(),
            password: joinPw.value.trim(),
            name: name.value.trim(),
            nickname: nickname.value.trim()
        };
        
        fetch('/mochilearn/member/join', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        })
            .then(response => {
                // 가입성공
                if (response.ok) {
                    return response.json();
                }
                // 실패
                return response.json().then(errorData => {
                    throw new Error(errorData.message || '가입 중 오류가 발생했습니다.');
                });
            })
            .then(data => {
                alert(data.message);

                // TODO: 가입 폼 초기화

                // 성공 후 로그인 폼 보여주기
                document.getElementById('form-title').textContent = "로그인";
                document.getElementById('loginForm').classList.add('active');
                document.getElementById('joinForm').classList.remove('active');
                document.getElementById('toggle-text').textContent = "계정이 없으신가요?";
                document.getElementById('toggle-link').textContent = "회원가입";

            })
            .catch(error => {
                // 서버에서 보낸 에러 메시지 출력
                console.error('회원가입 실패:', error);
                idErr.textContent = error.message;
            });
    }
}

// 구글 로그인 콜백 함수
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