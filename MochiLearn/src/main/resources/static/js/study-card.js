
// 학습 카드 페이지의 메인 로직 스크립트

// 전역 변수
let player; // 유튜브 플레이어 인스턴스
let card;   // 현재 카드 데이터
let currentTranscript = []; // 현재 섹션의 자막 배열
let currentTranscriptIndex = 0; // 현재 표시 중인 자막 인덱스
let currentSectionStart = 0; // 현재 재생 중인 섹션의 시작 시간

/**
 * 문서 로드 완료 후 실행되는 메인 함수
 */
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const cardId = urlParams.get('cardId');

    if (cardId) {
        // 1. API를 통해 카드 데이터 가져오기
        $.get(`/mochilearn/api/study/card?cardId=${cardId}`)
            .done(function(cardData) {
                card = cardData; // 전역 변수에 카드 데이터 저장
                card.videoId = extractVideoId(card.url);

                renderCardUI(card);
                renderSectionButtons(card.sections);

                // 2. 다른 기능 모듈들 초기화
                if (typeof YT !== 'undefined' && YT && YT.Player) {
                    createPlayer(card.videoId);
                }
                initializeTranscriptControls();
                initializeWordSelectionEventListeners();
                initializeSpeechPracticeListeners();
            })
            .fail(function() {
                console.error("❌ 카드 데이터 API 호출 실패");
                alert("학습 카드 정보를 불러오는 데 실패했습니다.");
            });

        initializeCardActionListeners(cardId);

    } else {
        console.warn("⚠️ URL에 cardId 파라미터가 존재하지 않음");
    }
});

/**
 * 카드 정보(제목, 태그, 레벨)를 UI에 렌더링하는 함수
 */
function renderCardUI(cardData) {
    $('#cardTitle').text(cardData.title || "");
    const tagsContainer = $('.tags.card');
    tagsContainer.empty();

    const levelMap = { 1: "초급", 2: "중급", 3: "고급" };
    if (cardData.level) {
        tagsContainer.append(`<div class="tag-item">${levelMap[cardData.level] || "기본"}</div>`);
    }
    if (cardData.tag) {
        cardData.tag.split(',').map(tag => tag.trim()).forEach(tag => {
            if (tag) tagsContainer.append(`<div class="tag-item">${tag}</div>`);
        });
    }
    updateLikeButton(cardData.liked, cardData.like);
}

/**
 * 좋아요, 삭제 등 카드 자체에 대한 액션 이벤트 리스너를 초기화하는 함수
 */
function initializeCardActionListeners(cardId) {
    let isLoggedIn = false;
    $.get('/mochilearn/api/user/session').done(userData => {
        isLoggedIn = !!(userData && userData.loggedIn);
    });

    $('#like-button').click(() => {
        if (!isLoggedIn) {
            alert('좋아요를 누르려면 로그인해야 합니다.');
            return;
        }
        toggleLike(cardId);
    });

    $('#delete-button').click(() => deleteCard(cardId));
}

/**
 * 좋아요 버튼 UI 업데이트
 */
function updateLikeButton(isLiked, likeCount) {
    card.liked = isLiked;
    card.like = likeCount;
    $('#cardLike').text(likeCount);
    $('#like-button').css('color', isLiked ? 'red' : 'black');
}

/**
 * 좋아요 토글 API 호출
 */
function toggleLike(cardId) {
    const newLiked = !card.liked;
    $.post('/mochilearn/api/study/likes/toggle', { cardId, liked: newLiked })
        .done(function(response) {
            if ((response.status === 'liked' && newLiked) || (response.status === 'unliked' && !newLiked)) {
                const newLikeCount = card.like + (newLiked ? 1 : -1);
                updateLikeButton(newLiked, newLikeCount);
            } else {
                alert('좋아요 상태 갱신에 실패했습니다.');
            }
        })
        .fail(() => alert('좋아요 처리 중 오류가 발생했습니다.'));
}

/**
 * 카드 삭제 API 호출
 */
function deleteCard(cardId) {
    if (!confirm("카드를 삭제하시겠습니까?")) return;
    $.ajax({
        url: `/mochilearn/api/study/card/${cardId}`,
        type: 'DELETE',
        success: () => {
            alert("카드가 삭제되었습니다.");
            window.location.href = "/mochilearn/page/study";
        },
        error: () => alert("카드 삭제에 실패했습니다.")
    });
}
