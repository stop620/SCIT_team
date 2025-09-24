document.addEventListener('DOMContentLoaded', async () => {

    const isLogin = await fetch('/mochilearn/api/user/session').then(res => res.json());

    if (!isLogin.loggedIn) {
        if (confirm('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동하시겠습니까?')) {
            window.location.href = '/mochilearn/member/loginForm?redirect=' + encodeURIComponent(window.location.href);
        } else {
            window.location.href = '/mochilearn/';
        }
        return;
    }
});