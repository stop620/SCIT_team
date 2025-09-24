/*<![CDATA[*/
//에러 무시해도 됩니다.

window.onload = function() {

    console.log("Navigation script loaded");
    const wordPageBtn = document.getElementById("wordPageBtn");

    if (wordPageBtn) {
        wordPageBtn.onclick = function() {
            fetch('/mochilearn/api/user/session')
                .then(response => response.json())
                .then(data => {
                    if (data.loggedIn) {
                        // 로그인 상태인 경우, 팝업 창을 엽니다.
                        window.open('/mochilearn/page/word', "_blank",
                            "toolbar=yes, scrollbars=yes, resizable=yes, top=100, left=500, width=470, height=700");
                    } else {
                        // 로그인 상태가 아닐 때 컨펌창
                        const userConfirmed = confirm('로그인이 필요한 서비스입니다. 로그인 페이지로 이동하시겠습니까?');
                        // 확인을 누르면 로그인 페이지로 이동
                        if (userConfirmed) {
                            // window.location.href = '/mochilearn/member/loginForm';
                            window.location.href = '/mochilearn/member/loginForm?redirect=' + encodeURIComponent(window.location.href);
                            
                        }
                    }
                })
                .catch(error => {
                    console.error('세션 데이터 로딩 중 오류 발생:', error);
                    alert('로그인 상태를 확인할 수 없습니다. 다시 시도해주세요.');
                });
        };
    }


};


function wordPage() {
    window.open('/mochilearn/page/word', "_blank", "toolbar=yes,scrollbars=yes,resizable=yes,top=100,left=500,width=700,height=400");
}
/*]]>*/